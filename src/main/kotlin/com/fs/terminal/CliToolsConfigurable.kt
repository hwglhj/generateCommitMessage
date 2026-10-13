package com.fs.terminal

import com.intellij.openapi.options.Configurable
import com.fs.i18n.SubMsgBundle.message as tr
import javax.swing.JComponent

/**
 * 设置页:CLI Tools 图标显示与高权限开关。
 */
class CliToolsConfigurable : Configurable {

    private var panel: CliToolsPanel? = null

    override fun getDisplayName(): String = tr("settings.cli.tools")

    override fun createComponent(): JComponent = (panel ?: CliToolsPanel().also { panel = it }).panel

    override fun isModified(): Boolean = panel?.isModified() == true

    override fun apply() { panel?.apply() }

    override fun reset() { panel?.reset() }

    override fun disposeUIResources() {
        panel = null
    }
}
