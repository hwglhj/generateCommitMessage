package com.fs.aicommit

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI

internal data class ApiSiteImportDocument(
    val activeConfigId: String,
    val configs: List<ApiProviderConfig>,
    val importedKeys: Map<String, String>
)

internal data class ApiSiteMergeResult(
    val configs: List<ApiProviderConfig>,
    val activeConfigId: String,
    val added: Int,
    val overwritten: Int,
    val importedKeys: Int,
    val preservedKeys: Int
)

internal class ApiSiteTransferException(message: String) : IllegalArgumentException(message)

internal object ApiSiteTransfer {
    private const val FORMAT = "generate-commit-message.api-sites"
    private const val VERSION = 1

    fun encode(configs: List<ApiProviderConfig>, activeId: String, includeApiKeys: Boolean): String {
        val root = JsonObject().apply {
            addProperty("format", FORMAT)
            addProperty("version", VERSION)
            addProperty("activeConfigId", activeId)
            addProperty("includeApiKeys", includeApiKeys)
            add("configs", JsonArray().apply {
                configs.forEach { source ->
                    val config = source.deepCopy().apply { normalizeModels() }
                    add(JsonObject().apply {
                        addProperty("id", config.id)
                        addProperty("name", config.name)
                        addProperty("provider", config.provider)
                        addProperty("baseUrl", config.baseUrl)
                        addProperty("model", config.model)
                        add("models", JsonArray().apply { config.models.forEach { add(it) } })
                        addProperty("reasoningCompatible", config.reasoningCompatible)
                        if (includeApiKeys && config.apiKey.isNotBlank()) addProperty("apiKey", config.apiKey)
                    })
                }
            })
        }
        return GsonBuilder().setPrettyPrinting().create().toJson(root)
    }

    fun decode(json: String): ApiSiteImportDocument {
        val root = try {
            JsonParser.parseString(json).takeIf { it.isJsonObject }?.asJsonObject
        } catch (error: Exception) {
            throw ApiSiteTransferException("Invalid JSON: ${error.message.orEmpty()}")
        } ?: throw ApiSiteTransferException("The root value must be a JSON object")

        requireString(root, "format").takeIf { it == FORMAT }
            ?: throw ApiSiteTransferException("Unsupported export format")
        requireInt(root, "version").takeIf { it == VERSION }
            ?: throw ApiSiteTransferException("Unsupported export version")
        val activeId = optionalString(root, "activeConfigId").orEmpty()
        root.get("includeApiKeys")?.let { value ->
            if (!value.isJsonPrimitive || !value.asJsonPrimitive.isBoolean) {
                throw ApiSiteTransferException("includeApiKeys must be a boolean")
            }
        }
        val configsElement = root.get("configs")
        if (configsElement == null || !configsElement.isJsonArray) {
            throw ApiSiteTransferException("configs must be an array")
        }

        val configs = mutableListOf<ApiProviderConfig>()
        val importedKeys = mutableMapOf<String, String>()
        val ids = mutableSetOf<String>()
        configsElement.asJsonArray.forEachIndexed { index, element ->
            val site = element.takeIf { it.isJsonObject }?.asJsonObject
                ?: throw ApiSiteTransferException("configs[$index] must be an object")
            val id = requireString(site, "id").trim()
            if (id.isBlank() || !ids.add(id)) throw ApiSiteTransferException("Duplicate or blank site id: $id")
            val name = requireString(site, "name").trim()
            val provider = requireString(site, "provider").trim()
            val baseUrl = requireString(site, "baseUrl").trim()
            val model = requireString(site, "model").trim()
            val models = readModels(site, index)
            val reasoningCompatible = requireBoolean(site, "reasoningCompatible")
            val apiKey = optionalString(site, "apiKey").orEmpty().trim()
            val config = ApiProviderConfig(
                id = id,
                name = name,
                provider = provider,
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                models = models.toMutableList(),
                reasoningCompatible = reasoningCompatible
            ).apply { normalizeModels() }
            validateImportedConfig(config, index)
            configs += config
            if (apiKey.isNotBlank()) importedKeys[id] = apiKey
        }
        return ApiSiteImportDocument(activeId, configs.map(ApiProviderConfig::deepCopy), importedKeys.toMap())
    }

    fun merge(
        existing: List<ApiProviderConfig>,
        existingActiveId: String,
        document: ApiSiteImportDocument
    ): ApiSiteMergeResult {
        val incomingById = document.configs.associateBy { it.id }
        val result = existing.map { current ->
            incomingById[current.id]?.let { imported ->
                imported.deepCopy().copy(apiKey = imported.apiKey.ifBlank { current.apiKey })
            } ?: current.deepCopy()
        }.toMutableList()
        val existingIds = existing.map { it.id }.toSet()
        document.configs.filter { it.id !in existingIds }.forEach { result += it.deepCopy() }
        val active = when {
            document.activeConfigId in result.map { it.id } -> document.activeConfigId
            existingActiveId in result.map { it.id } -> existingActiveId
            else -> result.firstOrNull()?.id.orEmpty()
        }
        val overwritten = document.configs.count { it.id in existingIds }
        val added = document.configs.count { it.id !in existingIds }
        val importedKeys = document.importedKeys.count { (id, _) -> document.configs.any { it.id == id } }
        val preservedKeys = document.configs.count { imported ->
            imported.id in existingIds && imported.apiKey.isBlank() && existing.first { it.id == imported.id }.apiKey.isNotBlank()
        }
        return ApiSiteMergeResult(result, active, added, overwritten, importedKeys, preservedKeys)
    }

    private fun validateImportedConfig(config: ApiProviderConfig, index: Int) {
        if (config.name.isBlank()) throw ApiSiteTransferException("configs[$index].name is required")
        if (config.provider !in setOf(ApiProviderConfig.OPENAI_COMPATIBLE, ApiProviderConfig.ANTHROPIC, ApiProviderConfig.CUSTOM)) {
            throw ApiSiteTransferException("configs[$index].provider is unsupported")
        }
        val uri = try { URI(config.baseUrl) } catch (_: Exception) { null }
        if (uri == null || uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank() || uri.userInfo != null) {
            throw ApiSiteTransferException("configs[$index].baseUrl is invalid")
        }
        if (config.model.isBlank()) throw ApiSiteTransferException("configs[$index].model is required")
    }

    private fun readModels(site: JsonObject, index: Int): List<String> {
        val element = site.get("models") ?: throw ApiSiteTransferException("configs[$index].models is required")
        if (!element.isJsonArray) throw ApiSiteTransferException("configs[$index].models must be an array")
        return element.asJsonArray.mapIndexed { modelIndex, value ->
            if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
                throw ApiSiteTransferException("configs[$index].models[$modelIndex] must be a string")
            }
            value.asString
        }
    }

    private fun requireString(obj: JsonObject, name: String): String {
        val value = obj.get(name)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
            throw ApiSiteTransferException("$name must be a string")
        }
        return value.asString
    }

    private fun optionalString(obj: JsonObject, name: String): String? {
        val value = obj.get(name) ?: return null
        if (value.isJsonNull) return null
        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
            throw ApiSiteTransferException("$name must be a string")
        }
        return value.asString
    }

    private fun requireInt(obj: JsonObject, name: String): Int {
        val value = obj.get(name)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) {
            throw ApiSiteTransferException("$name must be a number")
        }
        return try { value.asInt } catch (_: Exception) { throw ApiSiteTransferException("$name must be an integer") }
    }

    private fun requireBoolean(obj: JsonObject, name: String): Boolean {
        val value = obj.get(name)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isBoolean) {
            throw ApiSiteTransferException("$name must be a boolean")
        }
        return value.asBoolean
    }
}
