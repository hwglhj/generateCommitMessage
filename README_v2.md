# SubMsg - AI Commit & Inline Reference

[![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](https://github.com/hwglhj/submsg)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](./LICENSE)
[![Platform](https://img.shields.io/badge/platform-IntelliJ-orange.svg)](https://www.jetbrains.com/idea/)

> 🚀 一键生成 Git Commit Message + AI 行内代码补全参考

**SubMsg** 是一款强大的 IntelliJ IDEA 插件，提供 AI 驱动的 Git 提交信息生成和实时代码补全功能。基于 [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) 项目改进，专为 Windows 优化，并新增多项实用功能。

---

## ✨ 主要功能

### 🎯 AI Commit Message
- **智能分析**：自动分析 Git diff，生成规范的提交信息
- **双模式支持**：CLI 模式（无需 API Key）+ API 模式（自定义 API）
- **多 CLI 支持**：Claude、Codex、Cursor、Gemini、Grok、OMP 等
- **可取消操作**：后台任务可随时取消
- **自定义提示词**：支持项目特定的提交规范

### 💡 AI Inline Reference（v2.0 新增）
- **实时补全**：编码时提供 AI 驱动的代码建议
- **全语言支持**：适用于所有编程语言
- **上下文感知**：分析光标前后代码，提供精准建议
- **灵活配置**：支持触发延迟、Token 数、自定义提示词
- **隐私保护**：可使用本地 CLI，数据不上传

### 🌍 多语言界面（v2.0 新增）
- **中文界面**：完整的简体中文 UI（默认）
- **英文界面**：完整的 English UI
- **一键切换**：在设置中轻松切换语言
- **完整覆盖**：所有界面、消息、错误提示均已翻译

### ⚙️ 自定义 API 配置（v2.0 新增）
- **多 API 管理**：配置多个第三方 API，随时切换
- **广泛兼容**：支持 OpenAI、Anthropic、DeepSeek、Moonshot 等
- **一键测试**：快速验证 API 连接
- **模型列表**：自动获取可用模型

### 🖥️ Windows 优化
- **完美兼容**：解决 Windows CLI 检测问题
- **路径智能**：自动识别常见安装路径
- **命令适配**：使用 Windows 原生命令

---

## 📸 界面预览

### AI Commit 配置界面
```
┌─────────────────────────────────────────┐
│ SubMsg 设置                              │
├─────────────────────────────────────────┤
│ 语言: [中文 ▼]                           │
├─────────────────────────────────────────┤
│ ┌─ AI Commit ─┬─ AI Inline ─┐          │
│ │ 使用模式: [CLI 模式 ▼]      │          │
│ │                            │          │
│ │ CLI: [Claude ▼]            │          │
│ │ 模型: [claude-sonnet-4-6 ▼]│         │
│ │ 思考等级: [默认 ▼]          │          │
│ │ CLI 路径: [自动检测]        │          │
│ │                            │          │
│ │ [测试]  [刷新模型]          │          │
│ │                            │          │
│ │ 自定义提示词:               │          │
│ │ ┌────────────────────┐    │          │
│ │ │ 生成简洁的中文提交... │   │          │
│ │ └────────────────────┘    │          │
│ └────────────────────────────┘          │
└─────────────────────────────────────────┘
```

### AI Inline 配置界面
```
┌─────────────────────────────────────────┐
│ ┌─ AI Commit ─┬─ AI Inline ─┐          │
│ │              │ ☑ 启用 AI Inline      │ │
│ │              │                      │ │
│ │              │ 使用模式: [CLI ▼]    │ │
│ │              │ CLI: [Claude ▼]      │ │
│ │              │ 模型: [sonnet-4-6▼]  │ │
│ │              │                      │ │
│ │              │ 触发延迟: 500ms      │ │
│ │              │ [━━━━●━━━━]          │ │
│ │              │                      │ │
│ │              │ 最大Token: [100]     │ │
│ │              │                      │ │
│ │              │ 自定义提示词:        │ │
│ │              │ ┌──────────────────┐ │ │
│ │              │ │You are a code... │ │ │
│ │              │ └──────────────────┘ │ │
│ └──────────────┴──────────────────────┘ │
└─────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 1. 安装插件

**方法 A：从源码构建**
```bash
git clone https://github.com/hwglhj/submsg.git
cd submsg
./gradlew buildPlugin
```
生成的插件在 `build/distributions/` 目录下。

**方法 B：直接下载**
从 [Releases](https://github.com/hwglhj/submsg/releases) 下载最新版本。

**安装到 IDEA**：
1. 打开 `Settings → Plugins`
2. 点击 ⚙️ → `Install Plugin from Disk...`
3. 选择下载的 `.zip` 文件
4. 重启 IDEA

### 2. 配置 CLI（推荐）

**安装 Claude CLI**（推荐）：
```bash
# 使用 npm 全局安装
npm install -g @anthropic-ai/claude-cli

# 登录（会在浏览器中打开）
claude /login

# 验证安装
claude --version
```

**其他 CLI 选项**：
```bash
# Codex CLI
npm install -g @openai/codex-cli
codex login

# Gemini CLI (agy)
pip install antigravity-cli
agy login

# Grok CLI
npm install -g grok-cli
grok login
```

### 3. 配置插件

1. 打开 `Settings → Tools → SubMsg`
2. 选择 **AI Commit** 标签页
3. 选择 **使用模式** → `CLI 模式`
4. 选择已安装的 CLI（如 `Claude`）
5. 选择模型（如 `claude-sonnet-4-6`）
6. 点击 **测试** 验证配置
7. 点击 **Apply** 保存

### 4. 开始使用

**生成 Commit Message**：
1. 在 Git 提交界面选中要提交的文件
2. 点击 **AI Generate Commit Message** 按钮
3. 等待 AI 生成（可取消）
4. 查看并编辑生成的提交信息
5. 提交代码

**使用 AI Inline**（可选）：
1. 打开 `Settings → Tools → SubMsg`
2. 切换到 **AI Inline** 标签页
3. 勾选 **启用 AI Inline Reference**
4. 配置 CLI 或 API
5. 开始编码，输入时会自动提供补全建议

---

## 📖 详细文档

### 核心功能
- [**AI Inline 使用指南**](./AI_INLINE_GUIDE.md) - 详细的 AI 代码补全使用教程
- [**多语言功能指南**](./MULTILANG_GUIDE.md) - 语言切换和翻译贡献指南
- [**更新日志 v2.0**](./CHANGELOG_v2.0.md) - 完整的版本更新说明
- [**功能实现总结**](./FEATURE_SUMMARY.md) - 技术实现细节

### 安装和配置
- [**中文安装教程**](./INSTALL_CN.md) - 详细的安装步骤（Windows/macOS/Linux）
- [**API 配置指南**](./INSTALL_CN.md#api-模式) - 自定义 API 配置教程

### 故障排除
- [**常见问题**](./AI_INLINE_GUIDE.md#🔍-故障排除) - CLI 检测、连接问题等
- [**Windows 修复说明**](./WINDOWS_FIX_SUMMARY.md) - Windows 兼容性修复

---

## 🎯 使用场景

### 场景 1：标准开发流程
```bash
# 1. 修改代码
vim src/main/MyService.kt

# 2. 提交时使用 AI 生成 commit message
# 在 IDEA VCS 提交界面点击 "AI Generate Commit Message"

# 3. 得到规范的提交信息
feat: add user authentication service

- Implement JWT token generation
- Add login endpoint with validation
- Create user session management
- Add unit tests for auth service
```

### 场景 2：代码补全辅助
```kotlin
// 输入函数签名
data class User(val id: Int, val name: String)

fun processUser
// ↓ AI 自动建议
fun processUserData(user: User): Result {
    return try {
        // Validate user data
        if (user.name.isBlank()) {
            return Result.Error("Invalid user name")
        }
        
        // Process user
        val processed = userRepository.save(user)
        Result.Success(processed)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unknown error")
    }
}
```

### 场景 3：学习新技术
```python
# 写注释描述需求
# Create a Flask REST API endpoint for user registration
# with email validation and password hashing

# ↓ AI 提供完整实现
@app.route('/api/register', methods=['POST'])
def register_user():
    """User registration endpoint with validation"""
    data = request.get_json()
    
    # Validate email
    email = data.get('email')
    if not email or '@' not in email:
        return jsonify({'error': 'Invalid email'}), 400
    
    # Hash password
    password = data.get('password')
    hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt())
    
    # Save user
    user = User(email=email, password_hash=hashed)
    db.session.add(user)
    db.session.commit()
    
    return jsonify({'message': 'User registered successfully'}), 201
```

---

## ⚙️ 配置选项

### CLI 模式配置

| 选项 | 说明 | 默认值 |
|------|------|--------|
| CLI 选择 | 选择已安装的 CLI | Claude |
| 模型选择 | 选择模型 | claude-sonnet-4-6 |
| 思考等级 | 推理深度（如支持） | 默认 |
| CLI 路径 | 手动指定路径（可选） | 自动检测 |
| 自定义提示词 | 项目特定规范 | 空 |

### API 模式配置

| 选项 | 说明 | 示例 |
|------|------|------|
| 配置名称 | 识别用的名称 | "OpenAI GPT-4" |
| 提供商 | API 提供商类型 | OpenAI Compatible |
| Base URL | API 端点地址 | https://api.openai.com/v1 |
| API Key | 认证密钥 | sk-... |
| 模型 | 使用的模型 | gpt-4-turbo |

### AI Inline 配置

| 选项 | 说明 | 推荐值 |
|------|------|--------|
| 启用开关 | 是否启用补全 | 按需 |
| 使用模式 | CLI / API | CLI |
| 触发延迟 | 输入后多久触发（毫秒） | 500-1000 |
| 最大 Token | 单次补全长度限制 | 100-200 |
| 自定义提示词 | 补全风格定制 | 按项目 |

---

## 🛠️ 支持的 CLI 和模型

### Claude CLI
```bash
# 安装
npm install -g @anthropic-ai/claude-cli

# 推荐模型
- claude-sonnet-4-6（平衡）
- claude-opus-4-8（最强）
- claude-haiku-4-5（最快）

# 思考等级
low, medium, high, xhigh, max
```

### Codex CLI
```bash
# 安装
npm install -g @openai/codex-cli

# 推荐模型
- gpt-5.5（最新）
- gpt-5.4（稳定）
- gpt-5.4-mini（快速）

# 思考等级
minimal, low, medium, high, xhigh
```

### Cursor Agent
```bash
# 安装
# 安装 Cursor IDE 即自动包含

# 模型
- auto（自动选择）
```

### Gemini CLI (agy)
```bash
# 安装
pip install antigravity-cli

# 推荐模型
- Gemini 3.1 Pro (High)
- Gemini 3.5 Flash (High)
- Gemini 3.5 Flash (Low)

# 注：思考等级包含在模型名中
```

### Grok CLI
```bash
# 安装
npm install -g grok-cli

# 推荐模型
- grok-4.1（最新）
- grok-4-fast（快速）
- grok-code-fast-1（代码专用）
```

### OMP CLI
```bash
# 安装
npm install -g omp-cli

# 模型
- 按 OMP 配置，支持多提供商
- 示例：openai-codex/gpt-5.4

# 思考等级
off, minimal, low, medium, high, xhigh, max, auto
```

---

## 🌐 API 提供商配置示例

### OpenAI
```
Base URL: https://api.openai.com/v1
Model: gpt-4-turbo / gpt-3.5-turbo
```

### Anthropic Claude
```
Base URL: https://api.anthropic.com/v1
Model: claude-3-5-sonnet-20241022
推理兼容: ✅
```

### DeepSeek
```
Base URL: https://api.deepseek.com/v1
Model: deepseek-coder / deepseek-chat
```

### Moonshot AI
```
Base URL: https://api.moonshot.cn/v1
Model: moonshot-v1-8k / moonshot-v1-32k
```

### 本地 Ollama
```
Base URL: http://localhost:11434/v1
Model: codellama:7b / llama2:13b
API Key: ollama
```

### LM Studio
```
Base URL: http://localhost:1234/v1
Model: local-model
API Key: lm-studio
```

---

## 📊 版本历史

### v2.0.0 - 重大更新（2025-09）
- ✨ 新增 AI Inline Reference 功能
- 🌍 支持中英文界面切换
- ⚙️ 支持自定义第三方 API 配置
- 🎨 全新标签页 UI 设计
- 🐛 修复 Windows CLI 检测问题

### v1.0.0 - 首次发布（2025-08）
- 基于 cli-tools-idea 2.0.7
- 完全重写 UI 布局
- 修复 Windows 兼容性问题
- 精简代码，专注核心功能

---

## 🤝 贡献

欢迎贡献代码、报告问题或提出建议！

### 如何贡献

1. **Fork 项目**
2. **创建分支** (`git checkout -b feature/AmazingFeature`)
3. **提交更改** (`git commit -m 'Add some AmazingFeature'`)
4. **推送分支** (`git push origin feature/AmazingFeature`)
5. **创建 Pull Request**

### 贡献方向

- 🌍 **翻译**：添加更多语言支持
- 🐛 **Bug 修复**：报告和修复问题
- ✨ **新功能**：提出和实现新想法
- 📖 **文档**：改进文档和示例

---

## 📄 许可证

本项目基于 [MIT License](./LICENSE) 开源。

---

## 🙏 致谢

- [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) - 原始项目
- [Claude Code](https://www.anthropic.com/claude/code) - Claude CLI
- IntelliJ Platform - 强大的 IDE 平台
- 所有贡献者和用户

---

## 💬 联系方式

- **GitHub**: https://github.com/hwglhj/submsg
- **Issues**: https://github.com/hwglhj/submsg/issues
- **Email**: hwglhj@example.com

---

## ⭐ Star History

如果觉得这个项目对你有帮助，请给个 Star ⭐️

---

**让 AI 成为你的编码伙伴！🚀**
