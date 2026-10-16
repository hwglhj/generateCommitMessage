package com.fs.aicommit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickModelActionTest {
    @Test
    fun `API choices expose every saved config and selecting one activates it`() {
        val settings = AiCommitSettings().apply {
            mode = AiCommitSettings.MODE_API
            apiConfigs = mutableListOf(
                ApiProviderConfig(id = "ad", name = "ad站", model = "qwen3.8-flash"),
                ApiProviderConfig(id = "amd", name = "amd", model = "DeepSeek-V4-Flash")
            )
            activeApiConfigId = "ad"
        }

        val choices = QuickModelAction.choices(settings, emptyList())

        assertEquals(listOf("ad站 — qwen3.8-flash", "amd — DeepSeek-V4-Flash"), choices.map { it.label })
        assertTrue(choices.first().active)
        QuickModelAction.select(settings, choices[1])
        assertEquals("amd", settings.activeApiConfigId)
    }

    @Test
    fun `API sites expose normalized model choices and selecting one switches site and model`() {
        val settings = AiCommitSettings().apply {
            mode = AiCommitSettings.MODE_API
            apiConfigs = mutableListOf(
                ApiProviderConfig(id = "one", name = "One", model = "m1", models = mutableListOf("m1", "m2")),
                ApiProviderConfig(id = "two", name = "Two", model = "x1", models = mutableListOf("x1", "x2"))
            )
            activeApiConfigId = "one"
        }

        val sites = QuickModelAction.apiSites(settings)

        assertEquals(listOf("One", "Two"), sites.map { it.label })
        assertEquals(listOf("x1", "x2"), sites[1].choices.map { it.model })
        assertTrue(sites[0].active)
        assertFalse(sites[1].active)
        assertEquals("✓ One — m1", QuickModelAction.siteMenuLabel(sites[0]))
        assertEquals("Two", QuickModelAction.siteMenuLabel(sites[1]))
        assertTrue(QuickModelAction.select(settings, sites[1].choices[1]))
        assertEquals("two", settings.activeApiConfigId)
        assertEquals("x2", settings.apiConfigs[1].model)
    }

    @Test
    fun `API stale model choice cannot be selected`() {
        val settings = AiCommitSettings().apply {
            mode = AiCommitSettings.MODE_API
            apiConfigs = mutableListOf(ApiProviderConfig(id = "one", name = "One", model = "m1", models = mutableListOf("m1")))
            activeApiConfigId = "one"
        }
        val choice = QuickModelAction.apiSites(settings).single().choices.single()
        settings.apiConfigs.single().model = "other"
        settings.apiConfigs.single().models = mutableListOf("other")

        assertFalse(QuickModelAction.select(settings, choice))
        assertEquals("other", settings.apiConfigs.single().model)
    }

    @Test
    fun `API quick choices use the saved cache without a refresh step`() {
        val settings = AiCommitSettings().apply {
            mode = AiCommitSettings.MODE_API
            apiConfigs = mutableListOf(
                ApiProviderConfig(
                    id = "one",
                    name = "One",
                    model = "m1",
                    models = mutableListOf("m1", "cached"),
                )
            )
            activeApiConfigId = "one"
        }

        val site = QuickModelAction.apiSites(settings).single()

        assertEquals(listOf("m1", "cached"), site.choices.map { it.model })
        assertEquals(listOf("m1", "cached"), settings.apiConfigs.single().models)
    }

    @Test
    fun `refresh gate blocks only duplicate requests for the same site`() {
        assertTrue(QuickModelAction.beginApiRefresh("one"))
        try {
            assertFalse(QuickModelAction.beginApiRefresh("one"))
            assertTrue(QuickModelAction.beginApiRefresh("two"))
            try {
                assertTrue(QuickModelAction.isApiRefreshRunning("one"))
                assertTrue(QuickModelAction.isApiRefreshRunning("two"))
            } finally {
                QuickModelAction.endApiRefresh("two")
            }
        } finally {
            QuickModelAction.endApiRefresh("one")
        }
        assertFalse(QuickModelAction.isApiRefreshRunning("one"))
        assertFalse(QuickModelAction.isApiRefreshRunning("two"))
    }

    @Test
    fun `open site popup replaces its visible actions from the latest cache`() {
        val visibleActions = mutableListOf("refresh", "old-model")
        var synced = false

        QuickModelPopupItems.replaceValues(visibleActions, listOf("refresh", "new-a", "new-b")) {
            synced = true
        }

        assertEquals(listOf("refresh", "new-a", "new-b"), visibleActions)
        assertTrue(synced)
    }

    @Test
    fun `CLI choices include the current model and default when they differ`() {
        val settings = AiCommitSettings().apply {
            mode = AiCommitSettings.MODE_CLI
            activeProvider = AiCommitSettings.PROVIDER_CODEX
            model = "gpt-5.6-sol"
        }

        val choices = QuickModelAction.choices(settings, listOf("gpt-5.5"))

        assertEquals(listOf("gpt-5.6-sol", "gpt-5.5"), choices.map { it.label })
        QuickModelAction.select(settings, choices[1])
        assertEquals("gpt-5.5", settings.model)
    }

    @Test
    fun `CLI model switch preserves thinking level and deduplicates candidates`() {
        val settings = AiCommitSettings().apply { model = "custom-model high" }
        val choices = QuickModelAction.choices(settings, listOf("other-model", "other-model", " "))
        assertEquals(listOf("custom-model high", "other-model high"), choices.map { it.label })
        assertTrue(QuickModelAction.select(settings, choices.last()))
        assertEquals("other-model high", settings.model)
    }

    @Test
    fun `deleted config and stale provider choices cannot be selected`() {
        val settings = AiCommitSettings().apply {
            mode = AiCommitSettings.MODE_API
            apiConfigs.add(ApiProviderConfig(id = "one", name = "One", model = "one"))
        }
        val apiChoice = QuickModelAction.choices(settings, emptyList()).single()
        settings.apiConfigs.clear()
        assertFalse(QuickModelAction.select(settings, apiChoice))
        assertTrue(QuickModelAction.choices(settings, emptyList()).isEmpty())
        settings.mode = AiCommitSettings.MODE_CLI
        val cliChoice = QuickModelAction.choices(settings, listOf("other-model")).last()
        settings.activeProvider = AiCommitSettings.PROVIDER_CODEX
        assertFalse(QuickModelAction.select(settings, cliChoice))
        assertEquals("", settings.model)
    }

    @Test
    fun `snapshot keeps original provider and API values after settings change`() {
        val settings = AiCommitSettings().apply {
            mode = AiCommitSettings.MODE_API
            apiConfigs.add(ApiProviderConfig(id = "one", name = "One", apiKey = "key", model = "original"))
            activeApiConfigId = "one"
            customPrompt = "original prompt"
        }
        val snapshot = settings.generationSnapshot()
        settings.apiConfigs.single().model = "changed"
        settings.apiConfigs.clear()
        settings.mode = AiCommitSettings.MODE_CLI
        settings.customPrompt = "changed prompt"
        assertEquals(AiCommitSettings.MODE_API, snapshot.mode)
        assertEquals("original", snapshot.activeApiConfig()!!.model)
        assertEquals("key", snapshot.activeApiConfig()!!.apiKey)
        assertEquals("original prompt", snapshot.customPrompt)
    }

    @Test
    fun `quick switching is blocked while a generation is running`() {
        val settings = AiCommitSettings().apply { model = "before" }
        val choice = QuickModelAction.choices(settings, listOf("after")).last()
        val token = CommitGenerationTask.gate.acquire()!!
        try {
            assertFalse(QuickModelAction.select(settings, choice))
            assertEquals("before", settings.model)
        } finally {
            token.close()
        }
        assertTrue(QuickModelAction.select(settings, choice))
        assertEquals("after", settings.model)
    }

    @Test
    fun `CLI without an explicit default keeps an implicit default choice`() {
        val settings = AiCommitSettings().apply { activeProvider = AiCommitSettings.PROVIDER_OPENCODE }
        val choices = QuickModelAction.choices(settings, listOf("provider/custom"))
        assertEquals(listOf("", "provider/custom"), choices.map { it.model })
        assertTrue(choices.first().active)
        QuickModelAction.select(settings, choices.last())
        QuickModelAction.select(settings, choices.first())
        assertEquals("", settings.model)
    }
}
