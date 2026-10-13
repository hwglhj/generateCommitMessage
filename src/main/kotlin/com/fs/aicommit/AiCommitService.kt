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
import com.fs.i18n.SubMsgBundle
import com.intellij.util.diff.Diff

/**
 * AI 生成 Commit Message 服务。
 * 链路与 CC GUI 的 GitCommitMessageService 一致：
 * 变更列表 -> 生成 diff 摘要 -> 组装提示词 -> 调用 SDK -> 清洗输出。
 *
 * SDK 调用使用所选 CLI 的非交互模式，认证复用 CLI 自身的登录凭证，
 * 无需在插件中配置 API Key。
 */
class AiCommitService(
    private val project: Project?,
    private val settings: AiCommitSettings = AiCommitSettings.getInstance(),
    private val apiClient: ApiClient = ApiClient()
) {

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
        indicator?.text = SubMsgBundle.message("commit.collecting", changes.size + unversionedFiles.size)
        val diff = generateDiff(changes, unversionedFiles)
        if (diff.isBlank()) {
            throw RuntimeException(SubMsgBundle.message("error.diff.empty"))
        }
        indicator?.checkCanceled()

        val recentCommits = if (settings.includeRecentCommits) {
            indicator?.text = SubMsgBundle.message("commit.recent")
            collectRecentCommits()
        } else {
            ""
        }
        return generateFromDiff(diff, recentCommits, indicator)
    }

    internal fun generateFromDiff(diff: String, recentCommits: String = "", indicator: ProgressIndicator? = null): String {
        if (diff.isBlank()) throw IllegalArgumentException(SubMsgBundle.message("error.diff.empty"))
        val limitedDiff = diff.take(settings.maxDiffLength.coerceAtLeast(AiCommitSettings.MIN_MAX_DIFF_LENGTH))
        val prompt = AiCommitPrompts.buildFullPrompt(limitedDiff, if (settings.includeRecentCommits) recentCommits else "", settings.customPrompt, settings.outputLanguage)
        indicator?.checkCanceled()

        val raw = if (settings.mode == AiCommitSettings.MODE_API) {
            val config = settings.activeApiConfig() ?: throw ApiException(SubMsgBundle.message("api.select.config"))
            indicator?.text = SubMsgBundle.message("api.generating", config.name, config.model)
            apiClient.complete(ApiCredentials.resolve(config), prompt) { indicator?.checkCanceled() }
        } else {
            val sdk = settings.activeSdk()
            val model = settings.modelFor(sdk)
            indicator?.text = SubMsgBundle.message("cli.detecting", sdk.displayName)
            val status = SdkManager.detect(sdk, settings.cliPath)
            if (!status.installed || status.path == null) {
                throw RuntimeException(SubMsgBundle.message("message.cli.notfound", sdk.binName))
            }
            indicator?.text = SubMsgBundle.message("api.generating", sdk.displayName, model)
            runSdk(sdk, status.path, prompt, model, indicator)
        }

        indicator?.text = SubMsgBundle.message("commit.cleaning")
        val message = cleanupCommitMessage(raw)
        if (message.isBlank()) {
            throw RuntimeException(SubMsgBundle.message("commit.empty"))
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
                        diff.append("--- deleted\n")
                        before.content?.let { diff.append(it.take(maxDiffLength)).append('\n') }
                    }
                    before != null && after != null -> {
                        if (before.file.path != after.file.path) diff.append("--- ").append(before.file.path).append('\n')
                        val b = before.content
                        val a = after.content
                        if (b != null && a != null) {
                            diff.append(generateSimpleDiff(b, a))
                        }
                    }
                }
                if (diff.length > maxDiffLength) {
                    return diff.take(maxDiffLength).toString()
                }
            } catch (e: ProcessCanceledException) {
                throw e
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
                    return diff.take(maxDiffLength).toString()
                }
            } catch (e: Exception) {
                LOG.warn("Failed to read unversioned file ${filePath.path}: ${e.message}")
            }
        }
        return diff.toString()
    }

    private fun appendNewFileContent(diff: StringBuilder, content: String?) {
        if (content == null) return
        val limit = settings.maxDiffLength.coerceAtLeast(AiCommitSettings.MIN_MAX_DIFF_LENGTH)
        diff.append("+++ ").append(content.take(limit)).append('\n')
    }

    private fun readUnversionedContent(filePath: FilePath): String? {
        val file = filePath.ioFile
        if (!file.isFile || !file.canRead()) return null
        val limit = settings.maxDiffLength.coerceIn(AiCommitSettings.MIN_MAX_DIFF_LENGTH, 1_000_000)
        val bytes = file.inputStream().use { it.readNBytes(limit) }
        if (bytes.any { it == 0.toByte() }) return "[二进制文件]"
        return String(bytes, StandardCharsets.UTF_8)
    }

    internal fun generateSimpleDiff(before: String, after: String): String {
        val beforeLines = Diff.splitLines(before)
        val afterLines = Diff.splitLines(after)
        val diff = StringBuilder()
        val limit = settings.maxDiffLength.coerceAtLeast(AiCommitSettings.MIN_MAX_DIFF_LENGTH)
        var change = Diff.buildChanges(beforeLines, afterLines)
        while (change != null) {
            diff.append("@@ -${change.line0 + 1},${change.deleted} +${change.line1 + 1},${change.inserted} @@\n")
            for (line in change.line0 until change.line0 + change.deleted) {
                diff.append("- ").append(beforeLines[line]).append('\n')
                if (diff.length >= limit) return diff.take(limit).toString()
            }
            for (line in change.line1 until change.line1 + change.inserted) {
                diff.append("+ ").append(afterLines[line]).append('\n')
                if (diff.length >= limit) return diff.take(limit).toString()
            }
            change = change.link
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
            val result = CliProcess.run(listOf("git", "log", "-${AiCommitSettings.RECENT_COMMITS_COUNT}", "--pretty=format:%B"),
                directory = File(basePath), timeoutMillis = 15000)
            if (result.exitCode == 0) result.stdout.trim().take(2000) else ""
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
            SdkDefinition.PI_SDK -> runPi(cliPath, prompt, model, thinking, indicator)
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
            command.add("-")
            val stdout = execute(command, stdin = prompt, indicator = indicator)
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

    /** Pi print mode：不保存会话、不调用工具，也不加载项目级上下文文件。 */
    private fun runPi(cliPath: String, prompt: String, model: String, thinking: String, indicator: ProgressIndicator?): String =
        execute(buildPiCommand(cliPath, prompt, model, thinking), stdin = null, indicator = indicator)

    /**
     * 执行 CLI 进程。等待方式为 200ms 轮询而非一次性死等：
     * - 用户在进度条上点取消 -> 立即杀掉子进程并抛 [ProcessCanceledException]
     * - 每秒在进度条上更新已用时，便于区分"执行慢"和"卡死"
     * - 超过 [TIMEOUT_SECONDS] -> 杀掉子进程并报超时
     */
    private fun execute(command: List<String>, stdin: String?, indicator: ProgressIndicator? = null): String {
        LOG.info("Run SDK: ${command.first()} ${command.getOrNull(1) ?: ""}")
        val result = CliProcess.run(command, stdin, project?.basePath?.let { File(it) },
            timeoutMillis = TIMEOUT_SECONDS * 1000L,
            checkCanceled = { indicator?.checkCanceled() },
            onTick = { indicator?.text2 = SubMsgBundle.message("cli.elapsed", it, TIMEOUT_SECONDS) })
        if (result.exitCode != 0) {
            val err = (result.stderr.ifBlank { result.stdout }).trim().take(500)
            val hint = if (err.contains("login", true) || err.contains("auth", true) ||
                err.contains("credential", true) || err.contains("API key", true)
            ) "\n" + SubMsgBundle.message("cli.auth.hint") else ""
            throw RuntimeException(SubMsgBundle.message("cli.failed", result.exitCode, err + hint))
        }
        return result.stdout.trim()
    }

    /** 供设置面板"测试"使用，成功返回 null，失败返回错误信息 */
    fun testSdk(sdk: SdkDefinition, model: String, cliPath: String? = null, indicator: ProgressIndicator? = null): String? {
        return try {
            val status = SdkManager.detect(sdk, cliPath)
            if (!status.installed || status.path == null) {
                return SubMsgBundle.message("message.cli.notfound", sdk.binName)
            }
            val reply = runSdk(sdk, status.path, "Reply with OK only.", model, indicator)
            if (reply.isBlank()) SubMsgBundle.message("commit.empty") else null
        } catch (e: ProcessCanceledException) {
            throw e
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

        internal fun buildPiCommand(cliPath: String, prompt: String, model: String, thinking: String): List<String> {
            val command = mutableListOf(
                cliPath, "--print", "--no-session", "--no-tools", "--no-context-files", "--no-approve"
            )
            if (model.isNotBlank()) command.addAll(listOf("--model", model))
            if (thinking.isNotBlank()) command.addAll(listOf("--thinking", thinking))
            command.add(prompt)
            return command
        }
    }
}
