package com.fs.terminal

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 设置页:CLI Tools 图标显示与高权限开关。
 */
class CliToolsConfigurable : Configurable {

    private val showIconBoxes = linkedMapOf<CliTool, JBCheckBox>()
    private val highPermissionBoxes = linkedMapOf<CliTool, JBCheckBox>()

    override fun getDisplayName(): String = "CLI Tools 设置"

    override fun createComponent(): JComponent {
        val inner = JPanel()
        inner.layout = BoxLayout(inner, BoxLayout.Y_AXIS)
        val settings = CliToolsSettings.getInstance()

        inner.add(JBLabel("侧边栏图标显示:"))
        for (tool in CliTool.values()) {
            val cb = JBCheckBox("显示 ${tool.id} 图标", settings.isShowIcon(tool))
            showIconBoxes[tool] = cb
            inner.add(cb)
        }

        inner.add(JBLabel("高权限启动:"))
        for (tool in CliTool.values()) {
            val cb = JBCheckBox(tool.highPermissionDesc, settings.isHighPermission(tool))
            highPermissionBoxes[tool] = cb
            inner.add(cb)
        }

        val panel = JPanel(BorderLayout())
        panel.add(inner, BorderLayout.NORTH)
        return panel
    }

    override fun isModified(): Boolean {
        val settings = CliToolsSettings.getInstance()
        return showIconBoxes.any { (tool, cb) -> cb.isSelected != settings.isShowIcon(tool) } ||
            highPermissionBoxes.any { (tool, cb) -> cb.isSelected != settings.isHighPermission(tool) }
    }

    override fun apply() {
        val settings = CliToolsSettings.getInstance()
        showIconBoxes.forEach { (tool, cb) -> settings.setShowIcon(tool, cb.isSelected) }
        highPermissionBoxes.forEach { (tool, cb) -> settings.setHighPermission(tool, cb.isSelected) }
        // 图标显隐即时应用到所有已打开项目
        for (project in ProjectManager.getInstance().openProjects) {
            val manager = ToolWindowManager.getInstance(project)
            for (tool in CliTool.values()) {
                manager.getToolWindow(tool.id)?.setAvailable(settings.isShowIcon(tool), null)
            }
        }
    }

    override fun reset() {
        val settings = CliToolsSettings.getInstance()
        showIconBoxes.forEach { (tool, cb) -> cb.isSelected = settings.isShowIcon(tool) }
        highPermissionBoxes.forEach { (tool, cb) -> cb.isSelected = settings.isHighPermission(tool) }
    }

    override fun disposeUIResources() {
        showIconBoxes.clear()
        highPermissionBoxes.clear()
    }
}
