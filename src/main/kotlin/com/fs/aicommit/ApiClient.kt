package com.fs.aicommit

import com.fs.i18n.SubMsgBundle
import com.intellij.openapi.progress.ProcessCanceledException
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

data class ApiHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String = "",
    val checkCanceled: () -> Unit = {}
)
data class ApiHttpResponse(val status: Int, val body: String)
fun interface ApiTransport { fun send(request: ApiHttpRequest): ApiHttpResponse }
class ApiException(message: String) : RuntimeException(message)

class ApiClient(private val transport: ApiTransport = JdkApiTransport()) {
    fun complete(config: ApiProviderConfig, prompt: String, checkCanceled: () -> Unit = {}): String {
        config.validationError()?.let { throw ApiException(it) }
        val body = JsonObject().apply {
            addProperty("model", config.model.trim())
            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                })
            })
            if (config.provider == ApiProviderConfig.ANTHROPIC) {
                addProperty("max_tokens", 2048)
            } else if (config.reasoningCompatible) {
                addProperty("max_completion_tokens", 4096)
            } else {
                addProperty("max_tokens", 2048)
                addProperty("temperature", 0.2)
            }
            addProperty("stream", false)
        }
        val endpoint = if (config.provider == ApiProviderConfig.ANTHROPIC) "messages" else "chat/completions"
        val json = request(config, "POST", endpoint, body.toString(), checkCanceled)
        val content = if (config.provider == ApiProviderConfig.ANTHROPIC) {
            textContent(json.get("content"))
        } else {
            val choices = json.get("choices")?.takeIf { it.isJsonArray }?.asJsonArray
                ?: throw ApiException(SubMsgBundle.message("api.invalid.response"))
            val choice = choices.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
            textContent(choice?.get("message")?.takeIf { it.isJsonObject }?.asJsonObject?.get("content"))
        }
        return content.trim().ifBlank { throw ApiException(SubMsgBundle.message("api.empty.response")) }
    }

    fun fetchModels(config: ApiProviderConfig, checkCanceled: () -> Unit = {}): List<String> {
        config.validationError(requireModel = false)?.let { throw ApiException(it) }
        val json = request(config, "GET", "models", "", checkCanceled)
        val models = json.get("data")?.takeIf { it.isJsonArray }?.asJsonArray
            ?: throw ApiException(SubMsgBundle.message("api.invalid.response"))
        return models.mapNotNull { it.takeIf { it.isJsonObject }?.asJsonObject?.get("id")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString }
            .filter { it.isNotBlank() }.distinct().sorted()
    }

    private fun request(config: ApiProviderConfig, method: String, endpoint: String, body: String, checkCanceled: () -> Unit): JsonObject {
        val headers = linkedMapOf("Content-Type" to "application/json", "Accept" to "application/json")
        if (config.provider == ApiProviderConfig.ANTHROPIC) {
            headers["x-api-key"] = config.apiKey.trim()
            headers["anthropic-version"] = "2023-06-01"
        } else if (config.apiKey.isNotBlank()) {
            headers["Authorization"] = "Bearer ${config.apiKey.trim()}"
        }
        checkCanceled()
        val response = try {
            transport.send(ApiHttpRequest(method, config.endpoint(endpoint), headers, body, checkCanceled))
        } catch (error: ProcessCanceledException) {
            throw error
        } catch (error: java.util.concurrent.CancellationException) {
            throw ProcessCanceledException(error)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ProcessCanceledException(error)
        } catch (_: Exception) {
            throw ApiException(SubMsgBundle.message("error.api.connection"))
        }
        checkCanceled()
        if (response.status !in 200..299) {
            val detail = try {
                val error = JsonParser.parseString(response.body).asJsonObject.get("error")
                if (error?.isJsonObject == true) error.asJsonObject.get("message")?.asString else error?.asString
            } catch (_: Exception) { null }
            var message = detail ?: response.body
            if (config.apiKey.isNotBlank()) message = message.replace(config.apiKey.trim(), "***")
            message = message.take(500)
            throw ApiException(SubMsgBundle.message("api.http.error", response.status, message))
        }
        return try {
            JsonParser.parseString(response.body).asJsonObject
        } catch (_: Exception) {
            throw ApiException(SubMsgBundle.message("api.invalid.response"))
        }
    }

    private fun textContent(element: JsonElement?): String = when {
        element == null || element.isJsonNull -> ""
        element.isJsonPrimitive -> element.asString
        element.isJsonArray -> element.asJsonArray.mapNotNull { item ->
            if (item.isJsonObject) item.asJsonObject.get("text")?.takeIf { it.isJsonPrimitive }?.asString else null
        }.joinToString("\n")
        else -> ""
    }
}

private class JdkApiTransport : ApiTransport {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()

    override fun send(request: ApiHttpRequest): ApiHttpResponse {
        val builder = HttpRequest.newBuilder(URI(request.url)).timeout(Duration.ofSeconds(180))
        request.headers.forEach { (name, value) -> builder.header(name, value) }
        if (request.method == "GET") builder.GET() else builder.POST(HttpRequest.BodyPublishers.ofString(request.body))
        val future = client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
        try {
            while (true) {
                request.checkCanceled()
                try {
                    val response = future.get(200, TimeUnit.MILLISECONDS)
                    return ApiHttpResponse(response.statusCode(), response.body())
                } catch (_: TimeoutException) {
                    // Poll so the IDE's cancel action also cancels the HTTP request.
                }
            }
        } finally {
            if (!future.isDone) future.cancel(true)
        }
    }
}
