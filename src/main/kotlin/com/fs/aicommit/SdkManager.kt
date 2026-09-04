package com.fs.aicommit

import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * CLI 定义：直接使用本机已安装的 Claude、Codex、Cursor、OpenCode、Gemini(agy)、Grok 或 OMP CLI，
 * 认证复用 CLI 自身的登录凭证，无需在插件中配置 API Key。
 *
 * [thinkingLevels] 为该 CLI 支持的思考等级（跟随模型写在模型名末尾，如 "claude-opus-4-5 high"）；
 * Gemini 的思考等级已包含在模型显示名里（如 "Gemini 3.1 Pro (High)"），Grok 不支持。
 */
enum class SdkDefinition(
    val id: String,
    val displayName: String,
    val binName: String,
    val defaultModel: String,
    val description: String,
    val thinkingLevels: List<String> = emptyList()
) {
    CLAUDE_SDK(
        "claude", "Claude CLI", "claude", "claude-sonnet-4-6",
        "使用 Claude Code 的登录凭证（claude /login），无需 API Key",
        listOf("low", "medium", "high", "xhigh", "max")
    ),
    CODEX_SDK(
        "codex", "Codex CLI", "codex", "gpt-5.5",
        "使用 Codex 的登录凭证（codex login），无需 API Key",
        listOf("minimal", "low", "medium", "high", "xhigh")
    ),
    CURSOR_SDK(
        "cursor", "Cursor CLI", "cursor-agent", "auto",
        "使用 Cursor Agent 的登录凭证（cursor-agent login），无需 API Key"
    ),
    OPENCODE_SDK(
        "opencode", "OpenCode CLI", "opencode", "",
        "使用 OpenCode 已配置的模型提供方与认证，模型留空走 OpenCode 当前配置"
    ),
    GEMINI_SDK(
        "gemini", "Gemini CLI (agy)", "agy", "Gemini 3.1 Pro (High)",
        "使用 Antigravity 的登录凭证，思考等级包含在模型名里"
    ),
    GROK_SDK(
        "grok", "Grok CLI", "grok", "grok-4.1",
        "使用 Grok 的登录凭证（grok login），无需 API Key"
    ),
    OMP_SDK(
        "omp", "OMP CLI", "omp", "",
        "使用 OMP 已配置的模型提供方与认证，模型留空走 omp 自身默认",
        listOf("off", "minimal", "low", "medium", "high", "xhigh", "max", "auto")
    );

    companion object {
        fun fromProvider(provider: String): SdkDefinition =
            values().firstOrNull { it.id.equals(provider, ignoreCase = true) } ?: CLAUDE_SDK

        /**
         * 拆分 "模型名 [思考等级]"：末尾单词命中该 CLI 的等级表时拆出，
         * 否则整串当模型名（gemini 的 "(High)" 带括号不会命中，安全透传）。
         */
        fun splitModelThinking(sdk: SdkDefinition, raw: String): Pair<String, String> {
            val text = raw.trim()
            if (sdk.thinkingLevels.isEmpty() || text.isEmpty()) return text to ""
            val idx = text.lastIndexOf(' ')
            if (idx == -1) return text to ""
            val tail = text.substring(idx + 1).lowercase()
            return if (tail in sdk.thinkingLevels) text.substring(0, idx).trim() to tail else text to ""
        }
    }
}

data class SdkStatus(
    val installed: Boolean,
    val version: String?,
    val path: String?,
    val detail: String
)

/**
 * CLI 检测与模型列表获取。
 */
object SdkManager {
    private val LOG = Logger.getInstance(SdkManager::class.java)

    private val HOME: String = System.getProperty("user.home")

    /** OpenRouter 拉取失败时的兜底候选 */
    private val CLAUDE_FALLBACK_MODELS = listOf(
        "claude-sonnet-4-6", "claude-opus-4-8", "claude-opus-4-7", "claude-haiku-4-5"
    )
    private val CODEX_FALLBACK_MODELS = listOf("gpt-5.5", "gpt-5.4", "gpt-5.4-mini")
    private val CURSOR_FALLBACK_MODELS = listOf("auto")
    private val OPENCODE_FALLBACK_MODELS = emptyList<String>()
    private val GEMINI_FALLBACK_MODELS = listOf(
        "Gemini 3.1 Pro (High)", "Gemini 3.1 Pro (Low)",
        "Gemini 3.5 Flash (High)", "Gemini 3.5 Flash (Medium)", "Gemini 3.5 Flash (Low)"
    )
    private val GROK_FALLBACK_MODELS = listOf("grok-4.1", "grok-4-fast", "grok-code-fast-1")
    private val OMP_FALLBACK_MODELS = listOf(
        "openai-codex/gpt-5.4", "anthropic/claude-sonnet-4-6"
    )

    private const val OPENROUTER_MODELS_URL = "https://openrouter.ai/api/v1/models"

    /** 常见的 CLI 安装位置（GUI 启动的 IDE 拿不到 shell rc 里的 PATH，需要显式扫描） */
    private fun candidatePaths(sdk: SdkDefinition): List<Path> {
        val bin = sdk.binName
        val candidates = mutableListOf(
            Paths.get(HOME, ".local", "bin", bin),
            Paths.get(HOME, ".cursor", "bin", bin),
            Paths.get(HOME, ".opencode", "bin", bin),
            Paths.get(HOME, ".grok", "bin", bin),
            Paths.get(HOME, ".claude", "local", bin),
            Paths.get(HOME, ".omp", "bin", bin),
            Paths.get("/opt/homebrew/bin", bin),
            Paths.get("/usr/local/bin", bin),
            Paths.get(HOME, ".npm-global", "bin", bin),
            Paths.get(HOME, ".volta", "bin", bin),
            Paths.get(HOME, ".bun", "bin", bin),
            Paths.get(HOME, ".codex", "bin", bin)
        )
        // nvm 安装的 node 全局包
        val nvmVersions = Paths.get(HOME, ".nvm", "versions", "node").toFile()
        if (nvmVersions.isDirectory) {
            nvmVersions.listFiles()?.sortedDescending()?.forEach {
                candidates.add(it.toPath().resolve("bin").resolve(bin))
            }
        }
        return candidates
    }

    /** 检测 CLI：overridePath 优先，其次扫描常见位置，最后走登录 shell 的 which */
    fun detect(sdk: SdkDefinition, overridePath: String?): SdkStatus {
        val paths = mutableListOf<Path>()
        if (!overridePath.isNullOrBlank()) {
            paths.add(Paths.get(overridePath.trim()))
        } else {
            paths.addAll(candidatePaths(sdk))
        }
        for (p in paths) {
            if (Files.isExecutable(p) || Files.isRegularFile(p)) {
                val version = readVersion(p.toString())
                if (version != null) {
                    return SdkStatus(true, version, p.toString(), "已安装 v$version")
                }
            }
        }
        // 最后尝试登录 shell 的 which（可能较慢）
        if (overridePath.isNullOrBlank()) {
            whichInLoginShell(sdk.binName)?.let { found ->
                val version = readVersion(found)
                if (version != null) {
                    return SdkStatus(true, version, found, "已安装 v$version")
                }
            }
        }
        return SdkStatus(false, null, null, "未检测到 ${sdk.binName} 命令")
    }

    private fun readVersion(cliPath: String): String? {
        return try {
            val process = ProcessBuilder(cliPath, "--version")
                .redirectErrorStream(true)
                .apply { environment()["PATH"] = pathEnv() }
                .start()
            // 必须立刻关闭 stdin：agy 等 CLI 在 stdin 为打开的管道时会一直等 EOF
            process.outputStream.close()
            val output = process.inputStream.bufferedReader().readText().trim()
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return null
            }
            if (process.exitValue() != 0) return null
            // 输出形如 "2.1.197 (Claude Code)" 或 "codex-cli 0.142.2"
            Regex("\\d+\\.\\d+[\\w.-]*").find(output)?.value
        } catch (e: Exception) {
            LOG.debug("readVersion failed for $cliPath: ${e.message}")
            null
        }
    }

    private fun whichInLoginShell(bin: String): String? {
        return try {
            val process = ProcessBuilder("/bin/zsh", "-lc", "which $bin")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return null
            }
            if (process.exitValue() == 0 && output.isNotBlank() && File(output.lines().last()).exists()) {
                output.lines().last()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /** 组装给子进程用的 PATH：把常见 node/CLI 目录都拼上，避免 GUI 环境 PATH 缺失 */
    fun pathEnv(): String {
        val dirs = linkedSetOf(
            "/opt/homebrew/bin", "/usr/local/bin", "/usr/bin", "/bin",
            "$HOME/.local/bin", "$HOME/.npm-global/bin", "$HOME/.volta/bin", "$HOME/.bun/bin"
        )
        val nvmVersions = Paths.get(HOME, ".nvm", "versions", "node").toFile()
        if (nvmVersions.isDirectory) {
            nvmVersions.listFiles()?.sortedDescending()?.forEach { dirs.add(it.resolve("bin").absolutePath) }
        }
        System.getenv("PATH")?.let { dirs.addAll(it.split(":")) }
        return dirs.joinToString(":")
    }

    /**
     * 获取 CLI 可选模型。Cursor 读取本地 `cursor-agent models`（当前账号可用模型），
     * OpenCode 读取本地 `opencode models`（provider/model），
     * OMP 读取本地 `omp models --json`（selector 全称，避免模糊匹配歧义），
     * Gemini 读取本地 `agy models`（每行一个显示名，思考等级已含在名字里），
     * Claude/Codex/Grok 从 OpenRouter 拉取对应厂商模型。
     * 网络或命令失败时返回内置兜底候选。需在后台线程调用。
     */
    fun fetchModels(sdk: SdkDefinition): List<String> {
        if (sdk == SdkDefinition.CURSOR_SDK) return fetchCursorModels()
        if (sdk == SdkDefinition.OPENCODE_SDK) return fetchOpenCodeModels()
        if (sdk == SdkDefinition.OMP_SDK) return fetchOmpModels()
        if (sdk == SdkDefinition.GEMINI_SDK) return fetchAgyModels()
        val prefix = when (sdk) {
            SdkDefinition.CODEX_SDK -> "openai/"
            SdkDefinition.GROK_SDK -> "x-ai/"
            else -> "anthropic/"
        }
        try {
            val conn = URL(OPENROUTER_MODELS_URL).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 30_000
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val data = JsonParser.parseString(text).asJsonObject.getAsJsonArray("data")
                    val models = data?.mapNotNull { el ->
                        val id = el.asJsonObject.get("id")?.asString ?: return@mapNotNull null
                        if (!id.startsWith(prefix)) return@mapNotNull null
                        val slug = id.removePrefix(prefix)
                        if (sdk == SdkDefinition.CLAUDE_SDK) slug.replace('.', '-') else slug
                    }.orEmpty().distinct()
                    if (models.isNotEmpty()) return models
                } else {
                    LOG.warn("OpenRouter models API returned HTTP ${conn.responseCode}")
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            LOG.warn("fetch OpenRouter models failed: ${e.message}")
        }
        return when (sdk) {
            SdkDefinition.CODEX_SDK -> CODEX_FALLBACK_MODELS
            SdkDefinition.CURSOR_SDK -> CURSOR_FALLBACK_MODELS
            SdkDefinition.OPENCODE_SDK -> OPENCODE_FALLBACK_MODELS
            SdkDefinition.GROK_SDK -> GROK_FALLBACK_MODELS
            else -> CLAUDE_FALLBACK_MODELS
        }
    }

    /** cursor-agent models：每行格式为 "模型 ID - 展示名"。 */
    private fun fetchCursorModels(): List<String> {
        val status = detect(SdkDefinition.CURSOR_SDK, null)
        val cliPath = status.path ?: return CURSOR_FALLBACK_MODELS
        return try {
            val process = ProcessBuilder(cliPath, "models")
                .apply { environment()["PATH"] = pathEnv() }
                .start()
            process.outputStream.close()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return CURSOR_FALLBACK_MODELS
            }
            if (process.exitValue() != 0) return CURSOR_FALLBACK_MODELS
            val models = output.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != "Available models" }
                .map { it.substringBefore(" - ").trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            models.ifEmpty { CURSOR_FALLBACK_MODELS }
        } catch (e: Exception) {
            LOG.warn("fetch Cursor models failed: ${e.message}")
            CURSOR_FALLBACK_MODELS
        }
    }

    /** opencode models：每行一个 provider/model。 */
    private fun fetchOpenCodeModels(): List<String> {
        val status = detect(SdkDefinition.OPENCODE_SDK, null)
        val cliPath = status.path ?: return OPENCODE_FALLBACK_MODELS
        return try {
            val process = ProcessBuilder(cliPath, "models")
                .apply { environment()["PATH"] = pathEnv() }
                .start()
            process.outputStream.close()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return OPENCODE_FALLBACK_MODELS
            }
            if (process.exitValue() != 0) return OPENCODE_FALLBACK_MODELS
            output.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        } catch (e: Exception) {
            LOG.warn("fetch OpenCode models failed: ${e.message}")
            OPENCODE_FALLBACK_MODELS
        }
    }

    /** agy models：每行一个模型显示名 */
    private fun fetchAgyModels(): List<String> {
        val status = detect(SdkDefinition.GEMINI_SDK, null)
        val cliPath = status.path ?: return GEMINI_FALLBACK_MODELS
        return try {
            val process = ProcessBuilder(cliPath, "models")
                .apply { environment()["PATH"] = pathEnv() }
                .start()
            // 关闭 stdin（agy 会等 stdin EOF）；不合并 stderr，避免日志行混进模型列表
            process.outputStream.close()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return GEMINI_FALLBACK_MODELS
            }
            if (process.exitValue() != 0) return GEMINI_FALLBACK_MODELS
            val models = output.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            models.ifEmpty { GEMINI_FALLBACK_MODELS }
        } catch (e: Exception) {
            LOG.warn("fetch agy models failed: ${e.message}")
            GEMINI_FALLBACK_MODELS
        }
    }

    private fun fetchOmpModels(): List<String> {
        val status = detect(SdkDefinition.OMP_SDK, null)
        val cliPath = status.path ?: return OMP_FALLBACK_MODELS
        return try {
            val process = ProcessBuilder(cliPath, "models", "--json")
                .apply { environment()["PATH"] = pathEnv() }
                .start()
            process.outputStream.close()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return OMP_FALLBACK_MODELS
            }
            if (process.exitValue() != 0) return OMP_FALLBACK_MODELS
            val models = JsonParser.parseString(output).asJsonObject
                .getAsJsonArray("models")
                ?.mapNotNull { it.asJsonObject.get("selector")?.asString }
                .orEmpty()
                .distinct()
            models.ifEmpty { OMP_FALLBACK_MODELS }
        } catch (e: Exception) {
            LOG.warn("fetch OMP models failed: ${e.message}")
            OMP_FALLBACK_MODELS
        }
    }
}
