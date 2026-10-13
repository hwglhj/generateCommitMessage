package com.fs.aicommit

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiProviderConfigTest {
    @Test
    fun `normalization keeps current model first and removes blanks and duplicates`() {
        val config = ApiProviderConfig(
            model = "model-b",
            models = mutableListOf("model-a", "", "model-b", "model-a", "  ")
        )

        config.normalizeModels()

        assertEquals(listOf("model-b", "model-a"), config.models)
    }

    @Test
    fun `deep copy isolates mutable model list`() {
        val source = ApiProviderConfig(model = "one", models = mutableListOf("one", "two"))
        val copy = source.deepCopy()

        source.models.add("three")

        assertEquals(listOf("one", "two"), copy.models)
    }
}
