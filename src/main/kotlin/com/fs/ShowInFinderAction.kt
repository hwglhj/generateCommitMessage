package com.fs

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.diagnostic.Logger
import com.fs.aicommit.AiCommitSettings
import java.io.File
import java.io.IOException

class ShowInFinderAction : AnAction() {
    private val logger = Logger.getInstance(ShowInFinderAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        ApplicationManager.getApplication().runReadAction {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            
            if (currentFile != null) {
                try {
                    val physicalFile = File(currentFile.path)
                    if (physicalFile.exists()) {
                        // 获取当前光标位置
                        val currentLine = editor?.caretModel?.logicalPosition?.line?.plus(1) ?: 1
                        val currentColumn = editor?.caretModel?.logicalPosition?.column?.plus(1) ?: 1

                        // 默认使用 VS Code 作为外部编辑器
                        val editor = "code"
                        val editorName = "VS Code"

                        // 根据操作系统和编辑器获取路径
                        val osName = System.getProperty("os.name").toLowerCase()
                        val editorPath = when {
                            osName.contains("mac") -> when (editor) {
                                "cursor" -> "/Applications/Cursor.app/Contents/MacOS/Cursor"
                                "antigravity" -> "/Applications/Antigravity.app/Contents/Resources/app/bin/antigravity"
                                else -> "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code"
                            }
                            osName.contains("windows") -> {
                                val programFiles = System.getenv("ProgramFiles")
                                when (editor) {
                                    "cursor" -> "$programFiles\\Cursor\\Cursor.exe"
                                    "antigravity" -> "$programFiles\\Antigravity\\Antigravity.exe"
                                    else -> "$programFiles\\Microsoft VS Code\\Code.exe"
                                }
                            }
                            else -> when (editor) {
                                "cursor" -> "/usr/bin/cursor"
                                "antigravity" -> "/usr/bin/antigravity"
                                else -> "/usr/bin/code"
                            }
                        }

                        logger.info("使用的${editorName}路径: $editorPath")

                        if (File(editorPath).exists()) {
                            // 构建命令数组
                            val openCommand = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                                arrayOf(
                                    "cmd",
                                    "/c",
                                    "start",
                                    editorPath,
                                    "--goto",
                                    "${physicalFile.absolutePath}:${currentLine}:${currentColumn}"
                                )
                            } else {
                                arrayOf(
                                    editorPath,
                                    "--goto",
                                    "${physicalFile.absolutePath}:${currentLine}:${currentColumn}"
                                )
                            }
                            
                            // 打印完整命令
                            val commandStr = openCommand.joinToString(" ")
                            logger.info("执行命令: $commandStr")
                            println("执行命令: $commandStr")
                            
                            // 打印文件和位置信息
                            logger.info("文件路径: ${physicalFile.absolutePath}")
                            logger.info("跳转位置: 行 $currentLine, 列 $currentColumn")
                            println("文件路径: ${physicalFile.absolutePath}")
                            println("跳转位置: 行 $currentLine, 列 $currentColumn")
                            
                            // 执行命令
                            Runtime.getRuntime().exec(openCommand)
                        } else {
                            val errorMessage = when {
                                System.getProperty("os.name").toLowerCase().contains("mac") ->
                                    "未找到${editorName}，请确保已安装或在设置中切换编辑器"
                                System.getProperty("os.name").toLowerCase().contains("windows") ->
                                    "未找到${editorName}，请确保已安装或在设置中切换编辑器"
                                else ->
                                    "未找到${editorName}，请确保已正确安装或在设置中切换编辑器"
                            }
                            logger.warn("错误: $errorMessage")
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showErrorDialog(
                                    project,
                                    errorMessage,
                                    "错误"
                                )
                            }
                        }
                    }
                } catch (e: IOException) {
                    val errorMessage = "打开文件失败: ${e.message}"
                    logger.error(errorMessage, e)
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            errorMessage,
                            "错误"
                        )
                    }
                }
            }
        }
    }
} 