package com.fs.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CliToolTest {
    @Test
    fun `pi terminal uses plain pi command without fake permission mode`() {
        assertEquals("Pi", CliTool.PI.id)
        assertEquals("pi", CliTool.PI.command(false))
        assertEquals("pi", CliTool.PI.command(true))
        assertFalse(CliTool.PI.supportsHighPermission)
    }

    @Test
    fun `existing cli permission commands remain available`() {
        assertTrue(CliTool.CODEX.supportsHighPermission)
        assertTrue(CliTool.CODEX.command(true).contains("danger-full-access"))
    }

    @Test
    fun `pi icon is visible by default`() {
        val settings = CliToolsSettings()
        assertTrue(settings.isShowIcon(CliTool.PI))
        assertFalse(settings.isHighPermission(CliTool.PI))
    }
}
