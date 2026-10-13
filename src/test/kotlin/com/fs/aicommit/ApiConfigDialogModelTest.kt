package com.fs.aicommit

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiConfigDialogModelTest {
    @Test
    fun `merged models retain current fetched and saved values in stable order`() {
        assertEquals(
            listOf("current", "fetched-a", "saved-a"),
            ApiConfigDialog.mergeModels(
                current = "current",
                fetched = listOf("fetched-a", "current", ""),
                saved = listOf("saved-a", "fetched-a")
            )
        )
    }

    @Test
    fun `merged models trim values and drop blanks`() {
        assertEquals(
            listOf("manual", "remote"),
            ApiConfigDialog.mergeModels(
                current = " manual ",
                fetched = listOf(" remote ", " "),
                saved = emptyList()
            )
        )
    }
}
