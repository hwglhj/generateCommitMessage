package com.fs.aicommit

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

/**
 * 行内 AI 修改（Cmd+K 风格）设置面板：
 * CLI 与模型独立配置，不与 AI Commit 共用（[AiCommitConfigurable]）。
 */
class InlineEditConfigurable : Configurable {

    private val settings = AiCommitSettings.getInstance()

    private var mainPanel: JPanel? = null
    private val inlinePanel = ProviderModelPanel("CLI 与模型")

    override fun getDisplayName(): String = "AI 行内修改设置"

    override fun createComponent(): JComponent {
        if (mainPanel != null) return mainPanel!!

        val content = FormBuilder.createFormBuilder()
            .addComponent(inlinePanel.panel)
            .addVerticalGap(10)
            .addComponent(
                JBLabel(
                    "<html><small>作用于「AI 行内修改」与「AI 生成注释」；首次使用请先完成 CLI 认证。</small></html>"
                )
            )
            .panel

        mainPanel = JPanel(BorderLayout()).apply {
            border = EmptyBorder(JBUI.insets(10))
            add(content, BorderLayout.NORTH)
        }

        reset()
        return mainPanel!!
    }

    /** 尚未单独配置时（inlineProvider 为空），面板显示跟随 AI Commit 的当前值 */
    private fun providerForUi(): String =
        settings.inlineProvider.ifBlank { settings.activeProvider }

    private fun modelForUi(): String =
        if (settings.inlineProvider.isBlank()) settings.model else settings.inlineModel

    override fun isModified(): Boolean =
        inlinePanel.isModified(providerForUi(), modelForUi())

    override fun apply() {
        settings.inlineProvider = inlinePanel.selectedProvider()
        settings.inlineModel = inlinePanel.composedModel()
    }

    override fun reset() {
        inlinePanel.reset(providerForUi(), modelForUi())
    }

    override fun disposeUIResources() {
        mainPanel = null
    }
}
