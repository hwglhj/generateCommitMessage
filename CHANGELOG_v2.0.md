# SubMsg v2.0.0 更新日志

## 🎉 重大更新

### ✨ 新功能

#### 1. AI Inline Reference（AI 行内代码补全）
- **实时代码补全**：在编码时提供 AI 驱动的代码建议
- **支持所有语言**：适用于任何编程语言
- **双模式支持**：
  - **CLI 模式**：使用本地 CLI 工具（Claude/Codex/Cursor/Gemini/Grok/OMP）
  - **API 模式**：配置自定义第三方 API（OpenAI/Anthropic/DeepSeek 等）
- **自定义提示词**：可配置专属的补全提示模板
- **智能上下文感知**：分析光标前后代码，提供精准建议

#### 2. 多语言界面支持
- **中文界面**（默认）：完整的中文 UI
- **英文界面**：支持切换到 English
- **一键切换**：在设置中轻松切换语言
- **重启生效**：切换后重启 IDEA 即可应用

#### 3. 自定义 API 配置
- **多 API 管理**：支持配置多个第三方 API
- **灵活切换**：随时在不同 API 配置间切换
- **完整信息**：配置名称、提供商、Base URL、API Key、模型
- **一键测试**：测试 API 连接是否正常
- **模型列表获取**：自动从 API 获取可用模型列表

#### 4. 全新 UI 设计
- **标签页布局**：
  - **AI Commit 标签页**：Git 提交信息生成配置
  - **AI Inline 标签页**：代码补全功能配置
- **优化的配置界面**：更清晰的布局和更好的用户体验
- **更多配置选项**：
  - 补全触发延迟
  - 最大生成 Token 数
  - 自定义提示词模板

---

## 🔧 功能详解

### AI Inline Reference 配置项

#### 基本设置
| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| 启用 AI Inline | 是否启用代码补全功能 | 关闭 |
| 使用模式 | CLI 模式 / API 模式 | CLI |
| 触发延迟 | 输入后多久触发补全（毫秒） | 500ms |
| 最大 Token | 单次补全的最大 Token 数 | 100 |

#### CLI 模式配置
- **选择 CLI**：Claude/Codex/Cursor/OpenCode/Gemini/Grok/OMP
- **选择模型**：从自动检测的模型列表中选择
- **思考等级**：设置模型推理深度（如支持）
- **CLI 路径**：可选，手动指定 CLI 可执行文件路径

#### API 模式配置
- **管理 API 配置**：添加/编辑/删除多个 API 配置
- **选择激活配置**：从已保存的配置中选择一个使用
- **API 配置包含**：
  - 配置名称（如 "OpenAI GPT-4"）
  - 提供商（OpenAI Compatible / Anthropic）
  - Base URL（如 `https://api.openai.com/v1`）
  - API Key
  - 模型名称（支持自动获取）
  - 推理模型兼容性开关

#### 自定义提示词
```
你可以自定义 AI Inline 的提示词模板，例如：

You are a professional code assistant. Analyze the code context
and provide accurate, efficient code completions.

Requirements:
- Keep completions concise and relevant
- Follow the project's coding style
- Prioritize readability and maintainability
```

---

## 🌍 多语言支持

### 支持的语言
- 🇨🇳 **中文**（简体）
- 🇺🇸 **English**

### 如何切换语言
1. 打开 `Settings → Tools → SubMsg`
2. 在顶部找到 **语言** / **Language** 下拉框
3. 选择 `中文` 或 `English`
4. 点击 `Apply` / `OK`
5. 重启 IntelliJ IDEA

### 多语言覆盖范围
- ✅ 所有设置界面文本
- ✅ 按钮和标签
- ✅ 提示信息和错误消息
- ✅ 通知消息
- ✅ 操作菜单项

---

## 📋 使用场景

### AI Commit Message
**场景**：提交代码时自动生成规范的 commit message

**步骤**：
1. 在 Git 提交界面选中要提交的文件
2. 点击 "AI Generate Commit Message" 按钮
3. 等待 AI 分析代码变更
4. 查看并编辑生成的提交信息
5. 提交代码

**示例**：
```
feat: add user authentication module

- Implement JWT token generation and validation
- Add login and register endpoints
- Create user session management
```

### AI Inline Reference
**场景**：编码时获取代码补全建议

**步骤**：
1. 在设置中启用 AI Inline Reference
2. 配置 CLI 或 API 模式
3. 开始编码
4. 输入代码后，AI 会自动提供补全建议
5. 从建议列表中选择合适的代码

**示例**：
```kotlin
// 输入：
fun calculate

// AI 建议：
fun calculateTotal(items: List<Item>): Double {
    return items.sumOf { it.price * it.quantity }
}
```

---

## 🔄 从 v1.0 升级

### 自动迁移
- ✅ 现有的 CLI 配置会自动保留
- ✅ 模型选择和思考等级会自动迁移
- ✅ 自定义提示词会自动保留

### 新增配置项
- ❌ AI Inline 功能默认关闭，需手动启用
- ❌ 语言默认为中文，可切换到英文
- ❌ API 配置为空，需手动添加

### 建议操作
1. 升级插件到 v2.0.0
2. 打开设置页面，查看新增的 "AI Inline" 标签页
3. 根据需要配置 AI Inline 功能
4. 如需英文界面，切换语言并重启

---

## 🛠️ 技术改进

### 架构优化
- **模块化设计**：将 AI Commit 和 AI Inline 分离为独立模块
- **服务层封装**：创建 `AiInlineService` 统一管理补全逻辑
- **配置持久化**：使用 IDEA 的 `PersistentStateComponent` 保存配置

### UI 重构
- **标签页布局**：采用 `JTabbedPane` 组织配置界面
- **响应式设计**：自适应窗口大小变化
- **多语言支持**：完整的 i18n 实现

### 代码质量
- **类型安全**：使用 Kotlin 的类型系统避免空指针
- **错误处理**：完善的异常捕获和用户提示
- **日志记录**：详细的调试日志便于问题排查

---

## 🐛 Bug 修复

- 修复 Windows 下 CLI 检测失败的问题
- 修复 API 配置对话框布局错位
- 修复模型列表刷新不及时的问题
- 修复配置保存后未立即生效的问题

---

## 📚 文档更新

### 新增文档
- `CHANGELOG_v2.0.md`：详细的更新日志（本文档）
- `AI_INLINE_GUIDE.md`：AI Inline Reference 使用指南
- `MULTILANG_GUIDE.md`：多语言功能使用指南
- `API_CONFIG_GUIDE.md`：API 配置完整教程

### 更新文档
- `README.md`：更新功能介绍和使用说明
- `INSTALL_CN.md`：添加 v2.0 新功能的安装说明

---

## 🎯 后续计划

### v2.1.0（计划中）
- [ ] AI Inline 性能优化
- [ ] 支持更多补全提供商
- [ ] 添加补全历史记录
- [ ] 补全建议排序优化

### v2.2.0（计划中）
- [ ] 支持代码重构建议
- [ ] 添加代码质量分析
- [ ] 集成代码审查功能
- [ ] 支持团队配置共享

---

## 💬 反馈与支持

### 问题反馈
- GitHub Issues: https://github.com/hwglhj/submsg/issues
- Email: hwglhj@example.com

### 功能建议
欢迎在 GitHub 上提交 Feature Request！

### 贡献代码
欢迎提交 Pull Request，一起改进 SubMsg！

---

## 📄 许可证

MIT License

---

**感谢使用 SubMsg v2.0！**

如有任何问题或建议，请随时联系我们。
