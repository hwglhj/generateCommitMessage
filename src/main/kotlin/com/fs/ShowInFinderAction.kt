package com.fs

import com.fs.aicommit.AiCommitSettings
import com.fs.aicommit.CliProcess
import com.fs.aicommit.ExternalEditor
import com.fs.i18n.SubMsgBundle
import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import java.io.File

class ShowInFinderAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val manager = FileEditorManager.getInstance(project)
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: manager.selectedFiles.firstOrNull() ?: return
        val file = File(currentFile.path).takeIf { it.isFile } ?: return
        FileDocumentManager.getInstance().getDocument(currentFile)?.let { FileDocumentManager.getInstance().saveDocument(it) }
        val editor = e.getData(CommonDataKeys.EDITOR) ?: manager.selectedTextEditor
        val position = editor?.caretModel?.logicalPosition
        val selected = AiCommitSettings.getInstance().externalEditor
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val executable = ExternalEditor.find(selected) ?: throw IllegalStateException(tr("editor.notfound", ExternalEditor.name(selected)))
                CliProcess.builder(ExternalEditor.command(executable, file.absolutePath, (position?.line ?: 0) + 1, (position?.column ?: 0) + 1))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start().outputStream.close()
            } catch (error: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    if (!project.isDisposed) Messages.showDialog(project, tr("editor.failed", error.message.orEmpty()), tr("message.error"),
                        arrayOf(tr("dialog.button.ok")), 0, Messages.getErrorIcon())
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val settings = AiCommitSettings.getInstance()
        SubMsgBundle.language = settings.interfaceLanguage
        e.presentation.text = tr("editor.open", ExternalEditor.name(settings.externalEditor))
        e.presentation.description = tr("editor.description")
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.VIRTUAL_FILE)?.isInLocalFileSystem == true
    }
}
