package com.fs.aicommit

class ApiConfigurationModel internal constructor(private val credentials: ApiCredentialStore = ApiCredentials) {
    val configs: MutableList<ApiProviderConfig> = mutableListOf()
    var activeId: String = ""

    fun put(config: ApiProviderConfig) {
        val index = configs.indexOfFirst { it.id == config.id }
        if (index < 0) configs.add(config.deepCopy()) else configs[index] = config.deepCopy()
        if (activeId.isBlank()) activeId = config.id
    }

    fun remove(id: String) {
        configs.removeAll { it.id == id }
        if (activeId == id) activeId = configs.firstOrNull()?.id.orEmpty()
    }

    fun reset(settings: AiCommitSettings) {
        configs.clear()
        configs.addAll(settings.apiConfigs.map { config ->
            config.deepCopy().copy(apiKey = config.apiKey.ifBlank { credentials.read(config.id).orEmpty() })
        })
        activeId = settings.activeApiConfigId
        baseline = configs.map { it.deepCopy() }
    }

    fun apply(settings: AiCommitSettings) {
        configs.forEach { credentials.write(it.id, it.apiKey.takeIf(String::isNotBlank)) }
        settings.apiConfigs.filter { old -> configs.none { it.id == old.id } }.forEach { credentials.write(it.id, null) }
        settings.apiConfigs = configs.map { it.deepCopy() }.toMutableList()
        settings.activeApiConfigId = activeId
        baseline = configs.map { it.deepCopy() }
    }

    internal fun importSites(document: ApiSiteImportDocument): ApiSiteMergeResult {
        val result = ApiSiteTransfer.merge(configs, activeId, document)
        configs.clear()
        configs.addAll(result.configs.map { it.deepCopy() })
        activeId = result.activeConfigId
        return result
    }

    internal fun exportJson(includeApiKeys: Boolean): String =
        ApiSiteTransfer.encode(configs, activeId, includeApiKeys)

    private var baseline: List<ApiProviderConfig> = emptyList()

    fun isModified(settings: AiCommitSettings): Boolean =
        configs != baseline || activeId != settings.activeApiConfigId
}
