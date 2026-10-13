package com.fs.aicommit

import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderModelSelectionTest {
    @Test
    fun `reset preserves an empty model so CLI defaults remain implicit`() {
        val selection = ProviderModelSelection()
        selection.reset("claude", "")
        assertEquals("", selection.composedModel())
    }

    @Test
    fun `clearing a model does not restore the previous choice`() {
        val selection = ProviderModelSelection()
        selection.reset("claude", "custom-model high")
        selection.remember("", "high")
        assertEquals("", selection.composedModel())
        selection.select("codex")
        selection.select("claude")
        assertEquals("", selection.model)
    }

    @Test
    fun `provider switches preserve separate model and thinking selections`() {
        val selection = ProviderModelSelection()
        selection.reset("claude", "custom-claude high")
        selection.select("codex")
        selection.remember("custom-codex", "low")
        selection.select("claude")
        assertEquals("custom-claude high", selection.composedModel())
        selection.select("codex")
        assertEquals("custom-codex low", selection.composedModel())
    }

    @Test
    fun `reset discards edits for all providers`() {
        val selection = ProviderModelSelection()
        selection.reset("claude", "saved high")
        selection.select("codex")
        selection.remember("unsaved", "low")
        selection.reset("claude", "saved high")
        assertEquals("saved high", selection.composedModel())
        selection.select("codex")
        assertEquals("", selection.composedModel())
    }
}
