# 多语言功能使用指南

## 🌍 概述

SubMsg v2.0 新增完整的多语言支持，目前支持：
- 🇨🇳 **中文**（简体）- 默认语言
- 🇺🇸 **English** - 英文

---

## 🚀 快速切换语言

### 方法 1：通过设置界面

1. 打开 `Settings → Tools → SubMsg`
2. 在顶部找到 **语言** / **Language** 选择框
3. 选择你想要的语言：
   - `中文` - 简体中文界面
   - `English` - 英文界面
4. 点击 `Apply` 或 `OK`
5. 重启 IntelliJ IDEA

### 方法 2：直接编辑配置文件

配置文件位置：
- Windows: `%APPDATA%\JetBrains\<IDE>\options\submsg.xml`
- macOS: `~/Library/Application Support/JetBrains/<IDE>/options/submsg.xml`
- Linux: `~/.config/JetBrains/<IDE>/options/submsg.xml`

编辑 `<language>` 标签：
```xml
<component name="AiCommitSettings">
  <option name="language" value="zh" />  <!-- zh=中文, en=英文 -->
  <!-- 其他配置... -->
</component>
```

---

## 📋 多语言覆盖范围

### ✅ 已支持的界面

#### 1. 设置界面
- **主设置页面**
  - 标签页标题（AI Commit / AI Inline）
  - 语言选择器
  - 所有配置项标签

- **AI Commit 配置**
  - 使用模式选择
  - CLI 选择和配置
  - API 配置管理
  - 测试按钮和结果

- **AI Inline 配置**
  - 启用开关
  - 触发延迟、Token 数设置
  - CLI/API 模式选择
  - 自定义提示词

#### 2. 对话框和弹窗
- API 配置对话框
  - 所有表单字段标签
  - 按钮文本
  - 提示信息

- 确认对话框
  - 标题和消息
  - 按钮文本

#### 3. 通知消息
- 成功通知
- 错误提示
- 警告信息
- 进度提示

#### 4. 右键菜单
- VCS 提交界面的 "Generate Commit Message" 按钮
- 按钮描述文本

#### 5. 错误消息
- CLI 检测失败
- API 连接失败
- 配置验证错误
- 模型加载失败

---

## 🔤 语言对照表

### 主要功能

| 中文 | English |
|------|---------|
| AI Commit 设置 | AI Commit Settings |
| AI Inline 参考 | AI Inline Reference |
| 使用模式 | Mode |
| CLI 模式 | CLI Mode |
| API 模式 | API Mode |
| 启用 | Enable |
| 禁用 | Disable |
| 测试 | Test |
| 应用 | Apply |
| 取消 | Cancel |
| 确定 | OK |

### 配置项

| 中文 | English |
|------|---------|
| 选择 CLI | Select CLI |
| 选择模型 | Select Model |
| 思考等级 | Thinking Level |
| CLI 路径 | CLI Path |
| 刷新模型列表 | Refresh Models |
| 触发延迟 | Trigger Delay |
| 最大 Token 数 | Max Tokens |
| 自定义提示词 | Custom Prompt |
| API 配置 | API Configuration |
| 配置名称 | Configuration Name |
| Base URL | Base URL |
| API Key | API Key |
| 模型 | Model |

### 消息提示

| 中文 | English |
|------|---------|
| 测试成功 | Test Successful |
| 测试失败 | Test Failed |
| 连接成功 | Connection Successful |
| 连接失败 | Connection Failed |
| 配置已保存 | Configuration Saved |
| 请选择模型 | Please Select Model |
| 未检测到 CLI | CLI Not Detected |
| 正在生成... | Generating... |
| 生成成功 | Generated Successfully |
| 生成失败 | Generation Failed |
| 已取消 | Cancelled |

### Git Commit

| 中文 | English |
|------|---------|
| 生成提交信息 | Generate Commit Message |
| 正在生成 commit message | Generating commit message |
| 无法访问提交信息输入框 | Cannot access commit message panel |
| 没有选中任何变更 | No changes selected |
| 已取消生成 | Generation cancelled |
| 生成失败 | Generation failed |

---

## 🎨 界面预览

### 中文界面

```
┌─────────────────────────────────────────┐
│ SubMsg 设置                              │
├─────────────────────────────────────────┤
│ 语言: [中文 ▼]                           │
├─────────────────────────────────────────┤
│ ┌─ AI Commit ─┬─ AI Inline ─┐          │
│ │                            │          │
│ │ 使用模式: [CLI 模式 ▼]      │          │
│ │                            │          │
│ │ CLI 选择: [Claude ▼]       │          │
│ │ 模型选择: [claude-sonnet-4-6 ▼]     │
│ │ 思考等级: [默认 ▼]          │          │
│ │                            │          │
│ │ [测试]  [刷新模型]          │          │
│ │                            │          │
│ │ 自定义提示词:               │          │
│ │ ┌────────────────────┐    │          │
│ │ │                    │    │          │
│ │ └────────────────────┘    │          │
│ └────────────────────────────┘          │
│                                         │
│             [应用]  [取消]  [确定]       │
└─────────────────────────────────────────┘
```

### 英文界面

```
┌─────────────────────────────────────────┐
│ SubMsg Settings                          │
├─────────────────────────────────────────┤
│ Language: [English ▼]                    │
├─────────────────────────────────────────┤
│ ┌─ AI Commit ─┬─ AI Inline ─┐          │
│ │                            │          │
│ │ Mode: [CLI Mode ▼]         │          │
│ │                            │          │
│ │ Select CLI: [Claude ▼]     │          │
│ │ Select Model: [claude-sonnet-4-6 ▼] │
│ │ Thinking Level: [Default ▼]│          │
│ │                            │          │
│ │ [Test]  [Refresh Models]   │          │
│ │                            │          │
│ │ Custom Prompt:             │          │
│ │ ┌────────────────────┐    │          │
│ │ │                    │    │          │
│ │ └────────────────────┘    │          │
│ └────────────────────────────┘          │
│                                         │
│             [Apply]  [Cancel]  [OK]     │
└─────────────────────────────────────────┘
```

---

## 🛠️ 开发者指南

### 添加新语言

#### 1. 创建资源文件

在 `src/main/resources/messages/` 目录下创建新的属性文件：

```
messages/
├── SubMsg.properties          # 默认（中文）
├── SubMsg_en.properties       # 英文
└── SubMsg_ja.properties       # 日文（新增）
```

#### 2. 翻译所有文本

复制 `SubMsg_en.properties` 的所有键，翻译为目标语言：

```properties
# SubMsg_ja.properties
settings.title=SubMsg 設定
settings.language=言語
settings.tab.commit=AI コミット
settings.tab.inline=AI インライン

aicommit.mode.label=使用モード
aicommit.mode.cli=CLI モード
aicommit.mode.api=API モード

# ... 其他翻译
```

#### 3. 更新语言选择器

在 `AiCommitSettingsConfigurable.kt` 中添加新语言：

```kotlin
private val languageCombo = ComboBox(arrayOf(
    "中文", 
    "English",
    "日本語"  // 新增
))
```

#### 4. 更新语言映射

```kotlin
languageCombo.addActionListener {
    val selected = languageCombo.selectedItem as String
    val newLang = when (selected) {
        "English" -> "en"
        "日本語" -> "ja"  // 新增
        else -> "zh"
    }
    // ...
}
```

### 使用多语言文本

在代码中获取多语言文本：

```kotlin
// 简单文本
val title = SubMsgBundle.get("settings.title")

// 带参数的文本
val message = SubMsgBundle.message("message.test.failed", errorMsg)

// 在 UI 组件中使用
val button = JButton(SubMsgBundle.get("button.test"))
```

### 资源文件示例

```properties
# 简单文本
settings.title=SubMsg Settings

# 带参数的文本（{0}, {1} 为占位符）
message.test.failed=Test failed: {0}
message.config.invalid=Configuration {0} is invalid

# 多行文本（使用 \n）
message.help=This is line 1\nThis is line 2

# HTML 文本
message.info=<html><b>Important:</b> Please restart IDE</html>
```

---

## 📝 翻译贡献指南

### 如何贡献翻译

1. **Fork 项目**
   ```bash
   git clone https://github.com/hwglhj/submsg.git
   cd submsg
   ```

2. **创建翻译文件**
   ```bash
   cp src/main/resources/messages/SubMsg_en.properties \
      src/main/resources/messages/SubMsg_ja.properties
   ```

3. **翻译内容**
   - 保持键名不变
   - 只翻译等号右边的值
   - 保持占位符 {0}, {1} 等
   - 保持 HTML 标签不变

4. **测试翻译**
   - 在 IDEA 中测试新语言
   - 检查所有界面元素
   - 确保文本长度合适

5. **提交 PR**
   ```bash
   git add .
   git commit -m "Add Japanese translation"
   git push origin main
   ```

### 翻译质量检查清单

- [ ] 所有键都已翻译
- [ ] 占位符保持原样
- [ ] HTML 标签正确
- [ ] 专业术语一致
- [ ] 界面文本长度适中
- [ ] 在实际 UI 中测试通过
- [ ] 无拼写错误
- [ ] 语法正确

---

## 🔍 故障排除

### 问题 1：切换语言后没有变化

**原因**：需要重启 IDEA 才能加载新语言

**解决方案**：
1. 保存设置后完全关闭 IDEA
2. 重新启动 IDEA
3. 检查语言是否已切换

### 问题 2：部分文本仍显示英文

**原因**：某些文本可能未翻译或翻译文件缺失

**解决方案**：
1. 检查 `messages/SubMsg_zh.properties` 是否存在
2. 查看是否所有键都已翻译
3. 报告 Bug 或提交翻译贡献

### 问题 3：翻译文本显示乱码

**原因**：编码问题

**解决方案**：
1. 确保 `.properties` 文件使用 UTF-8 编码
2. 或使用 Unicode 转义（如 `中文`）
3. 重新保存文件为 UTF-8 编码

### 问题 4：自定义语言不生效

**原因**：未正确注册新语言

**解决方案**：
1. 确认资源文件命名正确（如 `SubMsg_ja.properties`）
2. 确认文件放在 `src/main/resources/messages/` 目录
3. 重新构建项目

---

## 🌐 支持的语言列表

### 当前支持

| 语言 | 代码 | 文件名 | 状态 |
|------|------|--------|------|
| 简体中文 | zh | SubMsg.properties | ✅ 完整 |
| English | en | SubMsg_en.properties | ✅ 完整 |

### 计划支持

| 语言 | 代码 | 文件名 | 状态 |
|------|------|--------|------|
| 日本語 | ja | SubMsg_ja.properties | 🔄 计划中 |
| 한국어 | ko | SubMsg_ko.properties | 🔄 计划中 |
| Français | fr | SubMsg_fr.properties | 🔄 计划中 |
| Deutsch | de | SubMsg_de.properties | 🔄 计划中 |
| Español | es | SubMsg_es.properties | 🔄 计划中 |
| Русский | ru | SubMsg_ru.properties | 🔄 计划中 |

---

## 💡 最佳实践

### 1. 选择合适的语言

**根据工作环境选择**：
- 中文团队 → 使用中文界面
- 国际团队 → 使用英文界面
- 学习英语 → 使用英文界面练习

### 2. 统一团队语言

**团队协作建议**：
- 导出配置：包含语言设置
- 团队共享：确保大家使用相同语言
- 文档一致：文档语言与界面语言一致

### 3. 文档语言对应

**建议**：
- 中文界面 → 阅读中文文档
- 英文界面 → 阅读英文文档
- 减少语言切换带来的困扰

---

## 📚 相关资源

### 官方文档
- [SubMsg 主文档](./README.md)
- [更新日志](./CHANGELOG_v2.0.md)
- [AI Inline 指南](./AI_INLINE_GUIDE.md)

### 贡献翻译
- [GitHub 项目](https://github.com/hwglhj/submsg)
- [提交 Issue](https://github.com/hwglhj/submsg/issues)
- [创建 Pull Request](https://github.com/hwglhj/submsg/pulls)

---

## 💬 反馈

**发现翻译问题？**
- 提交 GitHub Issue 说明问题
- 注明语言和出现问题的位置
- 建议更好的翻译

**想贡献新语言？**
- 查看贡献指南
- 创建翻译文件
- 提交 Pull Request

---

**让 SubMsg 支持更多语言，服务全球开发者！🌍**
