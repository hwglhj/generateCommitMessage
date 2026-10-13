package com.fs.i18n

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

object SubMsgBundle {
    @Volatile var language: String = "zh"

    fun message(key: String, vararg arguments: Any?): String = messageFor(language, key, *arguments)

    fun messageFor(language: String, key: String, vararg arguments: Any?): String {
        val locale = if (language == "en") Locale.ROOT else Locale.SIMPLIFIED_CHINESE
        val bundle = ResourceBundle.getBundle("messages.SubMsgBundle", locale,
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT))
        val pattern = if (bundle.containsKey(key)) bundle.getString(key) else key
        return MessageFormat(pattern, locale).format(arguments)
    }
}
