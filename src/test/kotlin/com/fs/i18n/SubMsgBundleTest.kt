package com.fs.i18n

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.ResourceBundle

class SubMsgBundleTest {
    @Test
    fun `chinese is the default and unknown languages fall back to it`() {
        assertEquals("使用模式", SubMsgBundle.messageFor("zh", "aicommit.mode"))
        assertEquals("使用模式", SubMsgBundle.messageFor("fr", "aicommit.mode"))
    }

    @Test
    fun `english and parameterized messages use selected language`() {
        assertEquals("Mode", SubMsgBundle.messageFor("en", "aicommit.mode"))
        assertEquals("Connection failed: offline", SubMsgBundle.messageFor("en", "message.test.failed", "offline"))
    }

    @Test
    fun `english is independent of the operating system locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
            assertEquals("Mode", SubMsgBundle.messageFor("en", "aicommit.mode"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `english and chinese bundles expose matching API transfer keys`() {
        val control = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)
        val english = ResourceBundle.getBundle("messages.SubMsgBundle", Locale.ROOT, control)
        val chinese = ResourceBundle.getBundle("messages.SubMsgBundle", Locale.SIMPLIFIED_CHINESE, control)
        val required = setOf(
            "aicommit.api.import",
            "aicommit.api.export",
            "aicommit.api.export.keys",
            "aicommit.api.export.keys.warning",
            "aicommit.api.export.keys.confirm",
            "aicommit.api.export.title",
            "aicommit.api.import.title",
            "aicommit.api.import.success",
            "aicommit.api.export.success",
            "aicommit.api.transfer.error",
            "aicommit.api.refresh.models",
            "action.quick.model.empty.site",
            "action.quick.model.refresh.site",
            "action.quick.model.refreshing",
            "action.quick.model.cache",
            "action.quick.model.cache.unknown",
            "action.quick.model.cache.empty",
            "message.model.refreshing.site",
            "message.model.refreshed.site",
            "message.model.failed.retained",
            "message.model.discarded",
        )

        assertEquals(required, required.filter(english::containsKey).toSet())
        assertEquals(required, required.filter(chinese::containsKey).toSet())
    }
}
