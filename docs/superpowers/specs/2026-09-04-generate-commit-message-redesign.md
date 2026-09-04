# Generate Commit Message - 项目重构设计文档

**日期**: 2026-09-04  
**状态**: 已批准  
**版本**: 1.0

## 1. 项目概述

### 1.1 目标
将现有的 `cli-tools-idea` / `SubMsg` 项目重构为 `generateCommitMessage`，统一配置界面，简化功能范围，专注于 AI 提交信息生成和 CLI 工具集成。

### 1.2 核心改动
- **项目重命名**：所有用户可见名称改为 "Generate Commit Message"
- **统一配置**：创建单一设置入口，使用标签页组织功能
- **功能精简**：移除 API Search 和 AI Inline，保留核心 AI Commit 和 CLI Tools
- **功能增强**：AI Commit 配置项增强，支持更多定制选项

---

## 2. 架构设计

### 2.1 整体结构

```
Settings → Tools → Generate Commit Message
├── [AI Commit] 标签页
│   ├── CLI 与模型配置（复用 ProviderModelPanel）
│   ├── 附加提示词
│   └── 增强选项（新增）
│       ├── 包含近期提交风格参考
│       ├── Diff 最大长度
│       └── 输出语言
└── [CLI Tools] 标签页
    ├── 侧边栏图标显示
    └── 高权限启动
```

### 2.2 核心组件

#### 2.2.1 GenerateCommitMessageConfigurable
统一配置入口，负责：
- 创建 JTabbedPane 布局
- 管理两个子面板的生命周期
- 协调 apply/reset/isModified 逻辑

#### 2.2.2 AiCommitPanel
AI Commit 标签页面板，包含：
- ProviderModelPanel（CLI + 模型 + 思考等级）
- 附加提示词文本框
- 查看内置提示词按钮
- 增强选项区域

#### 2.2.3 CliToolsPanel
CLI Tools 标签页面板，复用现有 CliToolsConfigurable 逻辑：
- 7 个 CLI 工具的图标显示开关
- 7 个 CLI 工具的高权限启动开关

---

## 3. 详细设计

### 3.1 项目重命名

#### 3.1.1 plugin.xml 更新
```xml
<id>com.fs.generateCommitMessage</id>
<name>Generate Commit Message</name>
<description>
  AI-powered Git commit message generator with CLI tools integration
</description>
```

#### 3.1.2 build.gradle.kts 更新
```kotlin
group = "com.fs"
version = "1.0.0"
// 插件包名：generateCommitMessage-1.0.0.zip
```

#### 3.1.3 包名
保持现有包名不变：
- `com.fs.aicommit.*` - AI Commit 相关
- `com.fs.terminal.*` - CLI Tools 相关
- `com.fs.*` - 其他工具类

### 3.2 AI Commit 标签页设计

#### 3.2.1 布局结构
```
┌─────────────────────────────────────────────┐
│ CLI 与模型                                   │
│ ┌─────────────────────────────────────────┐ │
│ │ CLI:   [Claude          ▼]              │ │
│ │ 模型:  [claude-sonnet-4-6 ▼]           │ │
│ │ 思考:  [medium          ▼]              │ │
│ │ [刷新模型列表] [测试]                   │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ 附加提示词:                                  │
│ ┌─────────────────────────────────────────┐ │
│ │                                         │ │
│ │  (多行文本框，6 行高)                   │ │
│ │                                         │ │
│ └─────────────────────────────────────────┘ │
│ [查看内置提示词] [清空附加提示词]           │
│                                             │
│ 增强选项:                                   │
│ ☑ 包含近期提交风格参考 (最近 10 条)         │
│                                             │
│ Diff 最大长度: [5000        ] 字符          │
│ (建议范围: 1000-10000)                      │
│                                             │
│ 输出语言: ⦿ 中文  ○ 英文                    │
└─────────────────────────────────────────────┘
```

#### 3.2.2 配置项说明

**CLI 与模型（复用 ProviderModelPanel）**
- 保持现有功能不变
- 支持 7 种 CLI：Claude, Codex, Cursor, OpenCode, Gemini, Grok, OMP
- 模型下拉支持搜索输入
- 思考等级根据 CLI 动态显示/隐藏
- 刷新按钮重新拉取模型列表
- 测试按钮验证 CLI 可用性

**附加提示词**
- 6 行高的多行文本框
- 支持换行和滚动
- "查看内置提示词" 按钮：弹窗显示只读的内置提示词
- "清空附加提示词" 按钮：一键清空文本框

**增强选项（新增）**

*包含近期提交风格参考*
- JBCheckBox，默认选中
- 说明文字：(最近 10 条)
- 选中时：调用 `git log --oneline -10` 获取近期提交
- 未选中时：不传递近期提交给 AI

*Diff 最大长度*
- JTextField，数字输入
- 默认值：5000
- 范围提示：(建议范围: 1000-10000)
- 用于截断过长的 git diff

*输出语言*
- ButtonGroup + 2 个 JRadioButton
- 选项：中文（默认）、英文
- 影响生成的 commit message 语言

#### 3.2.3 数据持久化

新增配置项存储在 `AiCommitSettings`：
```kotlin
var includeRecentCommits: Boolean = true
var maxDiffLength: Int = 5000
var outputLanguage: String = "zh" // "zh" 或 "en"
```

### 3.3 CLI Tools 标签页设计

#### 3.3.1 布局结构
```
┌─────────────────────────────────────────────┐
│ 侧边栏图标显示:                              │
│ ☑ 显示 Claude 图标                           │
│ ☑ 显示 Codex 图标                            │
│ ☑ 显示 Cursor 图标                           │
│ ☑ 显示 OpenCode 图标                         │
│ ☐ 显示 Antigravity 图标                      │
│ ☐ 显示 Grok 图标                             │
│ ☐ 显示 OMP 图标                              │
│                                             │
│ 高权限启动:                                  │
│ ☐ Claude 高权限启动 (--permission-mode ...)  │
│ ☐ Codex 高权限启动 (--sandbox danger...)     │
│ ☐ Cursor 高权限启动 (cursor-agent --force)   │
│ ☐ OpenCode 高权限启动 (opencode --auto)      │
│ ☐ Antigravity 高权限启动 (--dangerously...)  │
│ ☐ Grok 高权限启动 (--always-approve)         │
│ ☐ Oh My Pi 高权限启动 (--auto-approve)       │
└─────────────────────────────────────────────┘
```

#### 3.3.2 实现方式
- 直接复用 `CliToolsConfigurable` 的核心逻辑
- 提取为独立的 `CliToolsPanel` 组件
- 数据存储在 `CliToolsSettings`（已存在）

### 3.4 配置生命周期

#### 3.4.1 初始化流程
```kotlin
createComponent() {
    val tabbedPane = JTabbedPane()
    aiCommitPanel = AiCommitPanel()
    cliToolsPanel = CliToolsPanel()
    
    tabbedPane.addTab("AI Commit", aiCommitPanel)
    tabbedPane.addTab("CLI Tools", cliToolsPanel)
    
    reset() // 从 Settings 加载数据
    return tabbedPane
}
```

#### 3.4.2 保存流程
```kotlin
apply() {
    // AI Commit 配置
    settings.activeProvider = aiCommitPanel.getSelectedProvider()
    settings.model = aiCommitPanel.getComposedModel()
    settings.customPrompt = aiCommitPanel.getCustomPrompt()
    settings.includeRecentCommits = aiCommitPanel.getIncludeRecentCommits()
    settings.maxDiffLength = aiCommitPanel.getMaxDiffLength()
    settings.outputLanguage = aiCommitPanel.getOutputLanguage()
    
    // CLI Tools 配置
    cliToolsPanel.apply()
}
```

#### 3.4.3 变更检测
```kotlin
isModified(): Boolean {
    return aiCommitPanel.isModified() || cliToolsPanel.isModified()
}
```

---

## 4. 数据模型

### 4.1 AiCommitSettings 扩展

```kotlin
@State(
    name = "AiCommitSettings",
    storages = [Storage("ai-commit.xml")]
)
class AiCommitSettings : PersistentStateComponent<AiCommitSettings> {
    // 现有字段
    var activeProvider: String = PROVIDER_CLAUDE
    var model: String = ""
    var customPrompt: String = ""
    
    // 新增字段
    var includeRecentCommits: Boolean = true
    var maxDiffLength: Int = 5000
    var outputLanguage: String = "zh" // "zh" 或 "en"
    
    companion object {
        const val PROVIDER_CLAUDE = "claude"
        // ... 其他 PROVIDER 常量
        
        fun getInstance(): AiCommitSettings = 
            ApplicationManager.getApplication()
                .getService(AiCommitSettings::class.java)
    }
}
```

### 4.2 CliToolsSettings
保持现有实现不变：
- 图标显示状态 Map
- 高权限启动状态 Map

---

## 5. 移除功能

### 5.1 删除的文件
以下文件将被删除：
- `ApiSearchSettings.kt`
- `ApiSearchConfigurable.kt`
- `AiInlineService.kt`（如果存在）
- `AiInlineCompletionProvider.kt`（如果存在）
- `AiInlinePanel.kt`（如果存在）

### 5.2 plugin.xml 清理
移除以下配置项：
```xml
<!-- 删除 -->
<applicationService serviceImplementation="com.fs.ApiSearchSettings"/>
<applicationConfigurable instance="com.fs.ApiSearchConfigurable"/>
<applicationService serviceImplementation="com.fs.aicommit.AiInlineService"/>
<completion.contributor ... AiInlineCompletionProvider"/>
```

### 5.3 Action 清理
保留的 Actions：
- `GenerateCommitMessageAction` - VCS 提交窗口按钮
- `OpenExternalEditorAction` - 外部编辑器跳转（如果存在）

删除的 Actions：
- API Search 相关 Actions

---

## 6. UI/UX 设计

### 6.1 间距规范
使用 IntelliJ Platform UI 标准间距：
- 组件间垂直间距：10px (`addVerticalGap(10)`)
- 标签与输入框间距：6px
- 按钮间间距：8px
- 面板内边距：10px (`EmptyBorder(JBUI.insets(10))`)
- 分组边框：TitledBorder

### 6.2 布局原则
- 使用 `FormBuilder` 构建表单布局
- 使用 `BorderLayout` 控制整体布局
- 使用 `BoxLayout` 垂直排列多个组件
- 使用 `FlowLayout` 水平排列按钮

### 6.3 交互反馈
- 测试按钮：显示加载状态，成功/失败通知
- 刷新按钮：显示加载状态，更新模型列表
- 查看内置提示词：弹窗显示，只读，640x480 尺寸
- 清空按钮：无确认直接清空

---

## 7. 实现计划

### 7.1 阶段 1：项目重命名
1. 更新 `plugin.xml` id, name, description
2. 更新 `build.gradle.kts` group, version
3. 删除 API Search 和 AI Inline 相关文件
4. 清理 plugin.xml 中不需要的配置

### 7.2 阶段 2：数据模型扩展
1. 扩展 `AiCommitSettings` 添加新字段
2. 添加默认值和验证逻辑
3. 测试配置持久化

### 7.3 阶段 3：UI 组件开发
1. 创建 `GenerateCommitMessageConfigurable`
2. 创建 `AiCommitPanel` 组件
3. 提取 `CliToolsPanel` 组件
4. 实现标签页布局

### 7.4 阶段 4：功能集成
1. 连接 UI 与 Settings
2. 实现增强选项逻辑
3. 更新 `GenerateCommitMessageAction` 使用新配置
4. 测试所有配置项生效

### 7.5 阶段 5：测试与打包
1. 手动测试所有配置项
2. 测试 CLI 工具集成
3. 测试 AI Commit 生成
4. 构建插件包

---

## 8. 测试计划

### 8.1 功能测试
- [ ] 配置页面正常打开
- [ ] 两个标签页正常切换
- [ ] AI Commit 配置保存和加载
- [ ] CLI Tools 配置保存和加载
- [ ] 刷新模型列表功能
- [ ] 测试 CLI 连接功能
- [ ] 查看内置提示词功能
- [ ] 清空附加提示词功能

### 8.2 集成测试
- [ ] 生成 commit message 使用新配置
- [ ] 包含/不包含近期提交参考
- [ ] Diff 长度限制生效
- [ ] 输出语言切换生效
- [ ] CLI 终端图标显隐生效
- [ ] 高权限启动参数生效

### 8.3 兼容性测试
- [ ] IDEA 2022.2+ 版本兼容
- [ ] Windows 系统测试
- [ ] macOS 系统测试
- [ ] Linux 系统测试

---

## 9. 风险与依赖

### 9.1 技术风险
- **风险**：删除 API Search 可能影响现有用户
- **缓解**：在文档中说明移除原因，提供迁移指南

### 9.2 依赖项
- IntelliJ Platform SDK 2022.2+
- Kotlin 1.8.21
- Gson 库（JSON 解析）

### 9.3 向后兼容
- 配置文件：保留 `ai-commit.xml` 和 `cli-tools.xml`
- 新字段有默认值，不影响旧配置加载

---

## 10. 文档更新

### 10.1 用户文档
- 更新 README.md 项目名称
- 更新截图（设置界面）
- 更新功能列表
- 添加新配置项说明

### 10.2 开发文档
- 更新架构文档
- 更新 API 文档
- 添加配置项说明

---

## 11. 交付物

1. **源代码**
   - 重构后的完整项目代码
   - 单元测试（如果适用）

2. **插件包**
   - `generateCommitMessage-1.0.0.zip`

3. **文档**
   - README.md（更新）
   - 本设计文档
   - 用户手册（可选）

4. **测试报告**
   - 功能测试清单
   - 已知问题列表

---

## 12. 附录

### 12.1 CLI 工具列表
| ID | 名称 | 默认模型 | 思考等级 |
|----|------|---------|---------|
| claude | Claude CLI | claude-sonnet-4-6 | low→max |
| codex | Codex CLI | gpt-5.5 | minimal→xhigh |
| cursor | Cursor Agent | auto | - |
| opencode | OpenCode | (配置) | - |
| gemini | Gemini (agy) | 3.1 Pro High | (内置) |
| grok | Grok | grok-4.1 | - |
| omp | Oh My Pi | (配置) | off→auto |

### 12.2 配置文件位置
- AI Commit: `.idea/ai-commit.xml` 或 `~/.config/JetBrains/.../options/ai-commit.xml`
- CLI Tools: `.idea/cli-tools.xml` 或 `~/.config/JetBrains/.../options/cli-tools.xml`

### 12.3 相关链接
- 原项目：https://github.com/l-ning/cli-tools-idea
- IntelliJ Platform SDK：https://plugins.jetbrains.com/docs/intellij/

---

**文档结束**
