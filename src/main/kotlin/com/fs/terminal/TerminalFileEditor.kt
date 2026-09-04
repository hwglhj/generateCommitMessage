package com.fs.terminal

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileTypes.FileType
import javax.swing.Icon
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import java.beans.PropertyChangeListener
import javax.swing.JComponent

/**
 * 一个"伪文件":在编辑器区域中央以标签页形式打开的 CLI 终端。
 */
class TerminalVirtualFile(val tool: CliTool, name: String = tool.id) :
    LightVirtualFile(name, TerminalFileType(tool), "") {
    init {
        isWritable = false
    }
}

/** 仅用于让标签页显示对应 CLI 工具的图标 */
class TerminalFileType(private val tool: CliTool) : FileType {
    override fun getName(): String = "FS Terminal ${tool.id}"
    override fun getDescription(): String = "${tool.id} terminal tab"
    override fun getDefaultExtension(): String = ""
    override fun getIcon(): Icon = tool.icon
    override fun isBinary(): Boolean = true
    override fun isReadOnly(): Boolean = true
}

/**
 * 匹配 [TerminalVirtualFile],在编辑器标签页里内嵌 IDEA 自带终端 (JediTerm),
 * 并按配置自动执行对应 CLI 命令(可选高权限模式)。
 */
class TerminalFileEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean = file is TerminalVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        TerminalFileEditor(project, file as TerminalVirtualFile)

    override fun getEditorTypeId(): String = "fs-terminal-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

class TerminalFileEditor(project: Project, private val file: TerminalVirtualFile) :
    UserDataHolderBase(), FileEditor {

    private val widget = LocalTerminalDirectRunner(project)
        .createTerminalWidget(this, project.basePath, true)
        .also {
            val cmd = file.tool.command(CliToolsSettings.getInstance().isHighPermission(file.tool))
            (it as? ShellTerminalWidget)?.executeCommand(cmd)
        }

    override fun getComponent(): JComponent = widget.component

    override fun getPreferredFocusedComponent(): JComponent = widget.terminalPanel

    override fun getName(): String = file.name

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getFile(): VirtualFile = file

    override fun dispose() {
        Disposer.dispose(widget)
    }
}
