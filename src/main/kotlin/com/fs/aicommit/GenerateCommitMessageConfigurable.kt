package com.fs.aicommit

import com.fs.terminal.CliToolsPanel
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import javax.swing.JTabbedPane
import javax.swing.JComboBox
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.FlowLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.fs.i18n.SubMsgBundle
import com.fs.i18n.SubMsgBundle.message as tr

/**
 * Generate Commit Message 统一配置页面（Settings → Tools → Generate Commit Message）。
 * 包含两个标签页：AI Commit（[AiCommitPanel]）和 CLI Tools（[CliToolsPanel]）。
 */
class GenerateCommitMessageConfigurable : Configurable {

    private var tabbedPane: JTabbedPane? = null
    private var aiCommitPanel: AiCommitPanel? = null
    private var cliToolsPanel: CliToolsPanel? = null
    private var mainPanel: JPanel? = null
    private var languageCombo: JComboBox<String>? = null
    private var editorCombo: JComboBox<String>? = null
    private val editorIds = listOf("code", "cursor", "antigravity", "windsurf")

    override fun getDisplayName(): String = "Generate Commit Message"

    override fun createComponent(): JComponent {
        mainPanel?.let { return it }
        SubMsgBundle.language = AiCommitSettings.getInstance().interfaceLanguage

        val aiCommit = AiCommitPanel()
        val cliTools = CliToolsPanel()
        val pane = JTabbedPane().apply {
            addTab("AI Commit", JBScrollPane(aiCommit.panel))
            addTab("CLI Tools", JBScrollPane(cliTools.panel))
        }
        aiCommitPanel = aiCommit
        cliToolsPanel = cliTools
        tabbedPane = pane
        languageCombo = JComboBox(arrayOf(tr("language.chinese"), tr("language.english")))
        editorCombo = JComboBox(arrayOf("VS Code", "Cursor", "Antigravity", "Windsurf"))
        mainPanel = JPanel(BorderLayout(0, 10)).apply {
            val options = JPanel(java.awt.GridLayout(0, 1, 0, 4))
            options.add(JPanel(FlowLayout(FlowLayout.LEFT, 8, 6)).apply {
                add(JBLabel(tr("app.language")))
                add(languageCombo)
            })
            options.add(JPanel(FlowLayout(FlowLayout.LEFT, 8, 6)).apply {
                add(JBLabel(tr("settings.external.editor")))
                add(editorCombo)
            })
            add(options, BorderLayout.NORTH)
            add(pane, BorderLayout.CENTER)
        }

        reset()
        return mainPanel!!
    }

    override fun isModified(): Boolean =
        mainPanel != null && (aiCommitPanel?.isModified() == true || cliToolsPanel?.isModified() == true ||
            selectedLanguage() != AiCommitSettings.getInstance().interfaceLanguage ||
            selectedEditor() != AiCommitSettings.getInstance().externalEditor)

    override fun apply() {
        if (mainPanel == null) return
        aiCommitPanel?.apply()
        cliToolsPanel?.apply()
        val languageChanged = AiCommitSettings.getInstance().interfaceLanguage != selectedLanguage()
        AiCommitSettings.getInstance().interfaceLanguage = selectedLanguage()
        AiCommitSettings.getInstance().externalEditor = selectedEditor()
        SubMsgBundle.language = selectedLanguage()
        if (languageChanged) {
            val outer = mainPanel!!
            val selectedTab = tabbedPane?.selectedIndex ?: 0
            aiCommitPanel?.dispose()
            mainPanel = null
            val rebuilt = createComponent() as JPanel
            val layout = rebuilt.layout as BorderLayout
            outer.removeAll()
            outer.add(layout.getLayoutComponent(BorderLayout.NORTH), BorderLayout.NORTH)
            outer.add(layout.getLayoutComponent(BorderLayout.CENTER), BorderLayout.CENTER)
            mainPanel = outer
            tabbedPane?.selectedIndex = selectedTab
            outer.revalidate()
            outer.repaint()
        }
    }

    override fun reset() {
        aiCommitPanel?.reset()
        cliToolsPanel?.reset()
        languageCombo?.selectedIndex = if (AiCommitSettings.getInstance().interfaceLanguage == "en") 1 else 0
        editorCombo?.selectedIndex = editorIds.indexOf(AiCommitSettings.getInstance().externalEditor).coerceAtLeast(0)
    }

    override fun disposeUIResources() {
        aiCommitPanel?.dispose()
        tabbedPane = null
        aiCommitPanel = null
        cliToolsPanel = null
        mainPanel = null
        languageCombo = null
        editorCombo = null
    }

    private fun selectedLanguage() = if (languageCombo?.selectedIndex == 1) "en" else "zh"
    private fun selectedEditor() = editorIds.getOrElse(editorCombo?.selectedIndex ?: 0) { "code" }
}
