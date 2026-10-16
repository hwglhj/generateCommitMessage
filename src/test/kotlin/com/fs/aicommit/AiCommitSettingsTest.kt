package com.fs.aicommit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCommitSettingsTest {

    @Test
    fun `pi provider survives state loading`() {
        val source = AiCommitSettings().apply {
            activeProvider = AiCommitSettings.PROVIDER_PI
            model = "openai/gpt-5.5 high"
        }
        val target = AiCommitSettings()

        target.loadState(source)

        assertEquals(AiCommitSettings.PROVIDER_PI, target.activeProvider)
        assertEquals(SdkDefinition.PI_SDK, target.activeSdk())
        assertEquals("openai/gpt-5.5 high", target.model)
    }

    @Test
    fun `defaults match the design`() {
        val settings = AiCommitSettings()

        assertEquals(AiCommitSettings.PROVIDER_CLAUDE, settings.activeProvider)
        assertTrue(settings.includeRecentCommits)
        assertEquals(5000, settings.maxDiffLength)
        assertEquals(AiCommitSettings.LANGUAGE_ZH, settings.outputLanguage)
        assertEquals(AiCommitSettings.MODE_CLI, settings.mode)
        assertEquals(AiCommitSettings.LANGUAGE_ZH, settings.interfaceLanguage)
    }

    @Test
    fun `api mode and interface language survive loading independently of output language`() {
        val source = AiCommitSettings().apply {
            mode = AiCommitSettings.MODE_API
            interfaceLanguage = AiCommitSettings.LANGUAGE_EN
            outputLanguage = AiCommitSettings.LANGUAGE_ZH
            apiConfigs = mutableListOf(ApiProviderConfig(id = "one", name = "Local", model = "local"))
            activeApiConfigId = "one"
        }
        val target = AiCommitSettings()
        target.loadState(source)
        assertEquals(AiCommitSettings.MODE_API, target.mode)
        assertEquals(AiCommitSettings.LANGUAGE_EN, target.interfaceLanguage)
        assertEquals(AiCommitSettings.LANGUAGE_ZH, target.outputLanguage)
        assertEquals("one", target.activeApiConfig()?.id)
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

    @Test
    fun `legacy API config gets a model list during state load`() {
        val source = AiCommitSettings().apply {
            apiConfigs = mutableListOf(ApiProviderConfig(id = "one", name = "One", model = "legacy"))
            activeApiConfigId = "one"
        }

        val target = AiCommitSettings()
        target.loadState(source)

        assertEquals(listOf("legacy"), target.apiConfigs.single().models)
    }

    @Test
    fun `generation snapshot isolates mutable API model lists`() {
        val settings = AiCommitSettings().apply {
            mode = AiCommitSettings.MODE_API
            apiConfigs = mutableListOf(
                ApiProviderConfig(
                    id = "one",
                    name = "One",
                    model = "original",
                    models = mutableListOf("original"),
                    modelsFetchedAt = 123L,
                )
            )
            activeApiConfigId = "one"
        }

        val snapshot = settings.generationSnapshot()
        settings.apiConfigs.single().models.add("changed")

        assertEquals(listOf("original"), snapshot.apiConfigs.single().models)
        assertEquals(123L, snapshot.apiConfigs.single().modelsFetchedAt)
    }
}
