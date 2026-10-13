package com.fs.aicommit

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe

internal interface ApiCredentialStore {
    fun read(id: String): String?
    fun write(id: String, key: String?)
}

internal object ApiCredentials : ApiCredentialStore {
    private fun attributes(id: String) = CredentialAttributes("GenerateCommitMessage.API.$id")
    override fun read(id: String): String? = PasswordSafe.instance.getPassword(attributes(id))
    override fun write(id: String, key: String?) = PasswordSafe.instance.setPassword(attributes(id), key)

    fun resolve(config: ApiProviderConfig): ApiProviderConfig =
        if (config.apiKey.isNotBlank()) config.copy() else config.copy(apiKey = read(config.id).orEmpty())
}
