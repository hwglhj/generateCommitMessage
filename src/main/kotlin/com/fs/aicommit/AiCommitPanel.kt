package com.fs.aicommit

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

/**
 * AI Commit 配置面板（统一设置页的 "AI Commit" 标签页）：
 * 1. CLI 与模型（[ProviderModelPanel]）
 * 2. 附加提示词 + 查看内置提示词
 * 3. 增强选项：近期提交风格参考 / Diff 最大长度 / 输出语言
 *
 * 数据存储在 [AiCommitSettings]。
 */
class AiCommitPanel {

    val panel: JPanel
    private val settings = AiCommitSettings.getInstance()

    // CLI 与模型
    private val providerModelPanel = ProviderModelPanel("CLI 与模型")

    // 附加提示词
    private val promptArea = JBTextArea(6, 60)

    // 增强选项
    private val includeRecentCommitsCheckBox = JBCheckBox(
        "包含近期提交风格参考 (最近 ${AiCommitSettings.RECENT_COMMITS_COUNT} 条)",
        true
    )
    private val maxDiffLengthField = JBTextField(AiCommitSettings.DEFAULT_MAX_DIFF_LENGTH.toString(), 10)
    private val outputLanguageZh = JRadioButton("中文", true)
    private val outputLanguageEn = JRadioButton("英文", false)

    init {
        ButtonGroup().apply {
            add(outputLanguageZh)
            add(outputLanguageEn)
        }
        panel = createPanel()
    }

    private fun createPanel(): JPanel {
        // 1. 附加提示词区域
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

        // 2. 增强选项区域
        val languagePanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(outputLanguageZh)
            add(outputLanguageEn)
        }
        val maxDiffPanel = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(maxDiffLengthField)
            add(JBLabel("字符 (建议范围: 1000-10000)"))
        }
        val enhancedOptionsPanel = FormBuilder.createFormBuilder()
            .addComponent(includeRecentCommitsCheckBox)
            .addVerticalGap(6)
            .addLabeledComponent(JBLabel("Diff 最大长度:"), maxDiffPanel)
            .addVerticalGap(6)
            .addLabeledComponent(JBLabel("输出语言:"), languagePanel)
            .panel
        enhancedOptionsPanel.border = TitledBorder("增强选项")

        // 3. 组装
        val content = FormBuilder.createFormBuilder()
            .addComponent(providerModelPanel.panel)
            .addVerticalGap(6)
            .addComponent(
                JBLabel("<html><small>首次使用请先完成 CLI 认证；OMP/Gemini 使用其自身已配置的模型提供方与凭证。</small></html>")
            )
            .addVerticalGap(10)
            .addComponent(promptPanel)
            .addVerticalGap(10)
            .addComponent(enhancedOptionsPanel)
            .panel

        return JPanel(BorderLayout()).apply {
            border = EmptyBorder(JBUI.insets(10))
            add(content, BorderLayout.NORTH)
        }
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
    // 与 Configurable 生命周期对接
    // ---------------------------------------------------------------

    fun isModified(): Boolean {
        if (providerModelPanel.isModified(settings.activeProvider, settings.model)) return true
        if (promptArea.text != settings.customPrompt) return true
        if (includeRecentCommitsCheckBox.isSelected != settings.includeRecentCommits) return true
        if (maxDiffLengthField.text.trim() != settings.maxDiffLength.toString()) return true
        if (selectedLanguage() != settings.outputLanguage) return true
        return false
    }

    /** 校验失败时抛 [ConfigurationException]，由设置对话框提示用户 */
    fun apply() {
        val maxDiffLength = parseMaxDiffLength()
        settings.activeProvider = providerModelPanel.selectedProvider()
        settings.model = providerModelPanel.composedModel()
        settings.customPrompt = promptArea.text
        settings.includeRecentCommits = includeRecentCommitsCheckBox.isSelected
        settings.maxDiffLength = maxDiffLength
        settings.outputLanguage = selectedLanguage()
    }

    fun reset() {
        providerModelPanel.reset(settings.activeProvider, settings.model)
        promptArea.text = settings.customPrompt
        includeRecentCommitsCheckBox.isSelected = settings.includeRecentCommits
        maxDiffLengthField.text = settings.maxDiffLength.toString()
        if (settings.outputLanguage == AiCommitSettings.LANGUAGE_EN) {
            outputLanguageEn.isSelected = true
        } else {
            outputLanguageZh.isSelected = true
        }
    }

    private fun selectedLanguage(): String =
        if (outputLanguageEn.isSelected) AiCommitSettings.LANGUAGE_EN else AiCommitSettings.LANGUAGE_ZH

    private fun parseMaxDiffLength(): Int {
        val value = maxDiffLengthField.text.trim().toIntOrNull()
        if (value == null || value < AiCommitSettings.MIN_MAX_DIFF_LENGTH) {
            throw ConfigurationException("Diff 最大长度必须是不小于 ${AiCommitSettings.MIN_MAX_DIFF_LENGTH} 的整数")
        }
        return value
    }
}
