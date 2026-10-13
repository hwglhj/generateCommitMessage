# Pi CLI Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Add official Pi Coding Agent support to AI Commit model selection, commit generation, Windows CLI detection, and the IDE CLI tool window.

**Architecture:** Extend the existing `SdkDefinition` and `CliTool` enums so Pi follows the same provider and terminal flows as the existing CLIs. Keep Pi-specific model parsing and command construction as internal pure functions so their behavior can be tested without installing Pi or starting external processes.

**Tech Stack:** Kotlin 1.8, IntelliJ Platform SDK 2022.2, Swing, JUnit 4, Gradle IntelliJ Plugin, SVG resources.

**Repository constraint:** Do not run `git add`, `git commit`, `git push`, or `git merge`. Review changes with `git status` and `git diff` only.

---

### Task 1: Pi SDK definition, Windows detection, and model discovery

**Files:**
- Modify: `src/test/kotlin/com/fs/aicommit/SdkManagerTest.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/SdkManager.kt`

- [x] **Step 1: Write failing SDK and parser tests**

Add tests that require a Pi definition, the official Windows managed-install directory, and table parsing:

```kotlin
@Test
fun `pi sdk uses its own default model and thinking levels`() {
    val sdk = SdkDefinition.fromProvider("pi")
    assertEquals(SdkDefinition.PI_SDK, sdk)
    assertEquals("pi", sdk.binName)
    assertEquals("", sdk.defaultModel)
    assertEquals(listOf("off", "minimal", "low", "medium", "high", "xhigh", "max"), sdk.thinkingLevels)
}

@Test
fun `windows installation directories include pi managed launcher`() {
    val dirs = SdkManager.installationDirectories("Windows 11", "C:\\Users\\test", emptyMap())
    assertTrue(dirs.contains("C:\\Users\\test\\.pi\\agent\\bin"))
}

@Test
fun `pi model table is parsed into provider model selectors`() {
    val output = """
        provider   model                       context  max-out  thinking  images
        anthropic  claude-sonnet-4-6          200K     64K      yes       yes
        openai     gpt-5.5                    400K     128K     yes       yes
        anthropic  claude-sonnet-4-6          200K     64K      yes       yes
    """.trimIndent()
    assertEquals(
        listOf("anthropic/claude-sonnet-4-6", "openai/gpt-5.5"),
        SdkManager.parsePiModelTable(output)
    )
}

@Test
fun `pi model parser ignores messages and malformed rows`() {
    assertTrue(SdkManager.parsePiModelTable("No models available. Run /login first.").isEmpty())
    assertTrue(SdkManager.parsePiModelTable("provider  model  context").isEmpty())
}
```

- [x] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests com.fs.aicommit.SdkManagerTest`

Expected: compilation fails because `PI_SDK` and `parsePiModelTable` do not exist, proving the tests cover missing Pi behavior.

- [x] **Step 3: Add the Pi SDK definition and paths**

Add to `SdkDefinition` before the terminating semicolon:

```kotlin
PI_SDK(
    "pi", "Pi CLI", "pi", "",
    "使用 Pi Coding Agent 已配置的模型提供方与认证，模型留空走 Pi 自身默认",
    listOf("off", "minimal", "low", "medium", "high", "xhigh", "max")
)
```

Add `PI_FALLBACK_MODELS = emptyList<String>()`, return it from `fallbackModels`, and add `.pi/agent/bin` to the common installation directories:

```kotlin
for (relative in listOf(
    ".local/bin", ".cursor/bin", ".opencode/bin", ".grok/bin", ".claude/local",
    ".omp/bin", ".pi/agent/bin", ".npm-global/bin", ".volta/bin", ".bun/bin", ".codex/bin"
)) {
    directories.add(File(home, relative).path)
}
```

- [x] **Step 4: Implement Pi model discovery and table parsing**

Route Pi before OpenRouter handling:

```kotlin
if (sdk == SdkDefinition.PI_SDK) return fetchPiModels(overridePath)
```

Add the pure parser and command-backed fetcher:

```kotlin
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
```

- [x] **Step 5: Run the focused test and verify GREEN**

Run: `.\gradlew.bat test --tests com.fs.aicommit.SdkManagerTest`

Expected: all `SdkManagerTest` tests pass.

### Task 2: Pi commit-generation command

**Files:**
- Modify: `src/test/kotlin/com/fs/aicommit/AiCommitServiceTest.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitService.kt`

- [x] **Step 1: Write failing command-construction tests**

Add:

```kotlin
@Test
fun `pi command is one shot stateless and tool free`() {
    assertEquals(
        listOf(
            "C:/tools/pi.cmd", "--print", "--no-session", "--no-tools",
            "--no-context-files", "--no-approve", "Summarize"
        ),
        AiCommitService.buildPiCommand("C:/tools/pi.cmd", "Summarize", "", "")
    )
}

@Test
fun `pi command includes selected model and thinking level`() {
    assertEquals(
        listOf(
            "pi", "--print", "--no-session", "--no-tools", "--no-context-files", "--no-approve",
            "--model", "anthropic/claude-sonnet-4-6", "--thinking", "high", "Prompt"
        ),
        AiCommitService.buildPiCommand("pi", "Prompt", "anthropic/claude-sonnet-4-6", "high")
    )
}
```

- [x] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests com.fs.aicommit.AiCommitServiceTest`

Expected: compilation fails because `buildPiCommand` does not exist.

- [x] **Step 3: Implement Pi command construction and execution**

Add `SdkDefinition.PI_SDK -> runPi(cliPath, prompt, model, thinking, indicator)` to `runSdk`.

Add:

```kotlin
private fun runPi(
    cliPath: String,
    prompt: String,
    model: String,
    thinking: String,
    indicator: ProgressIndicator?
): String = execute(buildPiCommand(cliPath, prompt, model, thinking), stdin = null, indicator = indicator)
```

Add to the companion object:

```kotlin
internal fun buildPiCommand(cliPath: String, prompt: String, model: String, thinking: String): List<String> {
    val command = mutableListOf(
        cliPath, "--print", "--no-session", "--no-tools", "--no-context-files", "--no-approve"
    )
    if (model.isNotBlank()) command.addAll(listOf("--model", model))
    if (thinking.isNotBlank()) command.addAll(listOf("--thinking", thinking))
    command.add(prompt)
    return command
}
```

- [x] **Step 4: Run the focused test and verify GREEN**

Run: `.\gradlew.bat test --tests com.fs.aicommit.AiCommitServiceTest`

Expected: all `AiCommitServiceTest` tests pass.

### Task 3: Pi provider selection and localized UI text

**Files:**
- Modify: `src/test/kotlin/com/fs/aicommit/AiCommitSettingsTest.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitSettings.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/ProviderModelPanel.kt`
- Modify: `src/main/resources/messages/SubMsgBundle.properties`
- Modify: `src/main/resources/messages/SubMsgBundle_zh.properties`

- [x] **Step 1: Write the failing provider persistence test**

Add:

```kotlin
@Test
fun `pi provider survives state loading`() {
    val source = AiCommitSettings().apply {
        activeProvider = AiCommitSettings.PROVIDER_PI
        model = "openai/gpt-5.5 high"
    }
    val target = AiCommitSettings()
    target.loadState(source)
    assertEquals(AiCommitSettings.PROVIDER_PI, target.activeProvider)
    assertEquals(SdkDefinition.PI_SDK, target.activeSdk())
    assertEquals("openai/gpt-5.5 high", target.model)
}
```

- [x] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests com.fs.aicommit.AiCommitSettingsTest`

Expected: compilation fails because `PROVIDER_PI` does not exist.

- [x] **Step 3: Add Pi to settings and provider selector**

Add `const val PROVIDER_PI = "pi"` to `AiCommitSettings` and this entry to `ProviderModelPanel.PROVIDERS`:

```kotlin
AiCommitSettings.PROVIDER_PI to "Pi"
```

Update comments listing supported CLIs so Pi appears separately from OMP.

- [x] **Step 4: Add matching resource keys and hints**

Add `cli.pi=Pi CLI` to both resource bundles. Update `aicommit.hint` to state that Pi and OMP use their configured providers and credentials. Keep the English and Chinese key sets identical.

- [x] **Step 5: Run settings and resource tests**

Run: `.\gradlew.bat test --tests com.fs.aicommit.AiCommitSettingsTest --tests com.fs.i18n.SubMsgBundleTest`

Expected: both test classes pass.

### Task 4: Pi terminal tool-window integration

**Files:**
- Create: `src/test/kotlin/com/fs/terminal/CliToolTest.kt`
- Modify: `src/main/kotlin/com/fs/terminal/CliTool.kt`
- Modify: `src/main/kotlin/com/fs/terminal/CliToolsSettings.kt`
- Modify: `src/main/kotlin/com/fs/terminal/CliToolsPanel.kt`
- Modify: `src/main/kotlin/com/fs/terminal/CliToolWindowFactory.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Create: `src/main/resources/icons/pi.svg`

- [x] **Step 1: Write failing terminal metadata tests**

Create:

```kotlin
package com.fs.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CliToolTest {
    @Test
    fun `pi terminal uses plain pi command without fake permission mode`() {
        assertEquals("Pi", CliTool.PI.id)
        assertEquals("pi", CliTool.PI.command(false))
        assertEquals("pi", CliTool.PI.command(true))
        assertFalse(CliTool.PI.supportsHighPermission)
    }

    @Test
    fun `existing cli permission commands remain available`() {
        assertTrue(CliTool.CODEX.supportsHighPermission)
        assertTrue(CliTool.CODEX.command(true).contains("danger-full-access"))
    }

    @Test
    fun `pi icon is visible by default`() {
        val settings = CliToolsSettings()
        assertTrue(settings.isShowIcon(CliTool.PI))
        assertFalse(settings.isHighPermission(CliTool.PI))
    }
}
```

- [x] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat test --tests com.fs.terminal.CliToolTest`

Expected: compilation fails because `CliTool.PI` and `supportsHighPermission` do not exist.

- [x] **Step 3: Extend terminal metadata and persisted icon state**

Extend `CliTool` with `val supportsHighPermission: Boolean = true`, then add:

```kotlin
PI(
    "Pi", "pi", "pi",
    "Pi 使用当前进程权限启动",
    supportsHighPermission = false
)
```

Change command selection to:

```kotlin
fun command(highPermission: Boolean): String =
    if (supportsHighPermission && highPermission) highPermissionCommand else baseCommand
```

Add `showPiIcon: Boolean = true` to settings. Map `CliTool.PI` to `false` for high permission, ignore attempts to set it, and map icon get/set to `showPiIcon`.

- [x] **Step 4: Hide unsupported permission controls and register the factory**

Filter the permission loop with `CliTool.values().filter { it.supportsHighPermission }`. Add `class PiToolWindowFactory : CliToolWindowFactoryBase(CliTool.PI)` and register:

```xml
<toolWindow id="Pi" anchor="right" secondary="false"
            icon="/icons/pi.svg"
            factoryClass="com.fs.terminal.PiToolWindowFactory"/>
```

- [x] **Step 5: Add the official Pi mark adapted to a 13×13 IDE icon**

Create:

```xml
<svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 800 800">
  <path fill="#F09082" d="M165.29 165.29H517.36V400H400V282.65H165.29Z"/>
  <path fill="#4D9ABF" d="M165.29 282.65H282.65V400H400V517.36H282.65V634.72H165.29Z"/>
  <path fill="#F1BE58" d="M517.36 400H634.72V634.72H517.36Z"/>
</svg>
```

- [x] **Step 6: Run terminal tests and compile plugin sources**

Run: `.\gradlew.bat test --tests com.fs.terminal.CliToolTest compileKotlin`

Expected: `CliToolTest` passes and Kotlin compilation succeeds.

### Task 5: User-facing documentation

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml`
- Modify: `README.md`
- Modify: `README_CN.md`
- Modify: `FEATURE_SUMMARY.md`
- Modify: `FINAL_SUMMARY.md`

- [x] **Step 1: Update every supported-CLI list**

Add `Pi` to each current supported-CLI list and state explicitly:

```text
Pi (`pi`) and OMP (`omp`) are separate CLI tools and reuse their own configured providers and credentials.
```

Chinese equivalent:

```text
Pi（`pi`）与 OMP（`omp`）是两个独立 CLI，分别复用各自配置的模型提供方和凭证。
```

- [x] **Step 2: Verify no primary CLI list omits Pi**

Run: `rg -n "Claude.*Codex.*Cursor|OMP|Pi" README.md README_CN.md FEATURE_SUMMARY.md FINAL_SUMMARY.md src/main/resources/META-INF/plugin.xml`

Expected: each current supported-CLI summary includes Pi, and OMP-specific text is not incorrectly renamed.

### Task 6: Full verification and artifact inspection

**Files:**
- Review all files changed by Tasks 1–5

- [x] **Step 1: Run the complete test suite**

Run: `.\gradlew.bat test`

Expected: all tests pass with no failed tests.

- [x] **Step 2: Build the installable plugin**

Run: `.\gradlew.bat buildPlugin`

Expected: `BUILD SUCCESSFUL` and a ZIP under `build/distributions/`.

- [x] **Step 3: Inspect the distribution and checksum**

Run:

```powershell
$zip = Get-ChildItem build/distributions/*.zip | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$hash = Get-FileHash -Algorithm SHA256 $zip.FullName
[pscustomobject]@{ Path=$zip.FullName; Size=$zip.Length; SHA256=$hash.Hash } | Format-List
```

Expected: the artifact exists, has nonzero size, and produces a SHA-256 hash.

- [x] **Step 4: Review the final diff without staging it**

Run:

```powershell
git status --short
git diff -- src/main src/test README.md README_CN.md FEATURE_SUMMARY.md FINAL_SUMMARY.md docs/superpowers
```

Expected: only intended Pi integration changes appear alongside the repository's pre-existing uncommitted work. Do not stage or commit them.
