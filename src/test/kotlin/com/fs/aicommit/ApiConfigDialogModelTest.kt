package com.fs.aicommit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiConfigDialogModelTest {
    @Test
    fun `current model is placed before cached models`() {
        assertEquals(
            listOf("current", "cached-a"),
            ApiConfigDialog.mergeCurrentWithCache(
                current = "current",
                cached = listOf("cached-a", "current", "")
            )
        )
    }

    @Test
    fun `merged models trim values and drop blanks`() {
        assertEquals(
            listOf("manual", "remote"),
            ApiConfigDialog.mergeCurrentWithCache(
                current = " manual ",
                cached = listOf(" remote ", " ")
            )
        )
    }

    @Test
    fun `cached models belong only to the provider and endpoint that produced them`() {
        assertTrue(ApiConfigDialog.cacheMatches(
            provider = " openai ",
            baseUrl = " https://one.example/v1 ",
            cachedProvider = "openai",
            cachedBaseUrl = "https://one.example/v1",
        ))
        assertFalse(ApiConfigDialog.cacheMatches(
            provider = "anthropic",
            baseUrl = "https://one.example/v1",
            cachedProvider = "openai",
            cachedBaseUrl = "https://one.example/v1",
        ))
        assertFalse(ApiConfigDialog.cacheMatches(
            provider = "openai",
            baseUrl = "https://two.example/v1",
            cachedProvider = "openai",
            cachedBaseUrl = "https://one.example/v1",
        ))
    }
}
