package com.fs.aicommit

import org.junit.Assert.assertEquals
import org.junit.Test
import javax.swing.JComboBox
import javax.swing.JTextField
import javax.swing.SwingUtilities

class ProviderModelPanelTest {
    @Test
    fun `model search preserves spaces while typing`() {
        @Suppress("UNCHECKED_CAST")
        val cache = ProviderModelPanel::class.java.getDeclaredField("sharedModelCache").apply {
            isAccessible = true
        }.get(null) as MutableMap<String, List<String>>
        var previousCandidates: List<String>? = null
        var cacheChanged = false
        var panel: ProviderModelPanel? = null
        lateinit var editor: JTextField

        try {
            SwingUtilities.invokeAndWait {
                // Seed candidates without reset(), which starts a CLI or network model lookup.
                previousCandidates = cache.put(AiCommitSettings.PROVIDER_CLAUDE, listOf("Gemini 3.1 Pro (High)"))
                cacheChanged = true
                panel = ProviderModelPanel("Model input")
                val combo = ProviderModelPanel::class.java.getDeclaredField("modelCombo").apply {
                    isAccessible = true
                }.get(panel) as JComboBox<*>
                editor = combo.editor.editorComponent as JTextField
                editor.text = "Gemini "
            }

            var textAfterSpace = ""
            SwingUtilities.invokeAndWait { textAfterSpace = editor.text }
            assertEquals("Gemini ", textAfterSpace)

            SwingUtilities.invokeAndWait {
                editor.caretPosition = editor.document.length
                editor.replaceSelection("3.1 Pro (High)")
            }
            var composedModel = ""
            SwingUtilities.invokeAndWait { composedModel = panel!!.composedModel() }
            assertEquals("Gemini 3.1 Pro (High)", composedModel)

            SwingUtilities.invokeAndWait {
                editor.caretPosition = "Gemini ".length
                editor.replaceSelection("Custom ")
            }
            var caretAfterInsertion = -1
            SwingUtilities.invokeAndWait { caretAfterInsertion = editor.caretPosition }
            assertEquals("Gemini Custom ".length, caretAfterInsertion)
        } finally {
            SwingUtilities.invokeAndWait {
                panel?.dispose()
                if (cacheChanged) {
                    if (previousCandidates == null) cache.remove(AiCommitSettings.PROVIDER_CLAUDE)
                    else cache[AiCommitSettings.PROVIDER_CLAUDE] = previousCandidates!!
                }
            }
        }
    }
}
