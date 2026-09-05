package com.fs

import com.fs.aicommit.AiCommitSettings
import com.fs.aicommit.ProviderModelPanel
import com.fs.terminal.CliTool
import com.fs.terminal.CliToolsSettings
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * Generate Commit Message 统一配置界面
 * 包含 2 个标签页：AI Commit、CLI Tools
 */
class GenerateCommitMessageConfigurable : Configurable {

    private val aiCommitSettings = AiCommitSettings.getInstance()
    private val cliToolsSettings = CliToolsSettings.getInstance()

    private var mainPanel: JPanel? = null
    private val tabbedPane = JTabbedPane()

    // AI Commit 标签页组件
    private val commitPanel = ProviderModelPanel("CLI 与模型")
    private val commitPromptArea = JBTextArea(6, 60)

    // CLI Tools 标签页组件
    private val showClaudeCheckbox = JCheckBox("显示 Claude 图标")
    private val showCodexCheckbox = JCheckBox("显示 Codex 图标")
    private val showCursorCheckbox = JCheckBox("显示 Cursor 图标")
    private val showOpenCodeCheckbox = JCheckBox("显示 OpenCode 图标")
    private val showAntigravityCheckbox = JCheckBox("显示 Antigravity 图标")
    private val showGrokCheckbox = JCheckBox("显示 Grok 图标")
    private val showOmpCheckbox = JCheckBox("显示 OMP 图标")

    override fun getDisplayName(): String = "Generate Commit Message"

    override fun createComponent(): JComponent {
        if (mainPanel != null) return mainPanel!!

        // 创建 2 个标签页
        tabbedPane.addTab("AI Commit", createAiCommitTab())
        tabbedPane.addTab("CLI Tools", createCliToolsTab())

        mainPanel = JPanel(BorderLayout()).apply {
            border = EmptyBorder(JBUI.insets(10))
            add(tabbedPane, BorderLayout.CENTER)
        }

        reset()
        return mainPanel!!
    }

    // ============================================
    // AI Commit 标签页
    // ============================================
    private fun createAiCommitTab(): JComponent {
        commitPromptArea.lineWrap = true
        commitPromptArea.wrapStyleWord = true

        val viewBuiltinButton = JButton("查看内置提示词").apply {
            addActionListener { showBuiltinPrompt(com.fs.aicommit.AiCommitPrompts.BUILTIN_COMMIT_PROMPT) }
        }
        val clearPromptButton = JButton("清空附加提示词").apply {
            addActionListener { commitPromptArea.text = "" }
        }

        val promptButtons = JPanel(BorderLayout(10, 0)).apply {
            add(viewBuiltinButton, BorderLayout.WEST)
            add(clearPromptButton, BorderLayout.EAST)
        }

        val promptPanel = JPanel(BorderLayout(0, 6)).apply {
            add(
                FormBuilder.createFormBuilder()
                    .addComponent(JBLabel("附加提示词（叠加在内置提示词之后，AI 优先遵循，留空则仅使用内置提示词）:"))
                    .addComponent(JBScrollPane(commitPromptArea))
                    .addComponent(promptButtons)
                    .panel,
                BorderLayout.CENTER
            )
        }

        return JPanel(BorderLayout()).apply {
            border = EmptyBorder(JBUI.insets(10))
            add(
                FormBuilder.createFormBuilder()
                    .addComponent(commitPanel.panel)
                    .addVerticalGap(10)
                    .addComponent(
                        JBLabel(
                            "<html><small>首次使用请先完成 CLI 认证；OMP/Gemini 使用其自身已配置的模型提供方与凭证。</small></html>"
                        )
                    )
                    .addVerticalGap(10)
                    .addComponent(promptPanel)
                    .panel,
                BorderLayout.NORTH
            )
        }
    }

    // ============================================
    // CLI Tools 标签页
    // ============================================
    private fun createCliToolsTab(): JComponent {
        val checkboxPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("侧边栏图标显示:"))
            .addVerticalGap(5)
            .addComponent(showClaudeCheckbox)
            .addComponent(showCodexCheckbox)
            .addComponent(showCursorCheckbox)
            .addComponent(showOpenCodeCheckbox)
            .addComponent(showAntigravityCheckbox)
            .addComponent(showGrokCheckbox)
            .addComponent(showOmpCheckbox)
            .panel

        return JPanel(BorderLayout()).apply {
            border = EmptyBorder(JBUI.insets(10))
            add(checkboxPanel, BorderLayout.NORTH)
        }
    }

    // ============================================
    // 辅助方法
    // ============================================
    private fun showBuiltinPrompt(prompt: String) {
        val area = JBTextArea(prompt).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val scroll = JBScrollPane(area).apply {
            preferredSize = Dimension(JBUI.scale(640), JBUI.scale(480))
        }
        val dialog = object : com.intellij.openapi.ui.DialogWrapper(true) {
            init {
                title = "内置提示词（只读）"
                init()
            }
            override fun createCenterPanel(): JComponent = scroll
        }
        dialog.show()
    }

    // ============================================
    // Configurable 生命周期
    // ============================================
    override fun isModified(): Boolean {
        // AI Commit
        if (commitPanel.isModified(aiCommitSettings.activeProvider, aiCommitSettings.model)) return true
        if (commitPromptArea.text != aiCommitSettings.customPrompt) return true

        // CLI Tools
        if (showClaudeCheckbox.isSelected != cliToolsSettings.isShowIcon(CliTool.CLAUDE)) return true
        if (showCodexCheckbox.isSelected != cliToolsSettings.isShowIcon(CliTool.CODEX)) return true
        if (showCursorCheckbox.isSelected != cliToolsSettings.isShowIcon(CliTool.CURSOR)) return true
        if (showOpenCodeCheckbox.isSelected != cliToolsSettings.isShowIcon(CliTool.OPENCODE)) return true
        if (showAntigravityCheckbox.isSelected != cliToolsSettings.isShowIcon(CliTool.ANTIGRAVITY)) return true
        if (showGrokCheckbox.isSelected != cliToolsSettings.isShowIcon(CliTool.GROK)) return true
        if (showOmpCheckbox.isSelected != cliToolsSettings.isShowIcon(CliTool.OMP)) return true

        return false
    }

    override fun apply() {
        // AI Commit
        aiCommitSettings.activeProvider = commitPanel.selectedProvider()
        aiCommitSettings.model = commitPanel.composedModel()
        aiCommitSettings.customPrompt = commitPromptArea.text

        // CLI Tools
        cliToolsSettings.setShowIcon(CliTool.CLAUDE, showClaudeCheckbox.isSelected)
        cliToolsSettings.setShowIcon(CliTool.CODEX, showCodexCheckbox.isSelected)
        cliToolsSettings.setShowIcon(CliTool.CURSOR, showCursorCheckbox.isSelected)
        cliToolsSettings.setShowIcon(CliTool.OPENCODE, showOpenCodeCheckbox.isSelected)
        cliToolsSettings.setShowIcon(CliTool.ANTIGRAVITY, showAntigravityCheckbox.isSelected)
        cliToolsSettings.setShowIcon(CliTool.GROK, showGrokCheckbox.isSelected)
        cliToolsSettings.setShowIcon(CliTool.OMP, showOmpCheckbox.isSelected)
    }

    override fun reset() {
        // AI Commit
        commitPanel.reset(aiCommitSettings.activeProvider, aiCommitSettings.model)
        commitPromptArea.text = aiCommitSettings.customPrompt

        // CLI Tools
        showClaudeCheckbox.isSelected = cliToolsSettings.isShowIcon(CliTool.CLAUDE)
        showCodexCheckbox.isSelected = cliToolsSettings.isShowIcon(CliTool.CODEX)
        showCursorCheckbox.isSelected = cliToolsSettings.isShowIcon(CliTool.CURSOR)
        showOpenCodeCheckbox.isSelected = cliToolsSettings.isShowIcon(CliTool.OPENCODE)
        showAntigravityCheckbox.isSelected = cliToolsSettings.isShowIcon(CliTool.ANTIGRAVITY)
        showGrokCheckbox.isSelected = cliToolsSettings.isShowIcon(CliTool.GROK)
        showOmpCheckbox.isSelected = cliToolsSettings.isShowIcon(CliTool.OMP)
    }

    override fun disposeUIResources() {
        mainPanel = null
    }
}
