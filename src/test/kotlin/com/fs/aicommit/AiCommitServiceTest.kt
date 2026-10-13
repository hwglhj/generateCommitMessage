package com.fs.aicommit

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class AiCommitServiceTest {
    @Test
    fun `pi command is one shot stateless and tool free`() {
        assertEquals(
            listOf(
                "C:/tools/pi.cmd", "--print", "--no-session", "--no-tools",
                "--no-context-files", "--no-approve", "Summarize"
            ),
            AiCommitService.buildPiCommand("C:/tools/pi.cmd", "Summarize", "", "")
        )
    }

    @Test
    fun `pi command includes selected model and thinking level`() {
        assertEquals(
            listOf(
                "pi", "--print", "--no-session", "--no-tools", "--no-context-files", "--no-approve",
                "--model", "anthropic/claude-sonnet-4-6", "--thinking", "high", "Prompt"
            ),
            AiCommitService.buildPiCommand("pi", "Prompt", "anthropic/claude-sonnet-4-6", "high")
        )
    }

    @Test
    fun `inserting a line does not report unchanged following lines as replacements`() {
        val service = AiCommitService(null, AiCommitSettings())
        val before = (1..100).joinToString("\n") { "unchanged-$it" }
        val diff = service.generateSimpleDiff(before, "inserted\n$before\nlate addition")
        assertTrue(diff.contains("+ inserted"))
        assertTrue(diff.contains("+ late addition"))
        assertFalse(diff.contains("- unchanged"))
    }

    @Test
    fun `API generation includes configured context without detecting a CLI`() {
        val settings = AiCommitSettings().apply {
            mode = AiCommitSettings.MODE_API
            cliPath = "Z:/missing/cli.exe"
            customPrompt = "Use ticket ABC-123"
            outputLanguage = AiCommitSettings.LANGUAGE_EN
            apiConfigs.add(ApiProviderConfig(id = "active", name = "Test", apiKey = "test-key", model = "test-model"))
            activeApiConfigId = "active"
        }
        var sent = ""
        val client = ApiClient { request ->
            sent = JsonParser.parseString(request.body).asJsonObject.getAsJsonArray("messages")[0]
                .asJsonObject.get("content").asString
            ApiHttpResponse(200, """{"choices":[{"message":{"content":"fix: preserve selection"}}]}""")
        }
        val result = AiCommitService(null, settings, client).generateFromDiff("+ selected change", "fix: recent style")
        assertEquals("fix: preserve selection", result)
        assertTrue(sent.contains("selected change"))
        assertTrue(sent.contains("recent style"))
        assertTrue(sent.contains("ABC-123"))
        assertTrue(sent.contains("English"))
    }

    @Test
    fun `explicit empty commit selection prevents fallback to all changes`() {
        val selection = GenerateCommitMessageAction.selectionFrom(emptyList<Any>(), emptyList<Any>())
        assertNotNull(selection)
        assertTrue(selection!!.isEmpty())
        assertNull(GenerateCommitMessageAction.selectionFrom(null, null))
    }
}
