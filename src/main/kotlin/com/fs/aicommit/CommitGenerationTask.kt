package com.fs.aicommit

import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

internal class GenerationGate {
    @Volatile var isRunning: Boolean = false
        private set
    private val lock = Any()
    private var generation = 0L

    fun acquire(): Token? = synchronized(lock) {
        if (isRunning) return null
        isRunning = true
        Token(++generation)
    }

    private fun release(token: Token) = synchronized(lock) {
        if (isRunning && token.generation == generation) isRunning = false
    }

    inner class Token internal constructor(internal val generation: Long) : AutoCloseable {
        private val closed = AtomicBoolean(false)
        override fun close() {
            if (closed.compareAndSet(false, true)) release(this)
        }
    }
}

/** 同一次生成过程持有的模型选择面板。 */
internal interface CommitModelSwitcher {
    val isOpen: Boolean
    fun setGenerating(generating: Boolean)
    fun close()
}

/** 一次提交生成及用户选模型后的重试，共用原始输入和结果回调。 */
internal class CommitGenerationRequest(
    private val project: Project?,
    private val settings: AiCommitSettings,
    private val generate: (AiCommitSettings, ProgressIndicator) -> String,
    private val generated: (String) -> Unit,
    private val failed: (Throwable) -> Unit,
    private val canceled: () -> Unit,
    private val showModelSwitcher: (() -> Unit) -> CommitModelSwitcher?,
    private val canUpdateUi: () -> Boolean,
    private val runTask: (CommitGenerationTask) -> Unit = { ProgressManager.getInstance().run(it) },
    private var modelSwitcher: CommitModelSwitcher? = null,
) {
    fun start() {
        if (!canUpdateUi() || modelSwitcher?.isOpen == false) return
        val token = CommitGenerationTask.gate.acquire() ?: return
        try {
            modelSwitcher?.setGenerating(true)
            // 选中模型后才创建新的快照，避免重用上一次失败请求的配置。
            val snapshot = settings.generationSnapshot()
            runTask(CommitGenerationTask(
                project, token,
                generate = { indicator -> generate(snapshot, indicator) },
                generated = { message ->
                    modelSwitcher?.close()
                    generated(message)
                },
                failed = failed,
                canceled = {
                    modelSwitcher?.setGenerating(false)
                    canceled()
                },
                showModelSwitcher = {
                    if (modelSwitcher == null) modelSwitcher = showModelSwitcher(::start)
                    else modelSwitcher?.setGenerating(false)
                },
                canUpdateUi = canUpdateUi,
            ))
        } catch (error: Throwable) {
            token.close()
            modelSwitcher?.setGenerating(false)
            throw error
        }
    }
}

internal class CommitGenerationTask(
    project: Project?,
    private val token: GenerationGate.Token,
    private val generate: (ProgressIndicator) -> String,
    private val generated: (String) -> Unit,
    private val failed: (Throwable) -> Unit,
    private val canceled: () -> Unit,
    private val showModelSwitcher: () -> Unit,
    private val canUpdateUi: () -> Boolean,
) : Task.Backgroundable(project, tr("commit.generating"), true) {
    private var result: String? = null

    init { setCancelText(tr("dialog.button.cancel")) }

    override fun run(indicator: ProgressIndicator) {
        result = generate(indicator)
    }

    override fun onSuccess() {
        token.close()
        if (canUpdateUi()) result?.let(generated)
    }

    override fun onCancel() {
        token.close()
        if (canUpdateUi()) canceled()
    }

    override fun onThrowable(error: Throwable) {
        token.close()
        if (error is ProcessCanceledException || error is CancellationException) {
            if (canUpdateUi()) canceled()
            return
        }
        if (canUpdateUi()) {
            failed(error)
            showModelSwitcher()
        }
    }

    override fun onFinished() {
        token.close()
    }

    companion object {
        // 模型设置是应用级的；工具栏切换与生成共用一个运行状态。
        internal val gate = GenerationGate()
    }
}
