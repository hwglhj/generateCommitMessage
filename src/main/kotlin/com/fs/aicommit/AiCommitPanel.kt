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
import com.fs.i18n.SubMsgBundle.message as tr
import java.awt.CardLayout
import javax.swing.JComboBox

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
    private val cliPathField = JBTextField(32)
    private val providerModelPanel = ProviderModelPanel(tr("aicommit.cli.title")) { cliPathField.text.trim() }
    private val modeCombo = JComboBox(arrayOf(tr("aicommit.mode.cli"), tr("aicommit.mode.api")))
    private val modeLayout = CardLayout()
    private val modeCards = JPanel(modeLayout)
    private val apiPanel = ApiModePanel()

    // 附加提示词
    private val promptArea = JBTextArea(6, 60)

    // 增强选项
    private val includeRecentCommitsCheckBox = JBCheckBox(
        tr("aicommit.recent", AiCommitSettings.RECENT_COMMITS_COUNT),
        true
    )
    private val maxDiffLengthField = JBTextField(AiCommitSettings.DEFAULT_MAX_DIFF_LENGTH.toString(), 10)
    private val outputLanguageZh = JRadioButton(tr("language.chinese"), true)
    private val outputLanguageEn = JRadioButton(tr("language.english"), false)

    init {
        ButtonGroup().apply {
            add(outputLanguageZh)
            add(outputLanguageEn)
        }
        modeCombo.addActionListener { modeLayout.show(modeCards, selectedMode()) }
        panel = createPanel()
    }

    private fun createPanel(): JPanel {
        // 1. 附加提示词区域
        promptArea.lineWrap = true
        promptArea.wrapStyleWord = true

        val viewBuiltinButton = JButton(tr("aicommit.prompt.view")).apply {
            addActionListener { showBuiltinPrompt() }
        }
        val clearPromptButton = JButton(tr("aicommit.prompt.clear")).apply {
            addActionListener { promptArea.text = "" }
        }
        val promptButtons = JPanel(BorderLayout(10, 0)).apply {
            add(viewBuiltinButton, BorderLayout.WEST)
            add(clearPromptButton, BorderLayout.EAST)
        }
        val promptPanel = JPanel(BorderLayout(0, 6)).apply {
            add(
                FormBuilder.createFormBuilder()
                    .addComponent(JBLabel(tr("aicommit.prompt.label")))
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
            add(JBLabel(tr("aicommit.diff.hint")))
        }
        val enhancedOptionsPanel = FormBuilder.createFormBuilder()
            .addComponent(includeRecentCommitsCheckBox)
            .addVerticalGap(6)
            .addLabeledComponent(JBLabel(tr("aicommit.diff.limit")), maxDiffPanel)
            .addVerticalGap(6)
            .addLabeledComponent(JBLabel(tr("aicommit.output.language")), languagePanel)
            .panel
        enhancedOptionsPanel.border = TitledBorder(tr("aicommit.enhanced"))

        val cliPanel = FormBuilder.createFormBuilder()
            .addComponent(providerModelPanel.panel)
            .addLabeledComponent(tr("aicommit.cli.path"), cliPathField)
            .panel
        cliPathField.toolTipText = tr("aicommit.cli.path.hint")
        modeCards.add(cliPanel, AiCommitSettings.MODE_CLI)
        modeCards.add(apiPanel.panel, AiCommitSettings.MODE_API)
        val modeRow = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            modeCombo.preferredSize = Dimension(200, modeCombo.preferredSize.height)
            add(modeCombo)
        }

        // 3. 组装
        val content = FormBuilder.createFormBuilder()
            .addLabeledComponent(tr("aicommit.mode"), modeRow)
            .addVerticalGap(10)
            .addComponent(modeCards)
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
                title = tr("aicommit.prompt.builtin.title")
                setOKButtonText(tr("dialog.button.ok"))
                setCancelButtonText(tr("dialog.button.cancel"))
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
        if (selectedMode() != settings.mode || cliPathField.text.trim() != settings.cliPath) return true
        if (apiPanel.isModified(settings)) return true
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
        if (selectedMode() == AiCommitSettings.MODE_API && !apiPanel.hasActiveConfig()) {
            throw ConfigurationException(tr("api.select.config"))
        }
        apiPanel.apply(settings)
        settings.mode = selectedMode()
        settings.cliPath = cliPathField.text.trim()
        settings.activeProvider = providerModelPanel.selectedProvider()
        settings.model = providerModelPanel.composedModel()
        settings.customPrompt = promptArea.text
        settings.includeRecentCommits = includeRecentCommitsCheckBox.isSelected
        settings.maxDiffLength = maxDiffLength
        settings.outputLanguage = selectedLanguage()
    }

    fun reset() {
        modeCombo.selectedIndex = if (settings.mode == AiCommitSettings.MODE_API) 1 else 0
        modeLayout.show(modeCards, selectedMode())
        cliPathField.text = settings.cliPath
        apiPanel.reset(settings)
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

    fun dispose() { providerModelPanel.dispose() }

    private fun selectedLanguage(): String =
        if (outputLanguageEn.isSelected) AiCommitSettings.LANGUAGE_EN else AiCommitSettings.LANGUAGE_ZH

    private fun selectedMode(): String = if (modeCombo.selectedIndex == 1) AiCommitSettings.MODE_API else AiCommitSettings.MODE_CLI

    private fun parseMaxDiffLength(): Int {
        val value = maxDiffLengthField.text.trim().toIntOrNull()
        if (value == null || value < AiCommitSettings.MIN_MAX_DIFF_LENGTH) {
            throw ConfigurationException(tr("aicommit.diff.invalid", AiCommitSettings.MIN_MAX_DIFF_LENGTH))
        }
        return value
    }
}
