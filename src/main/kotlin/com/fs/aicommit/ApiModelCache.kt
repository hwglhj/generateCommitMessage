package com.fs.aicommit

internal data class ApiModelCacheIdentity(
    val id: String,
    val provider: String,
    val baseUrl: String,
)

internal data class ApiModelCacheUpdate(
    val identity: ApiModelCacheIdentity,
    val remoteModels: List<String>,
    val fetchedAt: Long,
)

/** Pure model-cache rules shared by every API configuration UI. */
internal object ApiModelCache {
    fun createUpdate(
        config: ApiProviderConfig,
        fetched: List<String>,
        fetchedAt: Long,
    ): ApiModelCacheUpdate? {
        val remote = fetched.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .toList()
        if (remote.isEmpty()) return null

        return ApiModelCacheUpdate(identityOf(config), remote, fetchedAt)
    }

    fun apply(configs: List<ApiProviderConfig>, update: ApiModelCacheUpdate): Boolean {
        val target = configs.firstOrNull { identityOf(it) == update.identity } ?: return false
        target.models = sequenceOf(target.model.trim())
            .plus(update.remoteModels.asSequence())
            .filter(String::isNotBlank)
            .distinct()
            .toMutableList()
        target.modelsFetchedAt = update.fetchedAt
        return true
    }

    private fun identityOf(config: ApiProviderConfig): ApiModelCacheIdentity = ApiModelCacheIdentity(
        id = config.id,
        provider = config.provider.trim(),
        baseUrl = config.baseUrl.trim(),
    )
}
