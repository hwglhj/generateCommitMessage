package com.fs.aicommit

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.CommitMessageI
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil

/**
 * 提交面板中的 "AI 生成 Commit Message" 按钮。
 * 实现链路与 CC GUI 的 GenerateCommitMessageAction 一致：
 * 获取提交面板(CommitMessageI) -> 获取用户勾选的变更 -> 后台生成 -> 回填提交信息框。
 *
 * 体验优化：
 * - 使用可取消的后台任务，右下角进度条分阶段显示进度与已用时，可随时取消（会终止 CLI 进程）
 * - 生成前保存输入框原有内容，失败/取消时原样恢复，不清空用户已写的文字
 */
class GenerateCommitMessageAction : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val commitPanel = getCommitMessagePanel(e)
        if (commitPanel == null) {
            notify(project, "无法访问提交信息输入框", NotificationType.WARNING)
            return
        }
        val selection = getUserSelectedChanges(e, project)
        if (selection.isEmpty()) {
            notify(project, "没有选中任何变更", NotificationType.WARNING)
            return
        }

        // 保存原有内容，失败/取消时恢复
        val originalMessage = readCommitMessage(e, commitPanel) ?: ""
        updateCommitMessage(project, commitPanel, "正在生成 commit message...")

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "AI 生成 commit message", true) {
            var result: String? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                result = AiCommitService(project).generate(selection.changes, selection.unversionedFiles, indicator)
            }

            override fun onSuccess() {
                val message = result ?: return
                updateCommitMessage(project, commitPanel, message)
            }

            override fun onCancel() {
                updateCommitMessage(project, commitPanel, originalMessage)
                notify(project, "已取消生成", NotificationType.INFORMATION)
            }

            override fun onThrowable(error: Throwable) {
                LOG.warn("Failed to generate commit message", error)
                updateCommitMessage(project, commitPanel, originalMessage)
                notify(project, "生成失败: ${error.message ?: error.javaClass.simpleName}", NotificationType.ERROR)
            }
        })
    }

    /**
     * 写入提交信息输入框，并同步到当前变更列表的注释。
     * 非模态提交面板的输入框内容与 changelist 注释双向同步：若只改输入框，
     * 生成期间的 changelist 刷新（如 Gradle sync、索引）会把旧注释写回输入框，
     * 导致"正在生成..."占位文本在生成完成后又覆盖掉结果。
     */
    private fun updateCommitMessage(project: Project, panel: CommitMessageI, text: String) {
        panel.setCommitMessage(text)
        try {
            val clm = ChangeListManager.getInstance(project)
            clm.editComment(clm.defaultChangeList.name, text)
        } catch (ex: Exception) {
            LOG.debug("Failed to sync changelist comment: ${ex.message}")
        }
    }

    private fun getCommitMessagePanel(e: AnActionEvent): CommitMessageI? {
        val workflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER)
        if (workflowHandler is CommitMessageI) return workflowHandler
        return e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
    }

    /** 读取提交输入框当前内容（CommitMessageI 只有 set 方法，读取需按具体实现分别处理） */
    private fun readCommitMessage(e: AnActionEvent, panel: CommitMessageI): String? {
        when (panel) {
            is CheckinProjectPanel -> return panel.commitMessage
            is CommitMessage -> return panel.comment
        }
        // 新版提交面板：workflowHandler.getUi().getCommitMessageUi().getText()
        val workflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER) ?: return null
        return try {
            val ui = workflowHandler.javaClass.getMethod("getUi").invoke(workflowHandler) ?: return null
            val messageUi = ui.javaClass.getMethod("getCommitMessageUi").invoke(ui) ?: return null
            messageUi.javaClass.getMethod("getText").invoke(messageUi) as? String
        } catch (ex: Exception) {
            LOG.debug("Failed to read commit message via reflection: ${ex.message}")
            null
        }
    }

    /** 用户在提交面板勾选的内容：已跟踪文件的变更 + 未版本化（untracked）文件 */
    data class SelectedChanges(
        val changes: List<Change>,
        val unversionedFiles: List<FilePath>,
    ) {
        fun isEmpty(): Boolean = changes.isEmpty() && unversionedFiles.isEmpty()
    }

    private fun getUserSelectedChanges(e: AnActionEvent, project: Project): SelectedChanges {
        // 1. 新版提交面板：反射获取勾选的变更与勾选的未版本化文件
        val workflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER)
        if (workflowHandler != null) {
            val changes = getIncludedViaReflection(workflowHandler, "getIncludedChanges")
                ?.filterIsInstance<Change>().orEmpty()
            val unversioned = getIncludedViaReflection(workflowHandler, "getIncludedUnversionedFiles")
                ?.mapNotNull { toFilePath(it) }.orEmpty()
            if (changes.isNotEmpty() || unversioned.isNotEmpty()) return SelectedChanges(changes, unversioned)
        }

        // 2. 旧版提交对话框
        val messageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
        if (messageControl is CheckinProjectPanel) {
            val changes = messageControl.selectedChanges
            if (!changes.isNullOrEmpty()) return SelectedChanges(changes.toList(), emptyList())
        }

        // 3. DataContext 中的变更
        val changesArray = e.getData(VcsDataKeys.CHANGES)
        if (!changesArray.isNullOrEmpty()) return SelectedChanges(changesArray.toList(), emptyList())

        // 4. 兜底：所有本地变更 + 所有未版本化文件
        val clm = ChangeListManager.getInstance(project)
        return SelectedChanges(clm.allChanges.toList(), clm.unversionedFilesPaths.toList())
    }

    /** 反射调用 workflowHandler.getUi() 上的取值方法（getIncludedChanges / getIncludedUnversionedFiles） */
    private fun getIncludedViaReflection(workflowHandler: Any, methodName: String): Collection<*>? {
        return try {
            val ui = workflowHandler.javaClass.getMethod("getUi").invoke(workflowHandler) ?: return null
            ui.javaClass.getMethod(methodName).invoke(ui) as? Collection<*>
        } catch (e: Exception) {
            LOG.debug("Failed to call $methodName via reflection: ${e.message}")
            null
        }
    }

    /** getIncludedUnversionedFiles 的元素类型随 IDEA 版本不同：新版是 FilePath，旧版是 VirtualFile */
    private fun toFilePath(item: Any?): FilePath? = when (item) {
        is FilePath -> item
        is VirtualFile -> VcsUtil.getFilePath(item)
        else -> null
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "Generate Commit Message"
        e.presentation.description = "使用 AI 根据选中的变更生成 commit message"
        e.presentation.isEnabledAndVisible = e.project != null
    }

    private fun notify(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("RestfulToolkit.AiCommit")
            .createNotification(content, type)
            .notify(project)
    }

    companion object {
        private val LOG = Logger.getInstance(GenerateCommitMessageAction::class.java)
    }
}
