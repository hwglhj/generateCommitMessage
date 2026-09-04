# SubMsg 开发文档

## 项目架构

### 核心组件

```
SubMsg
├── UI Layer (用户界面层)
│   ├── AiCommitConfigurable.kt      # 主设置界面
│   ├── CliModePanel.kt              # CLI 模式面板
│   ├── ApiModePanel.kt              # API 模式面板
│   └── ApiConfigDialog.kt           # API 配置对话框
│
├── Action Layer (动作层)
│   └── GenerateCommitMessageAction.kt  # 生成提交信息动作
│
├── Service Layer (服务层)
│   ├── AiCommitService.kt           # AI 提交服务（核心逻辑）
│   ├── SdkManager.kt                # CLI 检测和管理
│   └── ApiClient.kt                 # API 客户端
│
├── Configuration Layer (配置层)
│   ├── AiCommitSettings.kt          # 设置存储
│   ├── AiCommitPrompts.kt           # 提示词模板
│   └── ApiProviderConfig.kt         # API 配置模型
│
└── Plugin Metadata
    └── plugin.xml                    # 插件配置
```

### 数据流

```
用户点击按钮
    ↓
GenerateCommitMessageAction
    ↓
AiCommitService.generateCommitMessage()
    ↓
    ├─→ CLI 模式: SdkManager.executeCliCommand()
    │       ↓
    │   调用本地 CLI
    │
    └─→ API 模式: ApiClient.generateMessage()
            ↓
        调用远程 API
    ↓
返回生成的提交信息
```

## 核心类详解

### 1. AiCommitConfigurable

**职责**：主设置界面，管理 CLI 和 API 两种模式的切换。

**关键方法**：
- `createComponent()`: 创建 UI 组件
- `isModified()`: 检查设置是否被修改
- `apply()`: 应用设置更改
- `reset()`:重置为保存的设置

**UI 结构**：
```
┌─────────────────────────────────────┐
│ 使用模式: [CLI 模式 ▼]              │
├─────────────────────────────────────┤
│ ┌─ CLI 模式面板 ─────────────────┐ │
│ │ CLI: [Claude CLI ▼]            │ │
│ │ 模型: [claude-sonnet-4-6]      │ │
│ │ 思考等级: [跟随默认 ▼]         │ │
│ │ [刷新模型列表] [测试]          │ │
│ │ ✓ 已安装 2.1.197               │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ 附加提示词:                         │
│ ┌─────────────────────────────────┐ │
│ │ 自定义提示词...                 │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

### 2. CliModePanel

**职责**：CLI 模式配置界面。

**核心功能**：
- CLI 工具选择（Claude、Codex、Cursor 等）
- 模型名称输入
- 思考等级选择
- CLI 检测和状态显示
- 模型列表获取

**关键方法**：
```kotlin
fun updateStatus() {
    val sdk = getSelectedSdk()
    val status = SdkManager.detect(sdk, null)
    statusLabel.text = if (status.installed) {
        "✓ 已安装 ${status.version}"
    } else {
        "✗ 未检测到 ${sdk.binName}"
    }
}
```

### 3. ApiModePanel

**职责**：API 模式配置界面。

**核心功能**：
- API 配置列表管理（增删改查）
- 激活配置选择
- API 连接测试

**表格结构**：
```
┌──────┬──────────────┬─────────────────────────┬──────────┐
│ 名称 │ 提供商       │ Base URL                │ 模型     │
├──────┼──────────────┼─────────────────────────┼──────────┤
│ GPT4 │ OpenAI       │ https://api.openai...   │ gpt-4o   │
│ Claude│ Anthropic   │ https://api.anthropic...│ claude-3 │
└──────┴──────────────┴─────────────────────────┴──────────┘
[➕] [➖] [✏️]

激活配置: [GPT4 ▼]  [测试]
```

### 4. AiCommitService

**职责**：核心业务逻辑，生成提交信息。

**主要流程**：
```kotlin
fun generateCommitMessage(project: Project): String {
    // 1. 获取 Git diff
    val diff = getGitDiff(project)
    
    // 2. 构建提示词
    val prompt = buildPrompt(diff)
    
    // 3. 调用 AI（CLI 或 API）
    val message = if (settings.useMode == MODE_CLI) {
        generateWithCli(prompt)
    } else {
        generateWithApi(prompt)
    }
    
    // 4. 返回生成的提交信息
    return message
}
```

**提示词模板**：
```
System: You are a Git commit message generator...

User: Based on the following git diff, generate a commit message:

```diff
+ added code
- removed code
```

Format: <type>: <subject>
```

### 5. SdkManager

**职责**：检测和管理 CLI 工具。

**核心数据结构**：
```kotlin
enum class SdkDefinition(
    val id: String,              // claude, codex, cursor
    val displayName: String,     // Claude CLI
    val binName: String,         // claude, codex, cursor-agent
    val defaultModel: String,    // claude-sonnet-4-6
    val description: String,
    val thinkingLevels: List<String> = emptyList()
)
```

**检测逻辑**（Windows 优化）：
```kotlin
fun detect(sdk: SdkDefinition, overridePath: String?): SdkStatus {
    val paths = mutableListOf<Path>()
    
    // 1. 优先使用自定义路径
    if (!overridePath.isNullOrBlank()) {
        paths.add(Paths.get(overridePath))
    } else {
        // 2. 扫描常见安装位置
        paths.addAll(candidatePaths(sdk))
    }
    
    // 3. 检查每个路径
    for (p in paths) {
        // Windows: 支持 .exe, .cmd, .bat
        val exts = if (isWindows) listOf("", ".exe", ".cmd", ".bat") else listOf("")
        for (ext in exts) {
            val fullPath = p.toString() + ext
            if (Files.isExecutable(Paths.get(fullPath))) {
                val version = readVersion(fullPath)
                if (version != null) {
                    return SdkStatus(true, version, fullPath, "已安装")
                }
            }
        }
    }
    
    // 4. 最后尝试系统 PATH
    return detectFromPath(sdk)
}
```

**Windows 路径检测**：
```kotlin
private fun candidatePaths(sdk: SdkDefinition): List<Path> {
    val home = System.getProperty("user.home")
    val appData = System.getenv("APPDATA")
    val localAppData = System.getenv("LOCALAPPDATA")
    
    return listOf(
        // npm 全局
        Paths.get(appData, "npm", sdk.binName),
        // nvm-windows
        Paths.get(appData, "nvm"),
        // Program Files
        Paths.get("C:\\Program Files", sdk.binName),
        // 用户本地
        Paths.get(home, ".local", "bin", sdk.binName),
        // ... 更多路径
    )
}
```

### 6. ApiClient

**职责**：调用远程 API 生成提交信息。

**API 请求格式**（OpenAI 兼容）：
```kotlin
{
    "model": "gpt-4o",
    "messages": [
        {
            "role": "system",
            "content": "You are a Git commit message generator..."
        },
        {
            "role": "user",
            "content": "Based on the following git diff..."
        }
    ],
    "temperature": 0.7,
    "max_tokens": 500
}
```

**错误处理**：
```kotlin
fun generateMessage(config: ApiProviderConfig, prompt: String): String {
    try {
        val response = sendRequest(config, prompt)
        return parseResponse(response)
    } catch (e: IOException) {
        throw Exception("网络连接失败: ${e.message}")
    } catch (e: HttpException) {
        throw Exception("API 调用失败: ${e.statusCode} ${e.message}")
    }
}
```

## 配置存储

### AiCommitSettings

使用 IntelliJ Platform 的持久化存储：

```kotlin
@State(
    name = "AiCommitSettings",
    storages = [Storage("AiCommitSettings.xml")]
)
class AiCommitSettings : PersistentStateComponent<AiCommitSettings> {
    var useMode: String = MODE_CLI
    var activeProvider: String = "claude"
    var model: String = "claude-sonnet-4-6"
    var customPrompt: String = ""
    var apiConfigs: String = "[]"  // JSON
    var activeApiConfig: String = ""
    
    companion object {
        const val MODE_CLI = "cli"
        const val MODE_API = "api"
        
        fun getInstance(): AiCommitSettings {
            return ServiceManager.getService(AiCommitSettings::class.java)
        }
    }
}
```

**存储位置**：
- Windows: `%APPDATA%\JetBrains\<IDE>\options\AiCommitSettings.xml`
- macOS: `~/Library/Application Support/JetBrains/<IDE>/options/AiCommitSettings.xml`
- Linux: `~/.config/JetBrains/<IDE>/options/AiCommitSettings.xml`

## 关键技术点

### 1. Git Diff 获取

```kotlin
fun getGitDiff(project: Project): String {
    val changeListManager = ChangeListManagerImpl.getInstance(project)
    val changes = changeListManager.defaultChangeList.changes
    
    val diff = StringBuilder()
    for (change in changes) {
        val diffProvider = change.beforeRevision?.file?.let { file ->
            VcsUtil.getVcsFor(project, file)?.diffProvider
        }
        if (diffProvider != null) {
            val content = diffProvider.createFileContent(...)
            diff.append(content)
        }
    }
    return diff.toString()
}
```

### 2. 进程执行（Windows 兼容）

```kotlin
fun executeCommand(command: List<String>): String {
    val processBuilder = ProcessBuilder(command)
    
    // Windows: 确保 PATH 包含所有路径
    if (isWindows) {
        val env = processBuilder.environment()
        env["PATH"] = buildWindowsPath()
    }
    
    val process = processBuilder.start()
    
    // 立即关闭 stdin（某些 CLI 需要）
    process.outputStream.close()
    
    val output = process.inputStream.bufferedReader().readText()
    
    if (!process.waitFor(60, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        throw TimeoutException("命令执行超时")
    }
    
    return output
}
```

### 3. HTTP 请求

```kotlin
fun sendHttpRequest(url: String, headers: Map<String, String>, body: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 60000
        
        // 设置 headers
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        
        // 发送请求体
        connection.outputStream.use { it.write(body.toByteArray()) }
        
        // 读取响应
        val responseCode = connection.responseCode
        if (responseCode == 200) {
            return connection.inputStream.bufferedReader().readText()
        } else {
            val error = connection.errorStream?.bufferedReader()?.readText() ?: ""
            throw HttpException(responseCode, error)
        }
    } finally {
        connection.disconnect()
    }
}
```

## 调试技巧

### 1. 启用插件日志

```kotlin
import com.intellij.openapi.diagnostic.Logger

class MyClass {
    private val LOG = Logger.getInstance(MyClass::class.java)
    
    fun myMethod() {
        LOG.info("Info message")
        LOG.debug("Debug message")
        LOG.warn("Warning message")
        LOG.error("Error message", exception)
    }
}
```

查看日志：`Help` → `Show Log in Explorer`

### 2. 沙箱调试

```bash
# 启动调试实例
./gradlew runIde

# 查看沙箱位置
build/idea-sandbox/
```

### 3. 插件 DevKit

在 IDEA 中安装 Plugin DevKit，可以：
- 查看插件扩展点
- 调试插件代码
- 查看插件结构

## 常见问题

### Q: 如何添加新的 CLI 支持？

A: 在 `SdkDefinition` 枚举中添加：

```kotlin
NEW_CLI(
    "newcli",                    // ID
    "New CLI",                   // 显示名称
    "newcli",                    // 可执行文件名
    "default-model",             // 默认模型
    "使用 New CLI 的登录凭证",    // 描述
    listOf("low", "high")        // 思考等级（可选）
)
```

### Q: 如何修改提示词模板？

A: 编辑 `AiCommitPrompts.kt`：

```kotlin
object AiCommitPrompts {
    const val SYSTEM_PROMPT = """
        You are a Git commit message generator.
        Follow these rules:
        1. Use conventional commits format
        2. Be concise and clear
        3. Explain what and why, not how
    """.trimIndent()
}
```

### Q: 如何支持新的 API 提供商？

A: `ApiClient` 支持任何 OpenAI 兼容 API。如果是特殊格式：

```kotlin
fun buildRequest(config: ApiProviderConfig, prompt: String): String {
    return when (config.provider) {
        "OpenAI Compatible" -> buildOpenAIRequest(prompt)
        "Anthropic" -> buildAnthropicRequest(prompt)
        "Custom Provider" -> buildCustomRequest(prompt)
        else -> buildOpenAIRequest(prompt)
    }
}
```

## 性能优化

1. **懒加载**：只在需要时创建 UI 组件
2. **缓存**：缓存 CLI 检测结果
3. **异步执行**：使用后台线程调用 API
4. **超时控制**：设置合理的超时时间

## 安全考虑

1. **API Key 存储**：使用 `PasswordSafe` 加密存储
2. **命令注入**：验证和转义用户输入
3. **网络请求**：使用 HTTPS，验证证书

## 发布清单

- [ ] 更新版本号（`build.gradle.kts`, `plugin.xml`）
- [ ] 更新 CHANGELOG.md
- [ ] 测试所有功能
- [ ] 测试多个 IDEA 版本
- [ ] 测试多个操作系统
- [ ] 构建插件：`./gradlew buildPlugin`
- [ ] 验证插件：`./gradlew verifyPlugin`
- [ ] 创建 Git tag：`git tag v1.0.0`
- [ ] 推送 tag：`git push origin v1.0.0`
- [ ] GitHub Actions 自动发布
- [ ] 发布到 JetBrains Marketplace（可选）

---

更多信息请参考 [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/)
