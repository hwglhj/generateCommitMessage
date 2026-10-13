package com.fs.aicommit

import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.openapi.progress.ProgressIndicator
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
