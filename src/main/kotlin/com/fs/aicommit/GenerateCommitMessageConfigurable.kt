package com.fs.aicommit

import com.fs.terminal.CliToolsPanel
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import javax.swing.JTabbedPane

/**
 * Generate Commit Message 统一配置页面（Settings → Tools → Generate Commit Message）。
 * 包含两个标签页：AI Commit（[AiCommitPanel]）和 CLI Tools（[CliToolsPanel]）。
 */
class GenerateCommitMessageConfigurable : Configurable {

    private var tabbedPane: JTabbedPane? = null
    private var aiCommitPanel: AiCommitPanel? = null
    private var cliToolsPanel: CliToolsPanel? = null

    override fun getDisplayName(): String = "Generate Commit Message"

    override fun createComponent(): JComponent {
        tabbedPane?.let { return it }

        val aiCommit = AiCommitPanel()
        val cliTools = CliToolsPanel()
        val pane = JTabbedPane().apply {
            addTab("AI Commit", aiCommit.panel)
            addTab("CLI Tools", cliTools.panel)
        }
        aiCommitPanel = aiCommit
        cliToolsPanel = cliTools
        tabbedPane = pane

        reset()
        return pane
    }

    override fun isModified(): Boolean =
        aiCommitPanel?.isModified() == true || cliToolsPanel?.isModified() == true

    override fun apply() {
        aiCommitPanel?.apply()
        cliToolsPanel?.apply()
    }

    override fun reset() {
        aiCommitPanel?.reset()
        cliToolsPanel?.reset()
    }

    override fun disposeUIResources() {
        tabbedPane = null
        aiCommitPanel = null
        cliToolsPanel = null
    }
}
