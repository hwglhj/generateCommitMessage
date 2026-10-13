package com.fs.aicommit

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiSiteTransferTest {
    @Test
    fun `export omits apiKey when secrets are disabled`() {
        val json = ApiSiteTransfer.encode(
            listOf(ApiProviderConfig(id = "one", name = "One", apiKey = "secret", model = "m", models = mutableListOf("m"))),
            activeId = "one",
            includeApiKeys = false
        )

        val site = JsonParser.parseString(json).asJsonObject.getAsJsonArray("configs")[0].asJsonObject
        assertFalse(site.has("apiKey"))
    }

    @Test
    fun `export includes apiKey only when requested`() {
        val json = ApiSiteTransfer.encode(
            listOf(ApiProviderConfig(id = "one", name = "One", apiKey = "secret", model = "m")),
            activeId = "one",
            includeApiKeys = true
        )

        val site = JsonParser.parseString(json).asJsonObject.getAsJsonArray("configs")[0].asJsonObject
        assertEquals("secret", site.get("apiKey").asString)
    }

    @Test
    fun `decode rejects wrong format version duplicate ids and invalid URL`() {
        assertDecodeFails(validJson().replace("generate-commit-message.api-sites", "other"))
        assertDecodeFails(validJson().replace("\"version\":1", "\"version\":2"))
        assertDecodeFails(validJson(includeSecond = true).replace("\"id\":\"two\"", "\"id\":\"one\""))
        assertDecodeFails(validJson().replace("https://one.example/v1", "file:///bad"))
    }

    @Test
    fun `merge overwrites site fields and preserves missing local key`() {
        val existing = listOf(ApiProviderConfig(id = "one", name = "Old", apiKey = "local", model = "old"))
        val document = ApiSiteTransfer.decode(validJson(name = "New", apiKey = null))

        val result = ApiSiteTransfer.merge(existing, "one", document)

        assertEquals(1, result.overwritten)
        assertEquals("New", result.configs.single().name)
        assertEquals("local", result.configs.single().apiKey)
        assertEquals(1, result.preservedKeys)
    }

    @Test
    fun `merge imports nonblank key and chooses imported active site`() {
        val existing = listOf(
            ApiProviderConfig(id = "one", name = "One", apiKey = "local", model = "old"),
            ApiProviderConfig(id = "two", name = "Two", apiKey = "two-key", model = "two")
        )
        val document = ApiSiteTransfer.decode(validJson(id = "one", name = "Imported", apiKey = "imported", activeId = "one"))

        val result = ApiSiteTransfer.merge(existing, "two", document)

        assertEquals("one", result.activeConfigId)
        assertEquals("imported", result.configs.first().apiKey)
        assertEquals(1, result.importedKeys)
    }

    @Test
    fun `merge chooses existing active then first site when imported active id is invalid`() {
        val existing = listOf(ApiProviderConfig(id = "one", name = "One", model = "one"))
        val document = ApiSiteTransfer.decode(validJson(id = "new", name = "New", apiKey = null, activeId = "missing"))

        val result = ApiSiteTransfer.merge(existing, "one", document)

        assertEquals("one", result.activeConfigId)
        assertTrue(result.configs.any { it.id == "new" })
    }

    @Test
    fun `decode normalizes model list and preserves current model`() {
        val json = validJson().replace("\"models\": [\"m\", \"n\"]", "\"models\": [\"n\", \"\", \"m\"]")

        val document = ApiSiteTransfer.decode(json)

        assertEquals(listOf("m", "n"), document.configs.first().models)
    }

    private fun assertDecodeFails(json: String) {
        try {
            ApiSiteTransfer.decode(json)
            throw AssertionError("Expected decode to fail")
        } catch (_: ApiSiteTransferException) {
            // expected
        }
    }

    private fun validJson(
        id: String = "one",
        name: String = "One",
        apiKey: String? = "imported",
        activeId: String = "",
        includeSecond: Boolean = false
    ): String {
        val key = apiKey?.let { ", \"apiKey\": \"$it\"" }.orEmpty()
        val effectiveActiveId = activeId.ifBlank { id }
        val second = if (includeSecond) """ , {"id":"two","name":"Two","provider":"openai","baseUrl":"https://two.example/v1","model":"two","models":["two"],"reasoningCompatible":false} """ else ""
        return """
            {
              "format":"generate-commit-message.api-sites",
              "version":1,
              "activeConfigId":"$effectiveActiveId",
              "includeApiKeys":${apiKey != null},
              "configs":[
                {"id":"$id","name":"$name","provider":"openai","baseUrl":"https://one.example/v1","model":"m","models":["m","n"],"reasoningCompatible":false$key}$second
              ]
            }
        """.trimIndent()
    }
}
