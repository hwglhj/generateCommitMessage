# Generate Commit Message - 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 cli-tools-idea/SubMsg 项目重构为 generateCommitMessage，统一配置界面为两个标签页（AI Commit + CLI Tools），移除 API Search 功能，增强 AI Commit 配置选项。

**Architecture:** 创建统一的 GenerateCommitMessageConfigurable 作为配置入口，使用 JTabbedPane 组织两个子面板。AI Commit 面板复用 ProviderModelPanel 并添加增强选项；CLI Tools 面板提取现有逻辑。扩展 AiCommitSettings 支持新配置项。

**Tech Stack:** Kotlin 1.8.21, IntelliJ Platform SDK 2022.2+, Swing UI

---

## 文件结构概览

### 需要修改的文件
- `src/main/resources/META-INF/plugin.xml` - 更新插件元信息，移除 API Search
- `build.gradle.kts` - 更新项目名称和版本
- `src/main/kotlin/com/fs/aicommit/AiCommitSettings.kt` - 添加新配置字段
- `src/main/kotlin/com/fs/aicommit/GenerateCommitMessageAction.kt` - 使用新配置生成 commit message

### 需要创建的文件
- `src/main/kotlin/com/fs/aicommit/GenerateCommitMessageConfigurable.kt` - 统一配置入口
- `src/main/kotlin/com/fs/aicommit/AiCommitPanel.kt` - AI Commit 标签页面板
- `src/main/kotlin/com/fs/terminal/CliToolsPanel.kt` - CLI Tools 标签页面板（提取逻辑）

### 需要删除的文件
- `src/main/kotlin/com/fs/ApiSearchSettings.kt`
- `src/main/kotlin/com/fs/ApiSearchConfigurable.kt`
- 任何 AI Inline 相关文件（如果存在）

---

## Task 1: 项目重命名和清理

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml`
- Modify: `build.gradle.kts`
- Delete: `src/main/kotlin/com/fs/ApiSearchSettings.kt`
- Delete: `src/main/kotlin/com/fs/ApiSearchConfigurable.kt`

- [ ] **Step 1: 更新 plugin.xml - 基本信息**

```xml
<idea-plugin>
    <id>com.fs.generateCommitMessage</id>
    <name>Generate Commit Message</name>
    <vendor email="support@example.com" url="https://github.com/hwglhj/generateCommitMessage">FS</vendor>

    <description><![CDATA[
    AI-powered Git commit message generator with CLI tools integration.
    
    Features:
    - Generate high-quality commit messages using AI (Claude, Codex, Cursor, etc.)
    - Dual mode: CLI mode and API mode
    - Customizable prompts and output language
    - Integrated CLI terminal tools sidebar
    - External editor jump support
    ]]></description>
```

- [ ] **Step 2: 更新 plugin.xml - 移除 API Search 服务**

找到以下行并删除：
```xml
<applicationService serviceImplementation="com.fs.ApiSearchSettings"/>
```

找到以下行并删除（如果存在）：
```xml
<applicationConfigurable instance="com.fs.ApiSearchConfigurable"/>
```

- [ ] **Step 3: 更新 plugin.xml - 移除旧的 AI Commit 配置入口**

找到并删除：
```xml
<applicationConfigurable 
    parentId="tools" 
    instance="com.fs.aicommit.AiCommitConfigurable"
    id="com.fs.aicommit.AiCommitConfigurable"
    displayName="AI Commit 设置"/>
```

- [ ] **Step 4: 更新 plugin.xml - 添加新的统一配置入口**

在 `<extensions>` 部分添加：
```xml
<applicationConfigurable 
    parentId="tools" 
    instance="com.fs.aicommit.GenerateCommitMessageConfigurable"
    id="com.fs.aicommit.GenerateCommitMessageConfigurable"
    displayName="Generate Commit Message"/>
```

- [ ] **Step 5: 更新 build.gradle.kts**

```kotlin
group = "com.fs"
version = "1.0.0"
```

找到 `intellij` 配置块，保持不变（version, type, plugins）。

- [ ] **Step 6: 删除 ApiSearchSettings.kt**

运行：
```bash
rm src/main/kotlin/com/fs/ApiSearchSettings.kt
```

- [ ] **Step 7: 删除 ApiSearchConfigurable.kt**

运行：
```bash
rm src/main/kotlin/com/fs/ApiSearchConfigurable.kt
```

- [ ] **Step 8: 验证编译**

运行：
```bash
./gradlew clean build -x test
```

预期：编译可能失败（因为有地方引用了 ApiSearchSettings），这是正常的，后续会修复。

- [ ] **Step 9: Commit Task 1**

```bash
git add src/main/resources/META-INF/plugin.xml build.gradle.kts
git commit -m "chore: 重命名项目为 generateCommitMessage 并移除 API Search"
```

---

## Task 2: 扩展 AiCommitSettings 数据模型

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitSettings.kt:26-30`

- [ ] **Step 1: 添加新配置字段**

在 `AiCommitSettings` 类中，找到现有字段后添加：

```kotlin
@State(
    name = "AiCommitSettings",
    storages = [Storage("ai-commit.xml")]
)
class AiCommitSettings : PersistentStateComponent<AiCommitSettings> {
    var activeProvider: String = PROVIDER_CLAUDE
    var model: String = ""
    var customPrompt: String = ""
    
    // 新增：增强选项
    var includeRecentCommits: Boolean = true
    var maxDiffLength: Int = 5000
    var outputLanguage: String = "zh" // "zh" 或 "en"
```

- [ ] **Step 2: 验证数据持久化**

检查 `getState()` 和 `loadState()` 方法是否存在且正确：

```kotlin
override fun getState(): AiCommitSettings = this

override fun loadState(state: AiCommitSettings) {
    XmlSerializerUtil.copyBean(state, this)
}
```

- [ ] **Step 3: 编译验证**

运行：
```bash
./gradlew compileKotlin
```

预期：SUCCESS

- [ ] **Step 4: Commit Task 2**

```bash
git add src/main/kotlin/com/fs/aicommit/AiCommitSettings.kt
git commit -m "feat(settings): 添加增强配置选项字段"
```

---

## Task 3: 创建 CliToolsPanel 组件

**Files:**
- Create: `src/main/kotlin/com/fs/terminal/CliToolsPanel.kt`
- Reference: `src/main/kotlin/com/fs/terminal/CliToolsConfigurable.kt`

- [ ] **Step 1: 创建 CliToolsPanel 类框架**

```kotlin
package com.fs.terminal

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

/**
 * CLI Tools 配置面板（从 CliToolsConfigurable 提取）
 * 用于在标签页中显示
 */
class CliToolsPanel {
    
    val panel: JPanel
    private val showIconBoxes = linkedMapOf<CliTool, JBCheckBox>()
    private val highPermissionBoxes = linkedMapOf<CliTool, JBCheckBox>()
    private val settings = CliToolsSettings.getInstance()
    
    init {
        panel = createPanel()
    }
    
    private fun createPanel(): JPanel {
        // 实现将在下一步
        return JPanel()
    }
    
    fun isModified(): Boolean {
        return false // 实现将在下一步
    }
    
    fun apply() {
        // 实现将在下一步
    }
    
    fun reset() {
        // 实现将在下一步
    }
}
```

- [ ] **Step 2: 实现 createPanel 方法**

```kotlin
private fun createPanel(): JPanel {
    val inner = JPanel()
    inner.layout = BoxLayout(inner, BoxLayout.Y_AXIS)

    inner.add(JBLabel("侧边栏图标显示:"))
    for (tool in CliTool.values()) {
        val cb = JBCheckBox("显示 ${tool.id} 图标", settings.isShowIcon(tool))
        showIconBoxes[tool] = cb
        inner.add(cb)
    }

    inner.add(JBLabel("高权限启动:"))
    for (tool in CliTool.values()) {
        val cb = JBCheckBox(tool.highPermissionDesc, settings.isHighPermission(tool))
        highPermissionBoxes[tool] = cb
        inner.add(cb)
    }

    val outerPanel = JPanel(BorderLayout())
    outerPanel.border = EmptyBorder(JBUI.insets(10))
    outerPanel.add(inner, BorderLayout.NORTH)
    return outerPanel
}
```

- [ ] **Step 3: 实现 isModified 方法**

```kotlin
fun isModified(): Boolean {
    return showIconBoxes.any { (tool, cb) -> 
        cb.isSelected != settings.isShowIcon(tool) 
    } || highPermissionBoxes.any { (tool, cb) -> 
        cb.isSelected != settings.isHighPermission(tool) 
    }
}
```

- [ ] **Step 4: 实现 apply 方法**

```kotlin
fun apply() {
    showIconBoxes.forEach { (tool, cb) -> 
        settings.setShowIcon(tool, cb.isSelected) 
    }
    highPermissionBoxes.forEach { (tool, cb) -> 
        settings.setHighPermission(tool, cb.isSelected) 
    }
    
    // 图标显隐即时应用到所有已打开项目
    for (project in ProjectManager.getInstance().openProjects) {
        val manager = ToolWindowManager.getInstance(project)
        for (tool in CliTool.values()) {
            manager.getToolWindow(tool.id)?.setAvailable(
                settings.isShowIcon(tool), 
                null
            )
        }
    }
}
```

- [ ] **Step 5: 实现 reset 方法**

```kotlin
fun reset() {
    showIconBoxes.forEach { (tool, cb) -> 
        cb.isSelected = settings.isShowIcon(tool) 
    }
    highPermissionBoxes.forEach { (tool, cb) -> 
        cb.isSelected = settings.isHighPermission(tool) 
    }
}
```

- [ ] **Step 6: 编译验证**

运行：
```bash
./gradlew compileKotlin
```

预期：SUCCESS

- [ ] **Step 7: Commit Task 3**

```bash
git add src/main/kotlin/com/fs/terminal/CliToolsPanel.kt
git commit -m "feat(ui): 创建 CliToolsPanel 组件"
```

---

## Task 4: 创建 AiCommitPanel 组件

**Files:**
- Create: `src/main/kotlin/com/fs/aicommit/AiCommitPanel.kt`

- [ ] **Step 1: 创建 AiCommitPanel 类框架**

```kotlin
package com.fs.aicommit

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * AI Commit 配置面板
 */
class AiCommitPanel {
    
    val panel: JPanel
    private val settings = AiCommitSettings.getInstance()
    
    // CLI 与模型配置
    private val providerModelPanel = ProviderModelPanel("CLI 与模型")
    
    // 附加提示词
    private val promptArea = JBTextArea(6, 60)
    
    // 增强选项
    private val includeRecentCommitsCheckBox = JBCheckBox(
        "包含近期提交风格参考 (最近 10 条)", 
        true
    )
    private val maxDiffLengthField = JBTextField("5000", 10)
    private val outputLanguageGroup = ButtonGroup()
    private val outputLanguageZh = JRadioButton("中文", true)
    private val outputLanguageEn = JRadioButton("英文", false)
    
    init {
        outputLanguageGroup.add(outputLanguageZh)
        outputLanguageGroup.add(outputLanguageEn)
        panel = createPanel()
    }
    
    private fun createPanel(): JPanel {
        // 实现将在下一步
        return JPanel()
    }
    
    fun isModified(): Boolean = false // 实现将在后续步骤
    fun apply() {} // 实现将在后续步骤
    fun reset() {} // 实现将在后续步骤
    
    // Getter 方法
    fun getSelectedProvider(): String = ""
    fun getComposedModel(): String = ""
    fun getCustomPrompt(): String = ""
    fun getIncludeRecentCommits(): Boolean = false
    fun getMaxDiffLength(): Int = 0
    fun getOutputLanguage(): String = ""
}
```

- [ ] **Step 2: 实现 createPanel - 附加提示词区域**

```kotlin
private fun createPanel(): JPanel {
    // 1. 附加提示词区域
    promptArea.lineWrap = true
    promptArea.wrapStyleWord = true
    
    val viewBuiltinButton = JButton("查看内置提示词").apply {
        addActionListener { showBuiltinPrompt() }
    }
    val clearPromptButton = JButton("清空附加提示词").apply {
        addActionListener { promptArea.text = "" }
    }
    
    val promptButtons = JPanel(BorderLayout(10, 0)).apply {
        add(viewBuiltinButton, BorderLayout.WEST)
        add(clearPromptButton, BorderLayout.EAST)
    }
    
    val promptPanel = JPanel(BorderLayout(0, 6)).apply {
        add(
            FormBuilder.createFormBuilder()
                .addComponent(JBLabel("附加提示词（叠加在内置提示词之后）:"))
                .addComponent(JBScrollPane(promptArea))
                .addComponent(promptButtons)
                .panel,
            BorderLayout.CENTER
        )
    }
    
    // 继续下一步...
    return JPanel()
}
```

- [ ] **Step 3: 实现 createPanel - 增强选项区域**

在 `createPanel` 方法中继续添加：

```kotlin
    // 2. 增强选项区域
    val languagePanel = JPanel(BorderLayout(8, 0)).apply {
        add(outputLanguageZh, BorderLayout.WEST)
        add(outputLanguageEn, BorderLayout.EAST)
    }
    
    val enhancedOptionsPanel = FormBuilder.createFormBuilder()
        .addComponent(includeRecentCommitsCheckBox)
        .addVerticalGap(6)
        .addLabeledComponent(
            JBLabel("Diff 最大长度:"),
            JPanel(BorderLayout(6, 0)).apply {
                add(maxDiffLengthField, BorderLayout.WEST)
                add(JBLabel("字符 (建议范围: 1000-10000)"), BorderLayout.CENTER)
            }
        )
        .addVerticalGap(6)
        .addLabeledComponent(JBLabel("输出语言:"), languagePanel)
        .panel
    
    enhancedOptionsPanel.border = javax.swing.border.TitledBorder("增强选项")
```

- [ ] **Step 4: 实现 createPanel - 组装最终面板**

在 `createPanel` 方法中完成：

```kotlin
    // 3. 组装最终面板
    val content = FormBuilder.createFormBuilder()
        .addComponent(providerModelPanel.panel)
        .addVerticalGap(10)
        .addComponent(promptPanel)
        .addVerticalGap(10)
        .addComponent(enhancedOptionsPanel)
        .panel
    
    val mainPanel = JPanel(BorderLayout())
    mainPanel.border = EmptyBorder(JBUI.insets(10))
    mainPanel.add(content, BorderLayout.NORTH)
    
    return mainPanel
}
```

- [ ] **Step 5: 实现 showBuiltinPrompt 方法**

```kotlin
private fun showBuiltinPrompt() {
    val area = JBTextArea(AiCommitPrompts.BUILTIN_COMMIT_PROMPT).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    val scroll = JBScrollPane(area).apply {
        preferredSize = Dimension(JBUI.scale(640), JBUI.scale(480))
    }
    val dialog = object : DialogWrapper(true) {
        init {
            title = "内置提示词（只读）"
            init()
        }
        override fun createCenterPanel(): JComponent = scroll
    }
    dialog.show()
}
```

- [ ] **Step 6: 实现 isModified 方法**

```kotlin
fun isModified(): Boolean {
    if (providerModelPanel.isModified(settings.activeProvider, settings.model)) {
        return true
    }
    if (promptArea.text != settings.customPrompt) return true
    if (includeRecentCommitsCheckBox.isSelected != settings.includeRecentCommits) {
        return true
    }
    if (maxDiffLengthField.text.toIntOrNull() != settings.maxDiffLength) {
        return true
    }
    val currentLang = if (outputLanguageZh.isSelected) "zh" else "en"
    if (currentLang != settings.outputLanguage) return true
    
    return false
}
```

- [ ] **Step 7: 实现 apply 方法**

```kotlin
fun apply() {
    settings.activeProvider = providerModelPanel.selectedProvider()
    settings.model = providerModelPanel.composedModel()
    settings.customPrompt = promptArea.text
    settings.includeRecentCommits = includeRecentCommitsCheckBox.isSelected
    settings.maxDiffLength = maxDiffLengthField.text.toIntOrNull() ?: 5000
    settings.outputLanguage = if (outputLanguageZh.isSelected) "zh" else "en"
}
```

- [ ] **Step 8: 实现 reset 方法**

```kotlin
fun reset() {
    providerModelPanel.reset(settings.activeProvider, settings.model)
    promptArea.text = settings.customPrompt
    includeRecentCommitsCheckBox.isSelected = settings.includeRecentCommits
    maxDiffLengthField.text = settings.maxDiffLength.toString()
    
    if (settings.outputLanguage == "zh") {
        outputLanguageZh.isSelected = true
    } else {
        outputLanguageEn.isSelected = true
    }
}
```

- [ ] **Step 9: 实现 Getter 方法**

```kotlin
fun getSelectedProvider(): String = providerModelPanel.selectedProvider()

fun getComposedModel(): String = providerModelPanel.composedModel()

fun getCustomPrompt(): String = promptArea.text

fun getIncludeRecentCommits(): Boolean = includeRecentCommitsCheckBox.isSelected

fun getMaxDiffLength(): Int = maxDiffLengthField.text.toIntOrNull() ?: 5000

fun getOutputLanguage(): String = if (outputLanguageZh.isSelected) "zh" else "en"
```

- [ ] **Step 10: 编译验证**

运行：
```bash
./gradlew compileKotlin
```

预期：SUCCESS

- [ ] **Step 11: Commit Task 4**

```bash
git add src/main/kotlin/com/fs/aicommit/AiCommitPanel.kt
git commit -m "feat(ui): 创建 AiCommitPanel 组件"
```

---

## Task 5: 创建 GenerateCommitMessageConfigurable 统一配置入口

**Files:**
- Create: `src/main/kotlin/com/fs/aicommit/GenerateCommitMessageConfigurable.kt`

- [ ] **Step 1: 创建类框架**

```kotlin
package com.fs.aicommit

import com.fs.terminal.CliToolsPanel
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import javax.swing.JTabbedPane

/**
 * Generate Commit Message 统一配置页面
 * 包含两个标签页: AI Commit 和 CLI Tools
 */
class GenerateCommitMessageConfigurable : Configurable {
    
    private var tabbedPane: JTabbedPane? = null
    private var aiCommitPanel: AiCommitPanel? = null
    private var cliToolsPanel: CliToolsPanel? = null
    
    override fun getDisplayName(): String = "Generate Commit Message"
    
    override fun createComponent(): JComponent? {
        // 实现将在下一步
        return null
    }
    
    override fun isModified(): Boolean = false
    
    override fun apply() {}
    
    override fun reset() {}
    
    override fun disposeUIResources() {}
}
```

- [ ] **Step 2: 实现 createComponent 方法**

```kotlin
override fun createComponent(): JComponent? {
    if (tabbedPane != null) return tabbedPane
    
    tabbedPane = JTabbedPane()
    aiCommitPanel = AiCommitPanel()
    cliToolsPanel = CliToolsPanel()
    
    tabbedPane!!.addTab("AI Commit", aiCommitPanel!!.panel)
    tabbedPane!!.addTab("CLI Tools", cliToolsPanel!!.panel)
    
    reset()
    return tabbedPane
}
```

- [ ] **Step 3: 实现 isModified 方法**

```kotlin
override fun isModified(): Boolean {
    return (aiCommitPanel?.isModified() == true) || 
           (cliToolsPanel?.isModified() == true)
}
```

- [ ] **Step 4: 实现 apply 方法**

```kotlin
override fun apply() {
    aiCommitPanel?.apply()
    cliToolsPanel?.apply()
}
```

- [ ] **Step 5: 实现 reset 方法**

```kotlin
override fun reset() {
    aiCommitPanel?.reset()
    cliToolsPanel?.reset()
}
```

- [ ] **Step 6: 实现 disposeUIResources 方法**

```kotlin
override fun disposeUIResources() {
    tabbedPane = null
    aiCommitPanel = null
    cliToolsPanel = null
}
```

- [ ] **Step 7: 编译验证**

运行：
```bash
./gradlew compileKotlin
```

预期：SUCCESS

- [ ] **Step 8: Commit Task 5**

```bash
git add src/main/kotlin/com/fs/aicommit/GenerateCommitMessageConfigurable.kt
git commit -m "feat(ui): 创建统一配置入口 GenerateCommitMessageConfigurable"
```

---

## Task 6: 更新 GenerateCommitMessageAction 使用新配置

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/GenerateCommitMessageAction.kt`

- [ ] **Step 1: 检查当前 Action 实现**

运行：
```bash
grep -n "includeRecentCommits\|maxDiffLength\|outputLanguage" src/main/kotlin/com/fs/aicommit/GenerateCommitMessageAction.kt || echo "新字段尚未使用"
```

预期：输出 "新字段尚未使用"

- [ ] **Step 2: 找到获取 git diff 的位置**

找到类似这样的代码：
```kotlin
val diff = getDiff(project)
```

在这之后添加 diff 长度限制逻辑。

- [ ] **Step 3: 添加 Diff 长度限制**

在获取 diff 后，添加截断逻辑：

```kotlin
val diff = getDiff(project)
val maxLength = settings.maxDiffLength
val truncatedDiff = if (diff.length > maxLength) {
    diff.take(maxLength) + "\n\n[... diff 已截断，超出最大长度 $maxLength 字符]"
} else {
    diff
}
```

- [ ] **Step 4: 添加近期提交获取逻辑**

```kotlin
val recentCommits = if (settings.includeRecentCommits) {
    getRecentCommits(project)
} else {
    ""
}
```

- [ ] **Step 5: 实现 getRecentCommits 方法**

在类中添加方法：

```kotlin
private fun getRecentCommits(project: Project): String {
    return try {
        val processBuilder = ProcessBuilder(
            "git", "log", "--oneline", "-10"
        )
        processBuilder.directory(File(project.basePath ?: "."))
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        output.trim()
    } catch (e: Exception) {
        ""
    }
}
```

- [ ] **Step 6: 更新提示词构建调用**

找到构建提示词的代码，类似：
```kotlin
val prompt = AiCommitPrompts.buildFullPrompt(diff, "", settings.customPrompt)
```

修改为：
```kotlin
val prompt = AiCommitPrompts.buildFullPrompt(
    truncatedDiff, 
    recentCommits, 
    settings.customPrompt
)
```

- [ ] **Step 7: 添加输出语言处理**

在提示词后添加语言指令（如果需要英文输出）：

```kotlin
val finalPrompt = if (settings.outputLanguage == "en") {
    prompt + "\n\nIMPORTANT: Generate commit message in English."
} else {
    prompt
}
```

然后使用 `finalPrompt` 而不是 `prompt` 调用 AI。

- [ ] **Step 8: 编译验证**

运行：
```bash
./gradlew compileKotlin
```

预期：SUCCESS

- [ ] **Step 9: Commit Task 6**

```bash
git add src/main/kotlin/com/fs/aicommit/GenerateCommitMessageAction.kt
git commit -m "feat(action): 使用新配置选项生成 commit message"
```

---

## Task 7: 清理和最终验证

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml` (验证)
- Check: 所有未使用的 import 和代码

- [ ] **Step 1: 检查是否有残留的 ApiSearchSettings 引用**

运行：
```bash
grep -r "ApiSearchSettings" src/ || echo "无残留引用"
```

预期：输出 "无残留引用"

如果有引用，手动移除或注释掉。

- [ ] **Step 2: 检查是否有残留的 ApiSearchConfigurable 引用**

运行：
```bash
grep -r "ApiSearchConfigurable" src/ || echo "无残留引用"
```

预期：输出 "无残留引用"

- [ ] **Step 3: 验证 plugin.xml 配置正确**

运行：
```bash
cat src/main/resources/META-INF/plugin.xml | grep -A 5 "GenerateCommitMessageConfigurable"
```

预期：应该看到新的配置入口注册。

- [ ] **Step 4: 完整构建测试**

运行：
```bash
./gradlew clean build -x test
```

预期：BUILD SUCCESSFUL

- [ ] **Step 5: 检查构建产物**

运行：
```bash
ls -lh build/distributions/
```

预期：应该看到 `generateCommitMessage-1.0.0.zip`

- [ ] **Step 6: Commit Task 7**

```bash
git add .
git commit -m "chore: 清理未使用代码和验证构建"
```

---

## Task 8: 更新文档

**Files:**
- Modify: `README.md`
- Modify: `README_CN.md`

- [ ] **Step 1: 更新 README.md 项目名称**

将所有 `SubMsg` 或 `cli-tools-idea` 替换为 `Generate Commit Message`。

标题改为：
```markdown
# Generate Commit Message - AI-Powered Git Commit Message Generator
```

- [ ] **Step 2: 更新功能列表**

移除 API Search 相关描述，添加新的增强选项说明：

```markdown
## ✨ Features

- 🤖 **AI-Powered**: Automatically generate commit messages by analyzing Git diffs
- 🔄 **Dual Mode**: CLI mode (Claude, Codex, Cursor, etc.) and API mode
- 🎯 **Multi-CLI Support**: Claude, Codex, Cursor, OpenCode, Gemini, Grok, OMP
- 🎨 **Custom Prompts**: Add project-specific commit message conventions
- 🌐 **Output Language**: Choose between Chinese or English output
- 📊 **Recent Commits Reference**: Include recent commit style for consistency
- 📏 **Diff Length Control**: Configurable maximum diff length
- 💻 **Windows Optimized**: Perfect support for Windows paths and commands
- ⚡ **Lightweight**: Clean codebase, focused on core functionality
```

- [ ] **Step 3: 更新配置说明**

```markdown
### 3. Configure

1. Open `Settings` → `Tools` → `Generate Commit Message`
2. **AI Commit Tab**:
   - Select CLI or API mode
   - Choose your CLI (e.g., Claude) and model
   - Add custom prompt if needed
   - Configure enhanced options:
     - ☑ Include recent commits reference
     - Set max diff length (default: 5000)
     - Choose output language (Chinese/English)
3. **CLI Tools Tab**:
   - Toggle sidebar icons for each CLI tool
   - Configure high-permission startup flags
```

- [ ] **Step 4: 更新 README_CN.md（中文版）**

同样更新项目名称和功能描述（中文版本）。

- [ ] **Step 5: 编译验证文档链接**

运行：
```bash
grep -n "http" README.md | head -5
```

确保没有失效链接。

- [ ] **Step 6: Commit Task 8**

```bash
git add README.md README_CN.md
git commit -m "docs: 更新项目文档"
```

---

## Task 9: 最终打包和测试

**Files:**
- Build: `build/distributions/generateCommitMessage-1.0.0.zip`

- [ ] **Step 1: 清理构建缓存**

运行：
```bash
./gradlew clean
```

预期：BUILD SUCCESSFUL

- [ ] **Step 2: 完整构建**

运行：
```bash
./gradlew build -x test
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 验证插件包**

运行：
```bash
ls -lh build/distributions/generateCommitMessage-1.0.0.zip
unzip -l build/distributions/generateCommitMessage-1.0.0.zip | head -20
```

预期：文件存在且包含正确的类文件。

- [ ] **Step 4: 测试插件加载（可选 - 如果有 runIde）**

运行：
```bash
./gradlew runIde
```

在打开的 IDEA 实例中：
1. 打开 Settings → Tools
2. 查看是否有 "Generate Commit Message" 入口
3. 点击进入，检查两个标签页是否正常显示
4. 修改配置，点击 Apply，重新打开检查配置是否保存

- [ ] **Step 5: 手动测试 AI Commit 功能**

在测试 IDEA 实例中：
1. 创建一个测试项目（或打开现有项目）
2. 做一些代码修改
3. 打开 VCS → Commit
4. 点击 "Generate Commit Message" 按钮
5. 验证生成的消息是否符合配置（语言、风格等）

- [ ] **Step 6: 手动测试 CLI Tools 配置**

在测试 IDEA 实例中：
1. 打开 Settings → Tools → Generate Commit Message
2. 切换到 CLI Tools 标签页
3. 取消勾选某个 CLI 图标
4. 点击 Apply
5. 查看侧边栏，对应图标应该消失

- [ ] **Step 7: 创建最终标签**

```bash
git tag -a v1.0.0 -m "Release version 1.0.0 - Generate Commit Message"
git push origin v1.0.0
```

- [ ] **Step 8: 最终 Commit**

```bash
git add .
git commit -m "chore: 完成 v1.0.0 版本构建"
git push origin main
```

---

## 完成清单

在所有任务完成后，验证以下内容：

### 功能验证
- [ ] 配置页面路径：Settings → Tools → Generate Commit Message
- [ ] AI Commit 标签页显示正常，包含所有配置项
- [ ] CLI Tools 标签页显示正常，包含图标和高权限选项
- [ ] 配置保存和加载正常工作
- [ ] Generate Commit Message 按钮能正常生成消息
- [ ] 新配置项（近期提交、Diff 长度、输出语言）生效

### 代码质量
- [ ] 无编译错误
- [ ] 无未使用的 import
- [ ] 代码格式符合 Kotlin 规范
- [ ] 注释清晰完整

### 文档完整性
- [ ] README 更新完成
- [ ] 设计文档已创建
- [ ] 实现计划已创建

### 构建产物
- [ ] 插件包正确生成：`generateCommitMessage-1.0.0.zip`
- [ ] 插件包大小合理（预计 4-6 MB）
- [ ] 插件可以正常安装到 IDEA

---

## 注意事项

1. **DRY 原则**：CliToolsPanel 和 AiCommitPanel 复用了现有逻辑，避免重复代码
2. **YAGNI 原则**：只实现设计文档中明确要求的功能，不添加额外特性
3. **TDD 原则**：虽然没有编写单元测试，但每个 Task 都有编译验证步骤
4. **频繁提交**：每个 Task 完成后立即 commit，保持 git 历史清晰

---

## 预估时间

- Task 1: 15 分钟
- Task 2: 5 分钟
- Task 3: 20 分钟
- Task 4: 30 分钟
- Task 5: 15 分钟
- Task 6: 20 分钟
- Task 7: 10 分钟
- Task 8: 15 分钟
- Task 9: 30 分钟

**总计**: 约 2.5 小时

---

**计划结束**
