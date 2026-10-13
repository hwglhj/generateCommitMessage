package com.fs.aicommit

import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import com.fs.i18n.SubMsgBundle.message as tr
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * CLI 定义：直接使用本机已安装的 Claude、Codex、Cursor、OpenCode、Gemini(agy)、Grok、OMP 或 Pi CLI，
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
    ),
    PI_SDK(
        "pi", "Pi CLI", "pi", "",
        "使用 Pi Coding Agent 已配置的模型提供方与认证，模型留空走 Pi 自身默认",
        listOf("off", "minimal", "low", "medium", "high", "xhigh", "max")
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
    private val PI_FALLBACK_MODELS = emptyList<String>()

    private const val OPENROUTER_MODELS_URL = "https://openrouter.ai/api/v1/models"

    /** 快捷菜单必须即时打开，不在 EDT 上检测 CLI 或请求远程模型列表。 */
    internal fun quickModelCandidates(sdk: SdkDefinition): List<String> =
        ProviderModelPanel.cachedModels(sdk.id).ifEmpty { fallbackModels(sdk) }

    private fun fallbackModels(sdk: SdkDefinition): List<String> = when (sdk) {
        SdkDefinition.CLAUDE_SDK -> CLAUDE_FALLBACK_MODELS
        SdkDefinition.CODEX_SDK -> CODEX_FALLBACK_MODELS
        SdkDefinition.CURSOR_SDK -> CURSOR_FALLBACK_MODELS
        SdkDefinition.OPENCODE_SDK -> OPENCODE_FALLBACK_MODELS
        SdkDefinition.GEMINI_SDK -> GEMINI_FALLBACK_MODELS
        SdkDefinition.GROK_SDK -> GROK_FALLBACK_MODELS
        SdkDefinition.OMP_SDK -> OMP_FALLBACK_MODELS
        SdkDefinition.PI_SDK -> PI_FALLBACK_MODELS
    }

    internal fun executableNames(bin: String, osName: String = System.getProperty("os.name")): List<String> =
        if (osName.startsWith("Windows", true)) listOf("$bin.exe", "$bin.cmd", "$bin.bat", bin) else listOf(bin)

    internal fun installationDirectories(osName: String, home: String, env: Map<String, String>): List<String> {
        fun envValue(name: String) = env.entries.firstOrNull { it.key.equals(name, true) }?.value
        val directories = linkedSetOf<String>()
        for (relative in listOf(".local/bin", ".cursor/bin", ".opencode/bin", ".grok/bin", ".claude/local", ".omp/bin", ".pi/agent/bin", ".npm-global/bin", ".volta/bin", ".bun/bin", ".codex/bin")) {
            directories.add(File(home, relative).path)
        }
        if (osName.startsWith("Windows", true)) {
            envValue("APPDATA")?.let { directories.add(File(it, "npm").path) }
            envValue("LOCALAPPDATA")?.let { local ->
                directories.add(File(local, "Microsoft/WinGet/Links").path)
                for (tool in listOf("nodejs", "Claude", "Cursor", "OpenCode", "Antigravity")) {
                    directories.add(File(local, "Programs/$tool").path)
                    directories.add(File(local, "Programs/$tool/bin").path)
                }
            }
            envValue("ProgramFiles")?.let { directories.add(File(it, "nodejs").path) }
            for (key in listOf("NVM_SYMLINK", "NVM_HOME", "VOLTA_HOME", "BUN_INSTALL")) {
                envValue(key)?.let { directories.add(it); directories.add(File(it, "bin").path) }
            }
        } else {
            directories.addAll(listOf("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin", "/bin"))
            File(home, ".nvm/versions/node").listFiles()?.sortedByDescending { it.name }?.forEach {
                directories.add(File(it, "bin").path)
            }
        }
        return directories.toList()
    }

    internal fun mergePath(directories: List<String>, existing: String?, osName: String = System.getProperty("os.name")): String {
        val separator = if (osName.startsWith("Windows", true)) ";" else ":"
        return (directories + existing.orEmpty().split(separator)).filter { it.isNotBlank() }
            .distinctBy { if (separator == ";") it.lowercase() else it }.joinToString(separator)
    }

    private fun candidatePaths(sdk: SdkDefinition): List<Path> {
        val separator = File.pathSeparator
        val directories = pathEnv().split(separator).map { it.trim().trim('"') }.filter { it.isNotBlank() }
        return directories.flatMap { dir -> executableNames(sdk.binName).mapNotNull { name ->
            try { Paths.get(dir, name) } catch (_: java.nio.file.InvalidPathException) { null }
        } }
    }

    /** 检测 CLI：overridePath 优先，其次扫描常见位置，最后走登录 shell 的 which */
    fun detect(sdk: SdkDefinition, overridePath: String?): SdkStatus {
        val paths = mutableListOf<Path>()
        if (!overridePath.isNullOrBlank()) {
            try { paths.add(Paths.get(overridePath.trim().trim('"'))) } catch (_: java.nio.file.InvalidPathException) { }
        } else {
            paths.addAll(candidatePaths(sdk))
        }
        for (p in paths) {
            if (Files.isExecutable(p) || Files.isRegularFile(p)) {
                val version = readVersion(p.toString())
                if (version != null) {
                    return SdkStatus(true, version, p.toString(), tr("message.cli.detected", sdk.displayName, version))
                }
            }
        }
        // 最后尝试登录 shell 的 which（可能较慢）
        if (overridePath.isNullOrBlank()) {
            whichInLoginShell(sdk.binName)?.let { found ->
                val version = readVersion(found)
                if (version != null) {
                    return SdkStatus(true, version, found, tr("message.cli.detected", sdk.displayName, version))
                }
            }
        }
        return SdkStatus(false, null, null, tr("message.cli.notfound", sdk.binName))
    }

    private fun readVersion(cliPath: String): String? {
        return try {
            val result = CliProcess.run(listOf(cliPath, "--version"), timeoutMillis = 20000)
            if (result.exitCode != 0) return null
            val output = result.stdout.ifBlank { result.stderr }.trim()
            // 输出形如 "2.1.197 (Claude Code)" 或 "codex-cli 0.142.2"
            Regex("\\d+\\.\\d+[\\w.-]*").find(output)?.value
        } catch (e: Exception) {
            LOG.debug("readVersion failed for $cliPath: ${e.message}")
            null
        }
    }

    private fun whichInLoginShell(bin: String): String? {
        return try {
            val command = if (System.getProperty("os.name").startsWith("Windows", true)) {
                listOf("where.exe", bin)
            } else {
                val shell = System.getenv("SHELL")?.takeIf { File(it).isFile } ?: "/bin/sh"
                listOf(shell, "-lc", "command -v $bin")
            }
            val result = CliProcess.run(command, timeoutMillis = 10000)
            if (result.exitCode == 0) result.stdout.lines().firstOrNull { File(it.trim()).isFile }?.trim() else null
        } catch (e: Exception) {
            null
        }
    }

    /** 组装给子进程用的 PATH：把常见 node/CLI 目录都拼上，避免 GUI 环境 PATH 缺失 */
    fun pathEnv(): String {
        val osName = System.getProperty("os.name")
        val env = System.getenv()
        val inherited = env.entries.firstOrNull { it.key.equals("PATH", true) }?.value.orEmpty()
        return mergePath(inherited.split(File.pathSeparator),
            installationDirectories(osName, HOME, env).joinToString(File.pathSeparator), osName)
    }

    /**
     * 获取 CLI 可选模型。Cursor 读取本地 `cursor-agent models`（当前账号可用模型），
     * OpenCode 读取本地 `opencode models`（provider/model），
     * OMP 读取本地 `omp models --json`（selector 全称，避免模糊匹配歧义），
     * Pi 读取本地 `pi --list-models`（当前认证环境实际可用的 provider/model），
     * Gemini 读取本地 `agy models`（每行一个显示名，思考等级已含在名字里），
     * Claude/Codex/Grok 从 OpenRouter 拉取对应厂商模型。
     * 网络或命令失败时返回内置兜底候选。需在后台线程调用。
     */
    fun fetchModels(sdk: SdkDefinition, overridePath: String? = null): List<String> {
        if (sdk == SdkDefinition.CURSOR_SDK) return fetchCursorModels(overridePath)
        if (sdk == SdkDefinition.OPENCODE_SDK) return fetchOpenCodeModels(overridePath)
        if (sdk == SdkDefinition.OMP_SDK) return fetchOmpModels(overridePath)
        if (sdk == SdkDefinition.PI_SDK) return fetchPiModels(overridePath)
        if (sdk == SdkDefinition.GEMINI_SDK) return fetchAgyModels(overridePath)
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
        return fallbackModels(sdk)
    }

    /** cursor-agent models：每行格式为 "模型 ID - 展示名"。 */
    private fun fetchCursorModels(overridePath: String?): List<String> {
        val status = detect(SdkDefinition.CURSOR_SDK, overridePath)
        val cliPath = status.path ?: return CURSOR_FALLBACK_MODELS
        return try {
            val result = CliProcess.run(listOf(cliPath, "models"), timeoutMillis = 30000)
            if (result.exitCode != 0) return CURSOR_FALLBACK_MODELS
            val output = result.stdout
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
    private fun fetchOpenCodeModels(overridePath: String?): List<String> {
        val status = detect(SdkDefinition.OPENCODE_SDK, overridePath)
        val cliPath = status.path ?: return OPENCODE_FALLBACK_MODELS
        return try {
            val result = CliProcess.run(listOf(cliPath, "models"), timeoutMillis = 30000)
            if (result.exitCode != 0) return OPENCODE_FALLBACK_MODELS
            val output = result.stdout
            output.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        } catch (e: Exception) {
            LOG.warn("fetch OpenCode models failed: ${e.message}")
            OPENCODE_FALLBACK_MODELS
        }
    }

    /** agy models：每行一个模型显示名 */
    private fun fetchAgyModels(overridePath: String?): List<String> {
        val status = detect(SdkDefinition.GEMINI_SDK, overridePath)
        val cliPath = status.path ?: return GEMINI_FALLBACK_MODELS
        return try {
            val result = CliProcess.run(listOf(cliPath, "models"), timeoutMillis = 30000)
            if (result.exitCode != 0) return GEMINI_FALLBACK_MODELS
            val output = result.stdout
            val models = output.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            models.ifEmpty { GEMINI_FALLBACK_MODELS }
        } catch (e: Exception) {
            LOG.warn("fetch agy models failed: ${e.message}")
            GEMINI_FALLBACK_MODELS
        }
    }

    private fun fetchOmpModels(overridePath: String?): List<String> {
        val status = detect(SdkDefinition.OMP_SDK, overridePath)
        val cliPath = status.path ?: return OMP_FALLBACK_MODELS
        return try {
            val result = CliProcess.run(listOf(cliPath, "models", "--json"), timeoutMillis = 30000)
            if (result.exitCode != 0) return OMP_FALLBACK_MODELS
            val output = result.stdout
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

    internal fun parsePiModelTable(output: String): List<String> {
        val models = linkedSetOf<String>()
        for (line in output.lineSequence()) {
            val columns = line.trim().split(Regex("\\s{2,}"))
            if (columns.size < 2) continue
            val provider = columns[0].trim()
            val model = columns[1].trim()
            if (provider.equals("provider", true) && model.equals("model", true)) continue
            if (!provider.matches(Regex("[A-Za-z0-9._-]+"))) continue
            if (model.isBlank() || model.contains(' ')) continue
            models.add("$provider/$model")
        }
        return models.toList()
    }

    private fun fetchPiModels(overridePath: String?): List<String> {
        val status = detect(SdkDefinition.PI_SDK, overridePath)
        val cliPath = status.path ?: return PI_FALLBACK_MODELS
        return try {
            val result = CliProcess.run(listOf(cliPath, "--list-models"), timeoutMillis = 30000)
            if (result.exitCode != 0) return PI_FALLBACK_MODELS
            parsePiModelTable(result.stdout).ifEmpty { PI_FALLBACK_MODELS }
        } catch (e: Exception) {
            LOG.warn("fetch Pi models failed: ${e.message}")
            PI_FALLBACK_MODELS
        }
    }
}
