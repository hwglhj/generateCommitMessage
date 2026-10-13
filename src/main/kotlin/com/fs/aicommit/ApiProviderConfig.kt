package com.fs.aicommit

import com.fs.i18n.SubMsgBundle
import java.net.URI
import java.util.UUID
import com.intellij.util.xmlb.annotations.Transient

data class ApiProviderConfig(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var provider: String = OPENAI_COMPATIBLE,
    var baseUrl: String = "https://api.openai.com/v1",
    @get:Transient var apiKey: String = "",
    var model: String = "",
    var models: MutableList<String> = mutableListOf(),
    var reasoningCompatible: Boolean = false
) {
    override fun toString(): String = name

    fun validationError(requireModel: Boolean = true): String? {
        if (name.isBlank()) return SubMsgBundle.message("api.validation.name")
        val uri = try { URI(baseUrl.trim()) } catch (_: Exception) { null }
        if (uri == null || uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank() || uri.userInfo != null) {
            return SubMsgBundle.message("api.validation.url")
        }
        if (requireModel && model.isBlank()) return SubMsgBundle.message("api.validation.model")
        if (apiKey.isBlank() && provider != CUSTOM && uri.host !in listOf("localhost", "127.0.0.1", "[::1]")) {
            return SubMsgBundle.message("error.api.key.missing")
        }
        return null
    }

    fun endpoint(path: String): String {
        val uri = URI(baseUrl.trim())
        var root = uri.rawPath.orEmpty().trimEnd('/')
        for (suffix in listOf("/chat/completions", "/messages", "/models")) root = root.removeSuffix(suffix)
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        return URI("${uri.scheme}://${uri.rawAuthority}$root/$path$query").toASCIIString()
    }

    fun normalizeModels() {
        val current = model.trim()
        model = current
        models = sequenceOf(current)
            .plus(models.asSequence().map(String::trim))
            .filter(String::isNotBlank)
            .distinct()
            .toMutableList()
    }

    fun availableModels(): List<String> = deepCopy().apply { normalizeModels() }.models.toList()

    fun deepCopy(): ApiProviderConfig = copy(models = models.toMutableList())

    companion object {
        const val OPENAI_COMPATIBLE = "openai"
        const val ANTHROPIC = "anthropic"
        const val CUSTOM = "custom"
    }
}
