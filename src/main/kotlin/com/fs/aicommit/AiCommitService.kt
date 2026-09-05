package com.fs.aicommit

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangesUtil
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * AI 生成 Commit Message 服务。
 * 链路与 CC GUI 的 GitCommitMessageService 一致：
 * 变更列表 -> 生成 diff 摘要 -> 组装提示词 -> 调用 SDK -> 清洗输出。
 *
 * SDK 调用使用所选 CLI 的非交互模式，认证复用 CLI 自身的登录凭证，
 * 无需在插件中配置 API Key。
 */
class AiCommitService(private val project: Project?) {

    private val settings = AiCommitSettings.getInstance()

    /**
     * 同步生成 commit message（在后台任务中调用）。
     * 通过 [indicator] 分阶段汇报进度并支持取消（取消会立刻终止 CLI 进程）。
     * 失败抛异常，取消抛 [ProcessCanceledException]。
     */
    fun generate(
        changes: Collection<Change>,
        unversionedFiles: Collection<FilePath> = emptyList(),
        indicator: ProgressIndicator? = null,
    ): String {
        val sdk = settings.activeSdk()
        val model = settings.modelFor(sdk)

        indicator?.text = "检测 ${sdk.displayName}..."
        val status = SdkManager.detect(sdk, null)
        if (!status.installed || status.path == null) {
            throw RuntimeException("未检测到 ${sdk.displayName}（${sdk.binName} 命令），请先安装并登录")
        }
        indicator?.checkCanceled()

        indicator?.text = "收集变更 diff（${changes.size + unversionedFiles.size} 个文件）..."
        val diff = generateDiff(changes, unversionedFiles)
        if (diff.isBlank()) {
            throw RuntimeException("未找到可用的代码变更")
        }
        indicator?.checkCanceled()

        val recentCommits = if (settings.includeRecentCommits) {
            indicator?.text = "读取近期提交（风格参考）..."
            collectRecentCommits()
        } else {
            ""
        }
        val prompt = AiCommitPrompts.buildFullPrompt(diff, recentCommits, settings.customPrompt, settings.outputLanguage)
        indicator?.checkCanceled()

        indicator?.text = "调用 ${sdk.displayName}（$model）生成中..."
        val raw = runSdk(sdk, status.path, prompt, model, indicator)

        indicator?.text = "清洗输出..."
        val message = cleanupCommitMessage(raw)
        if (message.isBlank()) {
            throw RuntimeException("AI 返回了空的 commit message")
        }
        return message
    }

    // ---------------------------------------------------------------
    // diff 构建（与 CC GUI 的 generateGitDiff 逻辑一致）
    // ---------------------------------------------------------------

    fun generateDiff(changes: Collection<Change>, unversionedFiles: Collection<FilePath> = emptyList()): String {
        val maxDiffLength = settings.maxDiffLength.coerceAtLeast(AiCommitSettings.MIN_MAX_DIFF_LENGTH)
        val diff = StringBuilder()
        for (change in changes) {
            try {
                val filePath = ChangesUtil.getFilePath(change)
                val type = change.type
                diff.append("\n=== ").append(type.name).append(": ").append(filePath.path).append(" ===\n")
                val before = change.beforeRevision
                val after = change.afterRevision
                when {
                    type == Change.Type.NEW && after != null -> {
                        appendNewFileContent(diff, after.content)
                    }
                    type == Change.Type.DELETED && before != null -> {
                        diff.append("--- 文件已删除\n")
                    }
                    type == Change.Type.MODIFICATION && before != null && after != null -> {
                        val b = before.content
                        val a = after.content
                        if (b != null && a != null) {
                            diff.append(generateSimpleDiff(b, a))
                        }
                    }
                }
                if (diff.length > maxDiffLength) {
                    diff.append("\n... (后续变更因长度限制未包含；请仅根据以上内容直接生成提交信息)")
                    return diff.toString()
                }
            } catch (e: Exception) {
                LOG.warn("Failed to get diff for change: ${e.message}")
            }
        }
        // 未版本化（untracked）文件：git 里没有 before 版本，按新增文件处理，内容直接读磁盘
        for (filePath in unversionedFiles) {
            try {
                diff.append("\n=== NEW(未跟踪): ").append(filePath.path).append(" ===\n")
                appendNewFileContent(diff, readUnversionedContent(filePath))
                if (diff.length > maxDiffLength) {
                    diff.append("\n... (后续变更因长度限制未包含；请仅根据以上内容直接生成提交信息)")
                    return diff.toString()
                }
            } catch (e: Exception) {
                LOG.warn("Failed to read unversioned file ${filePath.path}: ${e.message}")
            }
        }
        return diff.toString()
    }

    private fun appendNewFileContent(diff: StringBuilder, content: String?) {
        if (content == null) return
        if (content.length <= 500) {
            diff.append("+++ ").append(content).append("\n")
        } else {
            diff.append("+++ [文件过大，仅显示前500字符]\n")
            diff.append(content, 0, 500).append("\n")
        }
    }

    private fun readUnversionedContent(filePath: FilePath): String? {
        val file = filePath.ioFile
        if (!file.isFile || !file.canRead()) return null
        // 只取前 4KB 判断/展示，避免大文件与二进制文件拖慢生成
        val bytes = file.inputStream().use { it.readNBytes(4096) }
        if (bytes.any { it == 0.toByte() }) return "[二进制文件]"
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun generateSimpleDiff(before: String, after: String): String {
        val beforeLines = before.split("\n")
        val afterLines = after.split("\n")
        val diff = StringBuilder()
        val maxLines = maxOf(beforeLines.size, afterLines.size)
        var shownLines = 0
        val maxShownLines = 30
        var i = 0
        while (i < maxLines && shownLines < maxShownLines) {
            val beforeLine = beforeLines.getOrElse(i) { "" }
            val afterLine = afterLines.getOrElse(i) { "" }
            if (beforeLine != afterLine) {
                if (beforeLine.isNotEmpty()) {
                    diff.append("- ").append(beforeLine).append("\n")
                    shownLines++
                }
                if (afterLine.isNotEmpty() && shownLines < maxShownLines) {
                    diff.append("+ ").append(afterLine).append("\n")
                    shownLines++
                }
            }
            i++
        }
        if (maxLines > maxShownLines) {
            diff.append("... (更多变更已省略)\n")
        }
        return diff.toString()
    }

    /**
     * 读取仓库近期提交消息作为风格参考
     * （对应 /git-commit-push 的 `git log --pretty=format:%B`，条数见 [AiCommitSettings.RECENT_COMMITS_COUNT]）。
     */
    private fun collectRecentCommits(): String {
        val basePath = project?.basePath ?: return ""
        return try {
            val pb = ProcessBuilder("git", "log", "-${AiCommitSettings.RECENT_COMMITS_COUNT}", "--pretty=format:%B")
            pb.directory(File(basePath))
            pb.environment()["PATH"] = SdkManager.pathEnv()
            pb.redirectErrorStream(false)
            val process = pb.start()
            val output = process.inputStream.bufferedReader().readText()
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return ""
            }
            if (process.exitValue() == 0) output.trim().take(2000) else ""
        } catch (e: Exception) {
            LOG.debug("collect recent commits failed: ${e.message}")
            ""
        }
    }

    // ---------------------------------------------------------------
    // SDK 调用（非交互模式，复用 SDK 登录凭证）
    // ---------------------------------------------------------------

    private fun runSdk(
        sdk: SdkDefinition,
        cliPath: String,
        prompt: String,
        rawModel: String,
        indicator: ProgressIndicator? = null
    ): String {
        // 思考等级跟随模型写在末尾（如 "claude-opus-4-5 high"），此处拆开转成各 CLI 参数
        val (model, thinking) = SdkDefinition.splitModelThinking(sdk, rawModel)
        return when (sdk) {
            SdkDefinition.CLAUDE_SDK -> runClaude(cliPath, prompt, model, thinking, indicator)
            SdkDefinition.CODEX_SDK -> runCodex(cliPath, prompt, model, thinking, indicator)
            SdkDefinition.CURSOR_SDK -> runCursor(cliPath, prompt, model, indicator)
            SdkDefinition.OPENCODE_SDK -> runOpenCode(cliPath, prompt, model, indicator)
            SdkDefinition.GEMINI_SDK -> runGemini(cliPath, prompt, model, indicator)
            SdkDefinition.GROK_SDK -> runGrok(cliPath, prompt, model, indicator)
            SdkDefinition.OMP_SDK -> runOmp(cliPath, prompt, model, thinking, indicator)
        }
    }

    /** claude -p --output-format text，提示词经 stdin 传入，使用 claude /login 的凭证 */
    private fun runClaude(cliPath: String, prompt: String, model: String, thinking: String, indicator: ProgressIndicator?): String {
        val command = mutableListOf(cliPath, "-p", "--output-format", "text")
        if (model.isNotBlank()) {
            command.add("--model")
            command.add(model)
        }
        if (thinking.isNotBlank()) {
            command.add("--effort")
            command.add(thinking)
        }
        return execute(command, stdin = prompt, indicator = indicator)
    }

    /** codex exec，最终回复写入临时文件（--output-last-message），使用 codex login 的凭证 */
    private fun runCodex(cliPath: String, prompt: String, model: String, thinking: String, indicator: ProgressIndicator?): String {
        val outFile = Files.createTempFile("ai-commit-codex", ".txt").toFile()
        try {
            val command = mutableListOf(
                cliPath, "exec", "--skip-git-repo-check",
                "--output-last-message", outFile.absolutePath
            )
            if (model.isNotBlank()) {
                command.add("-m")
                command.add(model)
            }
            if (thinking.isNotBlank()) {
                command.add("-c")
                command.add("model_reasoning_effort=$thinking")
            }
            command.add(prompt)
            val stdout = execute(command, stdin = null, indicator = indicator)
            val lastMessage = outFile.readText(StandardCharsets.UTF_8).trim()
            return lastMessage.ifBlank { stdout }
        } finally {
            outFile.delete()
        }
    }

    /** cursor-agent --print 非交互模式；显式输出纯文本，避免默认流式 JSON。 */
    private fun runCursor(cliPath: String, prompt: String, model: String, indicator: ProgressIndicator?): String {
        val command = mutableListOf(cliPath, "--print", "--output-format", "text")
        if (model.isNotBlank()) {
            command.add("--model")
            command.add(model)
        }
        command.add(prompt)
        return execute(command, stdin = null, indicator = indicator)
    }

    /** opencode run 非交互模式；模型可留空，使用 OpenCode 当前配置。 */
    private fun runOpenCode(cliPath: String, prompt: String, model: String, indicator: ProgressIndicator?): String {
        val command = mutableListOf(cliPath, "run")
        if (model.isNotBlank()) {
            command.add("--model")
            command.add(model)
        }
        command.add(prompt)
        return execute(command, stdin = null, indicator = indicator)
    }

    /** agy --print 非交互模式（新版 Gemini/Antigravity CLI），思考等级包含在模型名里 */
    private fun runGemini(cliPath: String, prompt: String, model: String, indicator: ProgressIndicator?): String {
        val command = mutableListOf(cliPath, "-p", prompt)
        if (model.isNotBlank()) {
            command.add("--model")
            command.add(model)
        }
        return execute(command, stdin = null, indicator = indicator)
    }

    /** grok -p 非交互模式，提示词作为参数传入 */
    private fun runGrok(cliPath: String, prompt: String, model: String, indicator: ProgressIndicator?): String {
        val command = mutableListOf(cliPath, "-p", prompt)
        if (model.isNotBlank()) {
            command.add("-m")
            command.add(model)
        }
        return execute(command, stdin = null, indicator = indicator)
    }

    /** omp -p 非交互模式；禁用工具与会话持久化，只生成最终文本；模型可留空走 omp 自身默认。 */
    private fun runOmp(cliPath: String, prompt: String, model: String, thinking: String, indicator: ProgressIndicator?): String {
        val command = mutableListOf(cliPath, "-p", "--no-tools", "--no-session")
        if (model.isNotBlank()) {
            command.add("--model")
            command.add(model)
        }
        if (thinking.isNotBlank()) {
            command.add("--thinking")
            command.add(thinking)
        }
        command.add(prompt)
        return execute(command, stdin = null, indicator = indicator)
    }

    /**
     * 执行 CLI 进程。等待方式为 200ms 轮询而非一次性死等：
     * - 用户在进度条上点取消 -> 立即杀掉子进程并抛 [ProcessCanceledException]
     * - 每秒在进度条上更新已用时，便于区分"执行慢"和"卡死"
     * - 超过 [TIMEOUT_SECONDS] -> 杀掉子进程并报超时
     */
    private fun execute(command: List<String>, stdin: String?, indicator: ProgressIndicator? = null): String {
        LOG.info("Run SDK: ${command.first()} ${command.getOrNull(1) ?: ""}")
        val pb = ProcessBuilder(command)
        project?.basePath?.let { pb.directory(File(it)) }
        pb.environment()["PATH"] = File(command.first()).parent + ":" + SdkManager.pathEnv()
        val process = pb.start()

        if (stdin != null) {
            process.outputStream.use { it.write(stdin.toByteArray(StandardCharsets.UTF_8)) }
        } else {
            process.outputStream.close()
        }

        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val outReader = Thread { process.inputStream.bufferedReader().forEachLine { stdout.appendLine(it) } }
        val errReader = Thread { process.errorStream.bufferedReader().forEachLine { stderr.appendLine(it) } }
        outReader.start(); errReader.start()

        val startAt = System.currentTimeMillis()
        while (true) {
            if (process.waitFor(200, TimeUnit.MILLISECONDS)) break
            val elapsedSec = (System.currentTimeMillis() - startAt) / 1000
            if (indicator != null && indicator.isCanceled) {
                process.destroyForcibly()
                throw ProcessCanceledException()
            }
            indicator?.text2 = "已用时 ${elapsedSec}s（上限 ${TIMEOUT_SECONDS}s，可点击取消）"
            if (elapsedSec >= TIMEOUT_SECONDS) {
                process.destroyForcibly()
                throw RuntimeException("SDK 调用超时（${TIMEOUT_SECONDS}秒）")
            }
        }
        outReader.join(3000); errReader.join(3000)

        if (process.exitValue() != 0) {
            val err = (stderr.toString().ifBlank { stdout.toString() }).trim().take(500)
            val hint = if (err.contains("login", true) || err.contains("auth", true) ||
                err.contains("credential", true) || err.contains("API key", true)
            ) "\n提示：请先在终端完成所选 CLI 的登录或认证配置" else ""
            throw RuntimeException("SDK 调用失败（退出码 ${process.exitValue()}）: $err$hint")
        }
        return stdout.toString().trim()
    }

    /** 供设置面板"测试"使用，成功返回 null，失败返回错误信息 */
    fun testSdk(sdk: SdkDefinition, model: String): String? {
        return try {
            val status = SdkManager.detect(sdk, null)
            if (!status.installed || status.path == null) {
                return "未检测到 ${sdk.displayName}（${sdk.binName} 命令），请先安装并登录"
            }
            val reply = runSdk(sdk, status.path, "请只回复：OK", model)
            if (reply.isBlank()) "调用成功但返回内容为空" else null
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }
    }

    // ---------------------------------------------------------------
    // 输出清洗（与 CC GUI 的 cleanupCommitMessage 逻辑一致）
    // ---------------------------------------------------------------

    fun cleanupCommitMessage(message: String): String {
        if (message.isEmpty()) return ""
        var cleaned = removeThinkingMarkers(message)

        // 1. 优先提取 <commit></commit> 标签内容
        val start = cleaned.indexOf(COMMIT_TAG_START)
        val end = cleaned.indexOf(COMMIT_TAG_END)
        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start + COMMIT_TAG_START.length, end)
            return convertLiteralNewlines(cleaned.trim())
        }

        // 2. 提取 ``` 代码块内容
        if (cleaned.contains("```")) {
            val codeBlockStart = cleaned.indexOf("```")
            val contentStart = cleaned.indexOf('\n', codeBlockStart)
            if (contentStart != -1) {
                val codeBlockEnd = cleaned.indexOf("```", contentStart)
                if (codeBlockEnd != -1) {
                    return convertLiteralNewlines(cleaned.substring(contentStart + 1, codeBlockEnd).trim())
                }
            }
        }

        // 3. 定位符合 Conventional Commits 格式的行
        val lines = cleaned.split("\n")
        for ((idx, line) in lines.withIndex()) {
            val trimmedLine = line.trim()
            if (isConventionalCommitLine(trimmedLine)) {
                val result = StringBuilder(trimmedLine)
                var inBody = false
                for (i in idx + 1 until lines.size) {
                    val nextLine = lines[i].trim()
                    if (isAnalysisSection(nextLine)) break
                    if (nextLine.isEmpty()) {
                        inBody = true
                        result.append("\n")
                    } else if (inBody && !nextLine.startsWith("#") && !nextLine.startsWith("*")) {
                        result.append("\n").append(nextLine)
                    } else if (!inBody) {
                        break
                    }
                }
                return convertLiteralNewlines(result.toString().trim())
            }
        }

        // 4. 兜底：取前几行非分析内容
        val fallback = StringBuilder()
        for (line in lines) {
            val trimmedLine = line.trim()
            if (isAnalysisSection(trimmedLine)) break
            if (trimmedLine.isNotEmpty()) {
                fallback.append(trimmedLine).append("\n")
            }
            if (fallback.toString().split("\n").size >= 5) break
        }
        return convertLiteralNewlines(fallback.toString().trim())
    }

    private fun convertLiteralNewlines(text: String): String {
        var result = text.replace("\\n", "\n")
        result = result.replaceFirst(Regex("^\\n+"), "")
        result = result.replace(Regex("\\n{3,}"), "\n\n")
        return result.trim()
    }

    private fun isConventionalCommitLine(line: String): Boolean {
        if (line.isEmpty()) return false
        val types = listOf("feat", "fix", "refactor", "docs", "test", "chore", "perf", "ci", "style", "build")
        return types.any { line.startsWith("$it:") || line.startsWith("$it(") }
    }

    private fun isAnalysisSection(line: String): Boolean {
        val analysisKeywords = listOf(
            "分析说明", "变更特征", "分析：", "说明：", "解释：", "备注：",
            "Analysis:", "Explanation:", "Note:", "---", "===",
            "1. 类型", "2. Scope", "3. 描述", "4. Body", "• ", "- 无需", "- 不涉及"
        )
        if (analysisKeywords.any { line.contains(it) }) return true
        return line.matches(Regex("^\\d+\\.\\s+.*"))
    }

    private fun removeThinkingMarkers(content: String): String {
        var result = content
        while (result.contains("<thinking>") && result.contains("</thinking>")) {
            val start = result.indexOf("<thinking>")
            val end = result.indexOf("</thinking>") + "</thinking>".length
            if (start >= end) break
            result = result.substring(0, start) + result.substring(end)
        }
        val uiMarkers = listOf("思考 ▸", "思考▸", "思考 ►", "思考►", "Thinking ▸", "Thinking▸")
        for (marker in uiMarkers) {
            result = result.replace(marker, "")
        }
        result = result.replaceFirst(Regex("^\\s*\\n+"), "")
        return result.trim()
    }

    companion object {
        private val LOG = Logger.getInstance(AiCommitService::class.java)
        private const val COMMIT_TAG_START = "<commit>"
        private const val COMMIT_TAG_END = "</commit>"
        private const val TIMEOUT_SECONDS = 180L
    }
}
