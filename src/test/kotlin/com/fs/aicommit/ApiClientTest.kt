package com.fs.aicommit

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class ApiClientTest {
    private fun config() = ApiProviderConfig(
        id = "one", name = "Example", baseUrl = "https://example.test/v1/", apiKey = "secret", model = "test-model"
    )

    @Test
    fun `openai request uses configured endpoint and parses content`() {
        var sent: ApiHttpRequest? = null
        val client = ApiClient(ApiTransport { request ->
            sent = request
            ApiHttpResponse(200, """{"choices":[{"message":{"content":"feat: add API mode"}}]}""")
        })

        assertEquals("feat: add API mode", client.complete(config(), "changes"))
        assertEquals("https://example.test/v1/chat/completions", sent!!.url)
        assertEquals("Bearer secret", sent!!.headers["Authorization"])
        val body = JsonParser.parseString(sent!!.body).asJsonObject
        assertEquals("test-model", body.get("model").asString)
        assertEquals("changes", body.getAsJsonArray("messages")[0].asJsonObject.get("content").asString)
    }

    @Test
    fun `anthropic request uses messages protocol`() {
        var sent: ApiHttpRequest? = null
        val client = ApiClient(ApiTransport { request ->
            sent = request
            ApiHttpResponse(200, """{"content":[{"type":"text","text":"fix: windows paths"}]}""")
        })
        val config = config().apply { provider = ApiProviderConfig.ANTHROPIC }

        assertEquals("fix: windows paths", client.complete(config, "changes"))
        assertEquals("https://example.test/v1/messages", sent!!.url)
        assertEquals("secret", sent!!.headers["x-api-key"])
        assertEquals("2023-06-01", sent!!.headers["anthropic-version"])
    }

    @Test
    fun `reasoning compatible request omits temperature and uses completion token limit`() {
        var sent: ApiHttpRequest? = null
        val client = ApiClient(ApiTransport { request ->
            sent = request
            ApiHttpResponse(200, """{"choices":[{"message":{"content":"ok"}}]}""")
        })
        client.complete(config().apply { reasoningCompatible = true }, "changes")
        val body = JsonParser.parseString(sent!!.body).asJsonObject
        assertFalse(body.has("temperature"))
        assertTrue(body.has("max_completion_tokens"))
    }

    @Test
    fun `models response is unique and endpoint suffix is not duplicated`() {
        var sent: ApiHttpRequest? = null
        val client = ApiClient(ApiTransport { request ->
            sent = request
            ApiHttpResponse(200, """{"data":[{"id":"a"},{"id":"b"},{"id":"a"}]}""")
        })
        val config = config().apply { baseUrl = "https://example.test/v1/chat/completions" }
        assertEquals(listOf("a", "b"), client.fetchModels(config))
        assertEquals("https://example.test/v1/models", sent!!.url)
    }

    @Test
    fun `gateway path and query escapes survive endpoint normalization`() {
        val config = config().apply {
            baseUrl = "https://example.test/proxy%2Ftenant/v1/chat/completions?token=a%2Bb%26c%3Dd"
        }
        assertEquals("https://example.test/proxy%2Ftenant/v1/models?token=a%2Bb%26c%3Dd", config.endpoint("models"))
    }

    @Test
    fun `api errors include status but redact the configured key`() {
        val client = ApiClient(ApiTransport { ApiHttpResponse(401, """{"error":{"message":"invalid secret"}}""") })
        val error = try {
            client.complete(config(), "changes")
            fail("Expected an API error")
            throw AssertionError()
        } catch (error: ApiException) { error }
        assertTrue(error.message!!.contains("401"))
        assertFalse(error.message!!.contains("secret"))
    }

    @Test
    fun `configuration rejects unsupported URLs and empty model`() {
        assertNotNull(config().apply { baseUrl = "file:///tmp/config" }.validationError())
        assertNotNull(config().apply { model = " " }.validationError())
        assertNull(config().apply { apiKey = ""; baseUrl = "http://localhost:11434/v1" }.validationError())
    }

    @Test
    fun `transport failures are readable and do not expose secrets`() {
        val client = ApiClient(ApiTransport { throw java.io.IOException("connection failed for secret") })
        try {
            client.complete(config(), "changes")
            fail("Expected failure")
        } catch (error: ApiException) {
            assertFalse(error.message!!.contains("secret"))
        }
    }

    @Test
    fun `malformed completion structure is reported as an API error`() {
        val client = ApiClient(ApiTransport { ApiHttpResponse(200, """{"choices":"invalid"}""") })
        try {
            client.complete(config(), "changes")
            fail("Expected failure")
        } catch (error: ApiException) {
            assertFalse(error.message.isNullOrBlank())
        }
    }
}
