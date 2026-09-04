package com.fs.aicommit

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

/**
 * AI Commit 设置面板：
 * 1. AI Commit 的 CLI 与模型（[ProviderModelPanel]）
 * 2. 生成提示词配置（附加提示词 + 查看内置提示词）
 *
 * 行内 AI 修改的 CLI 配置在独立的 [InlineEditConfigurable] 页面。
 */
class AiCommitConfigurable : Configurable {

    private val settings = AiCommitSettings.getInstance()

    private var mainPanel: JPanel? = null
    private val commitPanel = ProviderModelPanel("CLI 与模型")
    private val promptArea = JBTextArea(6, 60)

    override fun getDisplayName(): String = "AI Commit 设置"

    override fun createComponent(): JComponent {
        if (mainPanel != null) return mainPanel!!

        promptArea.lineWrap = true
        promptArea.wrapStyleWord = true
        val viewBuiltinButton = JButton("查看内置提示词").apply {
            addActionListener { showBuiltinPrompt() }
        }
        val clearPromptButton = JButton("清空附加提示词").apply {
            addActionListener { promptArea.text = "" }
        }
        val promptButtons = JPanel(BorderLayout(10, 0)).apply {
            add(viewBuiltinButton, BorderLayout.WEST)
            add(clearPromptButton, BorderLayout.EAST)
        }
        val promptPanel = JPanel(BorderLayout(0, 6)).apply {
            add(
                FormBuilder.createFormBuilder()
                    .addComponent(JBLabel("附加提示词（叠加在内置提示词之后，AI 优先遵循，留空则仅使用内置提示词）:"))
                    .addComponent(JBScrollPane(promptArea))
                    .addComponent(promptButtons)
                    .panel,
                BorderLayout.CENTER
            )
        }

        val content = FormBuilder.createFormBuilder()
            .addComponent(commitPanel.panel)
            .addVerticalGap(10)
            .addComponent(
                JBLabel(
                    "<html><small>首次使用请先完成 CLI 认证；OMP/Gemini 使用其自身已配置的模型提供方与凭证。</small></html>"
                )
            )
            .addVerticalGap(10)
            .addComponent(promptPanel)
            .panel

        mainPanel = JPanel(BorderLayout()).apply {
            border = EmptyBorder(JBUI.insets(10))
            add(content, BorderLayout.NORTH)
        }

        reset()
        return mainPanel!!
    }

    private fun showBuiltinPrompt() {
        val area = JBTextArea(AiCommitPrompts.BUILTIN_COMMIT_PROMPT).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val scroll = JBScrollPane(area).apply {
            preferredSize = Dimension(JBUI.scale(640), JBUI.scale(480))
        }
        val dialog = object : DialogWrapper(true) {
            init {
                title = "内置提示词（只读）"
                init()
            }

            override fun createCenterPanel(): JComponent = scroll
        }
        dialog.show()
    }

    // ---------------------------------------------------------------
    // Configurable 生命周期
    // ---------------------------------------------------------------

    override fun isModified(): Boolean {
        if (commitPanel.isModified(settings.activeProvider, settings.model)) return true
        if (promptArea.text != settings.customPrompt) return true
        return false
    }

    override fun apply() {
        settings.activeProvider = commitPanel.selectedProvider()
        settings.model = commitPanel.composedModel()
        settings.customPrompt = promptArea.text
    }

    override fun reset() {
        commitPanel.reset(settings.activeProvider, settings.model)
        promptArea.text = settings.customPrompt
    }

    override fun disposeUIResources() {
        mainPanel = null
    }
}
