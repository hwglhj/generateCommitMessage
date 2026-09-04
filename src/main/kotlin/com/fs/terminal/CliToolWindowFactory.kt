package com.fs.terminal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowManagerListener

/**
 * 右侧边栏 CLI 工具入口基类:点击条纹图标后,像打开文件一样
 * 在编辑器区域中央打开对应工具的终端标签页并自动执行命令。
 */
abstract class CliToolWindowFactoryBase(private val tool: CliTool) : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 内容为空,该工具窗口只作为一个"按钮"使用
    }

    override fun shouldBeAvailable(project: Project): Boolean =
        CliToolsSettings.getInstance().isShowIcon(tool)

    override fun init(toolWindow: ToolWindow) {
        val project = toolWindow.project
        project.messageBus.connect(toolWindow.disposable).subscribe(
            ToolWindowManagerListener.TOPIC,
            object : ToolWindowManagerListener {
                override fun toolWindowShown(shown: ToolWindow) {
                    if (shown.id == tool.id) {
                        ApplicationManager.getApplication().invokeLater {
                            shown.hide(null)
                            openTerminalInEditor(project)
                        }
                    }
                }
            }
        )
    }

    private fun openTerminalInEditor(project: Project) {
        val manager = FileEditorManager.getInstance(project)
        // 每次点击都新开一个终端标签页(同名标签用序号区分)
        val count = manager.openFiles.count { (it as? TerminalVirtualFile)?.tool == tool }
        val name = if (count == 0) tool.id else "${tool.id} (${count + 1})"
        manager.openFile(TerminalVirtualFile(tool, name), true)
    }
}

class ClaudeToolWindowFactory : CliToolWindowFactoryBase(CliTool.CLAUDE)
class CodexToolWindowFactory : CliToolWindowFactoryBase(CliTool.CODEX)
class CursorToolWindowFactory : CliToolWindowFactoryBase(CliTool.CURSOR)
class OpenCodeToolWindowFactory : CliToolWindowFactoryBase(CliTool.OPENCODE)
class AntigravityToolWindowFactory : CliToolWindowFactoryBase(CliTool.ANTIGRAVITY)
class GrokToolWindowFactory : CliToolWindowFactoryBase(CliTool.GROK)
class OmpToolWindowFactory : CliToolWindowFactoryBase(CliTool.OMP)
