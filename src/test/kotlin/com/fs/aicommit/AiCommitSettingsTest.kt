package com.fs.aicommit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCommitSettingsTest {

    @Test
    fun `defaults match the design`() {
        val settings = AiCommitSettings()

        assertEquals(AiCommitSettings.PROVIDER_CLAUDE, settings.activeProvider)
        assertTrue(settings.includeRecentCommits)
        assertEquals(5000, settings.maxDiffLength)
        assertEquals(AiCommitSettings.LANGUAGE_ZH, settings.outputLanguage)
    }

    @Test
    fun `loadState copies new fields`() {
        val source = AiCommitSettings().apply {
            includeRecentCommits = false
            maxDiffLength = 8000
            outputLanguage = AiCommitSettings.LANGUAGE_EN
        }

        val target = AiCommitSettings()
        target.loadState(source)

        assertEquals(false, target.includeRecentCommits)
        assertEquals(8000, target.maxDiffLength)
        assertEquals(AiCommitSettings.LANGUAGE_EN, target.outputLanguage)
    }

    @Test
    fun `loadState normalizes unknown language to chinese`() {
        val source = AiCommitSettings().apply { outputLanguage = "fr" }

        val target = AiCommitSettings()
        target.loadState(source)

        assertEquals(AiCommitSettings.LANGUAGE_ZH, target.outputLanguage)
    }

    @Test
    fun `loadState migrates legacy per-cli model field`() {
        val source = AiCommitSettings().apply {
            activeProvider = AiCommitSettings.PROVIDER_CODEX
            model = ""
            codexModel = "gpt-5.5 high"
        }

        val target = AiCommitSettings()
        target.loadState(source)

        assertEquals("gpt-5.5 high", target.model)
        assertEquals("", target.codexModel)
    }
}
