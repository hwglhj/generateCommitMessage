package com.fs.aicommit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCommitPromptsTest {

    private val diff = "=== MODIFICATION: a.kt ===\n- old line\n+ new line"

    @Test
    fun `default prompt is chinese and contains builtin prompt and diff`() {
        val prompt = AiCommitPrompts.buildFullPrompt(diff, "", "")

        assertTrue(prompt.startsWith(AiCommitPrompts.BUILTIN_COMMIT_PROMPT))
        assertTrue(prompt.contains(diff))
        assertTrue(prompt.contains("type(scope): 中文描述"))
        assertFalse(prompt.contains(AiCommitPrompts.ENGLISH_OUTPUT_SECTION_TITLE))
    }

    @Test
    fun `english output adds override section and english example`() {
        val prompt = AiCommitPrompts.buildFullPrompt(diff, "", "", AiCommitSettings.LANGUAGE_EN)

        assertTrue(prompt.contains(AiCommitPrompts.ENGLISH_OUTPUT_SECTION_TITLE))
        assertTrue(prompt.contains("type(scope): short English description"))
        assertFalse(prompt.contains("type(scope): 中文描述"))
    }

    @Test
    fun `unknown language falls back to chinese`() {
        val prompt = AiCommitPrompts.buildFullPrompt(diff, "", "", "fr")

        assertFalse(prompt.contains(AiCommitPrompts.ENGLISH_OUTPUT_SECTION_TITLE))
        assertTrue(prompt.contains("type(scope): 中文描述"))
    }

    @Test
    fun `recent commits section only appears when provided`() {
        val without = AiCommitPrompts.buildFullPrompt(diff, "", "")
        val with = AiCommitPrompts.buildFullPrompt(diff, "feat(ui): 添加设置面板\n\n- 新增标签页", "")

        assertFalse(without.contains(AiCommitPrompts.RECENT_COMMITS_SECTION_TITLE))
        assertTrue(with.contains(AiCommitPrompts.RECENT_COMMITS_SECTION_TITLE))
        assertTrue(with.contains("feat(ui): 添加设置面板"))
    }

    @Test
    fun `user additional prompt is appended when not blank`() {
        val without = AiCommitPrompts.buildFullPrompt(diff, "", "   ")
        val with = AiCommitPrompts.buildFullPrompt(diff, "", "scope 固定为 core")

        assertFalse(without.contains(AiCommitPrompts.USER_PROMPT_SECTION_TITLE))
        assertTrue(with.contains(AiCommitPrompts.USER_PROMPT_SECTION_TITLE))
        assertTrue(with.contains("scope 固定为 core"))
    }
}
