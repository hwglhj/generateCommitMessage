package com.fs.aicommit

import org.junit.Assert.*
import org.junit.Test

class ApiConfigurationModelTest {
    private class MemoryCredentials : ApiCredentialStore {
        val keys = mutableMapOf<String, String>()
        override fun read(id: String) = keys[id]
        override fun write(id: String, key: String?) { if (key == null) keys.remove(id) else keys[id] = key }
    }
    @Test
    fun `editing working configs does not change saved settings until apply`() {
        val settings = AiCommitSettings().apply {
            apiConfigs = mutableListOf(ApiProviderConfig(id = "one", name = "Original"))
            activeApiConfigId = "one"
        }
        val model = ApiConfigurationModel(MemoryCredentials())
        model.reset(settings)
        model.put(model.configs.first().copy(name = "Edited"))
        assertEquals("Original", settings.apiConfigs.first().name)
        assertTrue(model.isModified(settings))
        model.apply(settings)
        assertEquals("Edited", settings.apiConfigs.first().name)
        assertFalse(model.isModified(settings))
    }

    @Test
    fun `removing active configuration selects next available configuration`() {
        val model = ApiConfigurationModel(MemoryCredentials())
        model.put(ApiProviderConfig(id = "one", name = "One"))
        model.put(ApiProviderConfig(id = "two", name = "Two"))
        assertEquals("one", model.activeId)
        model.remove("one")
        assertEquals("two", model.activeId)
        model.remove("two")
        assertEquals("", model.activeId)
    }

    @Test
    fun `keys are loaded saved and removed only on apply`() {
        val credentials = MemoryCredentials().apply { keys["one"] = "saved-key" }
        val settings = AiCommitSettings().apply {
            apiConfigs.add(ApiProviderConfig(id = "one", name = "One"))
            activeApiConfigId = "one"
        }
        val model = ApiConfigurationModel(credentials)
        model.reset(settings)
        assertEquals("saved-key", model.configs.single().apiKey)
        assertFalse(model.isModified(settings))
        model.put(model.configs.single().copy(apiKey = "edited-key"))
        assertEquals("saved-key", credentials.read("one"))
        model.reset(settings)
        assertEquals("saved-key", model.configs.single().apiKey)
        model.remove("one")
        assertEquals("saved-key", credentials.read("one"))
        model.apply(settings)
        assertNull(credentials.read("one"))
        assertFalse(model.isModified(settings))
    }

    @Test
    fun `import changes working copy but does not write credentials before apply`() {
        val credentials = MemoryCredentials().apply { keys["one"] = "local" }
        val settings = AiCommitSettings().apply {
            apiConfigs.add(ApiProviderConfig(id = "one", name = "Old", model = "old"))
            activeApiConfigId = "one"
        }
        val model = ApiConfigurationModel(credentials)
        model.reset(settings)
        val document = ApiSiteTransfer.decode(importJson(name = "New", apiKey = "imported"))

        model.importSites(document)

        assertEquals("New", model.configs.single().name)
        assertEquals("local", credentials.read("one"))
        model.apply(settings)
        assertEquals("imported", credentials.read("one"))
    }

    @Test
    fun `import without key preserves local credential after apply`() {
        val credentials = MemoryCredentials().apply { keys["one"] = "local" }
        val settings = AiCommitSettings().apply {
            apiConfigs.add(ApiProviderConfig(id = "one", name = "Old", model = "old"))
            activeApiConfigId = "one"
        }
        val model = ApiConfigurationModel(credentials)
        model.reset(settings)

        model.importSites(ApiSiteTransfer.decode(importJson(name = "New", apiKey = null)))
        model.apply(settings)

        assertEquals("local", credentials.read("one"))
    }

    @Test
    fun `working copies isolate mutable model lists`() {
        val settings = AiCommitSettings().apply {
            apiConfigs.add(ApiProviderConfig(id = "one", name = "One", model = "one", models = mutableListOf("one")))
        }
        val model = ApiConfigurationModel(MemoryCredentials())
        model.reset(settings)

        model.configs.single().models.add("two")

        assertEquals(listOf("one"), settings.apiConfigs.single().models)
    }

    private fun importJson(name: String, apiKey: String?): String {
        val key = apiKey?.let { ",\"apiKey\":\"$it\"" }.orEmpty()
        return """
            {
              "format":"generate-commit-message.api-sites",
              "version":1,
              "activeConfigId":"one",
              "configs":[
                {"id":"one","name":"$name","provider":"openai","baseUrl":"https://one.example/v1","model":"new","models":["new","other"],"reasoningCompatible":false$key}
              ]
            }
        """.trimIndent()
    }
}
