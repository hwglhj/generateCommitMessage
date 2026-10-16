package com.fs.aicommit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiModelCacheTest {
    @Test
    fun `successful refresh replaces stale cache but preserves current model`() {
        val config = ApiProviderConfig(
            id = "one",
            provider = ApiProviderConfig.OPENAI_COMPATIBLE,
            baseUrl = "https://one.example/v1",
            model = "manual",
            models = mutableListOf("manual", "stale"),
            modelsFetchedAt = 10L,
        )

        val update = ApiModelCache.createUpdate(
            config,
            listOf(" remote-a ", "manual", "remote-b", "remote-a"),
            20L,
        )!!

        assertTrue(ApiModelCache.apply(listOf(config), update))
        assertEquals(listOf("manual", "remote-a", "remote-b"), config.models)
        assertEquals(20L, config.modelsFetchedAt)
    }

    @Test
    fun `apply preserves the current model selected while refresh was running`() {
        val requested = ApiProviderConfig(
            id = "one",
            provider = ApiProviderConfig.OPENAI_COMPATIBLE,
            baseUrl = "https://one.example/v1",
            model = "A",
            models = mutableListOf("A", "B"),
        )
        val update = ApiModelCache.createUpdate(requested, listOf("C"), 20L)!!
        val current = requested.deepCopy().apply { model = "B" }

        assertTrue(ApiModelCache.apply(listOf(current), update))
        assertEquals(listOf("B", "C"), current.models)
    }

    @Test
    fun `empty refresh produces no update and keeps prior cache`() {
        val config = ApiProviderConfig(
            model = "current",
            models = mutableListOf("current", "cached"),
            modelsFetchedAt = 10L,
        )

        assertNull(ApiModelCache.createUpdate(config, listOf(" ", ""), 20L))
        assertEquals(listOf("current", "cached"), config.models)
        assertEquals(10L, config.modelsFetchedAt)
    }

    @Test
    fun `stale refresh is discarded when endpoint identity changes`() {
        val requested = ApiProviderConfig(
            id = "one",
            provider = ApiProviderConfig.OPENAI_COMPATIBLE,
            baseUrl = "https://old.example/v1",
            model = "m1",
        )
        val update = ApiModelCache.createUpdate(requested, listOf("m2"), 20L)!!
        val current = requested.deepCopy().copy(baseUrl = "https://new.example/v1")

        assertFalse(ApiModelCache.apply(listOf(current), update))
        assertEquals(listOf("m1"), current.availableModels())
        assertEquals(0L, current.modelsFetchedAt)
    }

    @Test
    fun `stale refresh is discarded when provider changes`() {
        val requested = ApiProviderConfig(
            id = "one",
            provider = ApiProviderConfig.OPENAI_COMPATIBLE,
            baseUrl = "https://one.example/v1",
            model = "m1",
        )
        val update = ApiModelCache.createUpdate(requested, listOf("m2"), 20L)!!
        val current = requested.deepCopy().copy(provider = ApiProviderConfig.ANTHROPIC)

        assertFalse(ApiModelCache.apply(listOf(current), update))
        assertEquals(listOf("m1"), current.availableModels())
        assertEquals(0L, current.modelsFetchedAt)
    }
}
