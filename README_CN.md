# Generate Commit Message - AI 驱动的 Git 提交信息生成器

[![版本](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/hwglhj/generateCommitMessage)
[![平台](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)](https://github.com/hwglhj/generateCommitMessage)
[![许可证](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

> 基于 [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) 改进的 IntelliJ IDEA 插件，用你本机已安装并登录的 AI CLI 生成提交信息，针对 Windows 优化。

[English](README.md) | [简体中文](README_CN.md)

## ✨ 特性

- 🤖 **AI 驱动**：分析提交面板中勾选的变更，自动生成提交信息
- 🎯 **多 CLI 支持**：Claude、Codex、Cursor Agent、OpenCode、Gemini (agy)、Grok、OMP
- 🔐 **无需 API Key**：复用各 CLI 自身的登录凭证，IDE 中不保存任何密钥
- 🎨 **自定义提示词**：在内置提示词之后叠加项目特定的提交规范
- 📊 **近期提交风格参考**：可选把最近 10 条提交信息传给 AI，保持风格一致
- 📏 **Diff 长度控制**：可配置传给 AI 的 diff 最大长度
- 🌐 **输出语言**：中文（默认）或英文提交信息
- 🖥️ **CLI Tools 侧边栏**：每个 CLI 一个工具窗口，可按需高权限启动
- 🔗 **外部编辑器跳转**：在 VS Code / Cursor / Antigravity 中打开当前文件
- 💻 **Windows 优化**：支持 `.exe`/`.cmd`/`.bat` 检测、npm 与 nvm-windows 路径

## 🚀 快速开始

### 1. 安装 CLI 工具

至少安装并登录一个支持的 CLI，例如：

```bash
# Claude CLI（推荐）
npm install -g @anthropic-ai/claude-code
claude /login

# Codex CLI
npm install -g @openai/codex
codex login
```

### 2. 安装插件

- 构建：`./gradlew buildPlugin`，产物为 `build/distributions/generateCommitMessage-1.0.0.zip`
- 打开 IntelliJ IDEA → `设置` → `插件` → `⚙️` → `从磁盘安装插件...`
- 选择该 `.zip` 文件并重启 IDE

### 3. 配置

打开 `设置` → `工具` → `Generate Commit Message`。

**AI Commit 标签页**

1. 选择 CLI 与模型（模型列表从 CLI 拉取，也可手动输入任意模型名）
2. 支持思考等级的 CLI 可选择思考等级
3. 点击 `测试` 验证 CLI 已安装并登录
4. 可选填写附加提示词（叠加在内置提示词之后，可点 `查看内置提示词` 查看）
5. 增强选项：
   - ☑ 包含近期提交风格参考（最近 10 条）
   - Diff 最大长度（字符数，默认 5000，最小 500）
   - 输出语言：中文 / 英文

**CLI Tools 标签页**

- 逐个开关各 CLI 工具的侧边栏图标
- 逐个开启高权限启动参数（例如 `claude --permission-mode bypassPermissions`）

### 4. 使用

1. 修改一些代码
2. 打开 VCS 提交面板，勾选要提交的文件
3. 点击提交信息框旁的 ⚡ **Generate Commit Message** 按钮
4. 审查或编辑生成的信息，然后提交

生成过程是可取消的后台任务；取消或失败时会恢复你原先输入的内容。

## 🛠️ 支持的 CLI

| CLI | ID | 默认模型 | 思考等级 |
|-----|----|---------:|---------|
| **Claude CLI** | `claude` | claude-sonnet-4-6 | low, medium, high, xhigh, max |
| **Codex CLI** | `codex` | gpt-5.5 | minimal, low, medium, high, xhigh |
| **Cursor Agent** | `cursor` | auto | - |
| **OpenCode** | `opencode` | （已配置）| - |
| **Gemini (agy)** | `gemini` | Gemini 3.1 Pro (High) | （包含在模型名中）|
| **Grok** | `grok` | grok-4.1 | - |
| **OMP** | `omp` | （已配置）| off, minimal, low, medium, high, xhigh, max, auto |

## 📋 系统要求

- **IntelliJ IDEA**：2021.1 或更高版本（社区版 / 旗舰版），需启用 Terminal 插件
- **Java**：构建需要 JDK 17
- **操作系统**：Windows 10/11、macOS、Linux

## 🔧 高级配置

### 自定义提示词

在"附加提示词"中添加项目特定要求，例如：

```
scope 固定使用 core。
在 footer 中引用分支名里的 Jira 单号。
```

### 配置文件位置

- AI Commit：IDE options 目录下的 `ai-commit.xml`
- CLI Tools：IDE options 目录下的 `fs-cli-tools.xml`

## 🐛 故障排除

### Windows 下检测不到 CLI

1. 在终端确认 CLI 可用：`claude --version` 或 `codex --version`
2. 命令未找到时把 npm 全局目录（通常是 `%APPDATA%\npm`）加入 PATH
3. 重启 IntelliJ IDEA

### 模型列表获取失败

1. 确认 CLI 已登录（`claude /login`、`codex login`）
2. 点击 `刷新模型列表` 重试，或直接手动输入模型名

### 生成失败提示需要登录

插件复用 CLI 自身的凭证，请先在终端完成对应 CLI 的登录或认证配置。

## 🧑‍💻 开发

```bash
./gradlew compileKotlin        # 编译
./gradlew test                 # 单元测试
./gradlew buildPlugin          # 生成 build/distributions/generateCommitMessage-1.0.0.zip
./gradlew runIde               # 启动带插件的沙箱 IDE
```

设计文档见 `docs/superpowers/specs/`，实现计划见 `docs/superpowers/plans/`。

## 🤝 贡献

欢迎贡献，请直接提交 Pull Request。

## 📝 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- 基于 [@l-ning](https://github.com/l-ning) 的 [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) 项目
- 感谢 Anthropic、OpenAI 等公司提供的优秀 AI CLI 工具

---

⭐ 如果觉得这个项目有用，请给个 star！
