package com.fs.aicommit

internal class ProviderModelSelection {
    private val models = mutableMapOf<String, String>()
    private val thinkingLevels = mutableMapOf<String, String>()
    var provider: String = AiCommitSettings.PROVIDER_CLAUDE
        private set
    val model: String get() = models[provider].orEmpty()
    val thinking: String get() = thinkingLevels[provider].orEmpty()

    fun reset(provider: String, rawModel: String) {
        models.clear()
        thinkingLevels.clear()
        select(provider)
        val (model, thinking) = SdkDefinition.splitModelThinking(SdkDefinition.fromProvider(provider), rawModel)
        remember(model, thinking)
    }

    fun select(provider: String) { this.provider = provider }

    fun remember(model: String, thinking: String) {
        models[provider] = model.trim()
        thinkingLevels[provider] = thinking.takeIf { it in SdkDefinition.fromProvider(provider).thinkingLevels }.orEmpty()
    }

    fun composedModel(): String = if (model.isNotBlank() && thinking.isNotBlank()) "$model $thinking" else model
}
