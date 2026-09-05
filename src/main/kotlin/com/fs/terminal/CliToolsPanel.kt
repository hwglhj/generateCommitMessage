package com.fs.terminal

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

/**
 * CLI Tools 配置面板：侧边栏图标显示 + 高权限启动开关。
 * 作为统一设置页 [com.fs.aicommit.GenerateCommitMessageConfigurable] 的一个标签页使用，
 * 数据存储在 [CliToolsSettings]。
 */
class CliToolsPanel {

    val panel: JPanel
    private val showIconBoxes = linkedMapOf<CliTool, JBCheckBox>()
    private val highPermissionBoxes = linkedMapOf<CliTool, JBCheckBox>()
    private val settings = CliToolsSettings.getInstance()

    init {
        panel = createPanel()
    }

    private fun createPanel(): JPanel {
        val inner = JPanel()
        inner.layout = BoxLayout(inner, BoxLayout.Y_AXIS)

        inner.add(JBLabel("侧边栏图标显示:"))
        for (tool in CliTool.values()) {
            val cb = JBCheckBox("显示 ${tool.id} 图标", settings.isShowIcon(tool))
            showIconBoxes[tool] = cb
            inner.add(cb)
        }

        inner.add(Box.createVerticalStrut(JBUI.scale(10)))
        inner.add(JBLabel("高权限启动:"))
        for (tool in CliTool.values()) {
            val cb = JBCheckBox(tool.highPermissionDesc, settings.isHighPermission(tool))
            highPermissionBoxes[tool] = cb
            inner.add(cb)
        }

        return JPanel(BorderLayout()).apply {
            border = EmptyBorder(JBUI.insets(10))
            add(inner, BorderLayout.NORTH)
        }
    }

    fun isModified(): Boolean {
        return showIconBoxes.any { (tool, cb) -> cb.isSelected != settings.isShowIcon(tool) } ||
            highPermissionBoxes.any { (tool, cb) -> cb.isSelected != settings.isHighPermission(tool) }
    }

    fun apply() {
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

    fun reset() {
        showIconBoxes.forEach { (tool, cb) -> cb.isSelected = settings.isShowIcon(tool) }
        highPermissionBoxes.forEach { (tool, cb) -> cb.isSelected = settings.isHighPermission(tool) }
    }
}
