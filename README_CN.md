# SubMsg - AI 驱动的 Git 提交信息生成器

[![版本](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/hwglhj/submsg)
[![平台](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)](https://github.com/hwglhj/submsg)
[![许可证](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

> 基于 [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) 项目改进，专为 Windows 优化的 IntelliJ IDEA AI Commit 插件

[English](README.md) | [简体中文](README_CN.md)

## ✨ 特性

- 🤖 **AI 驱动**：通过分析 Git diff 自动生成提交信息
- 🔄 **双模式**：CLI 模式（Claude、Codex、Cursor 等）和 API 模式（OpenAI、Anthropic 等）
- 🎯 **多 CLI 支持**：Claude CLI、Codex CLI、Cursor Agent、OpenCode、Gemini (agy)、Grok、OMP
- 🎨 **自定义提示词**：添加项目特定的提交信息规范
- 💻 **Windows 优化**：完美支持 Windows 路径、命令检测和模型列表
- ⚡ **轻量级**：精简代码，专注核心功能

## 📸 截图

### 设置界面
![设置](docs/images/settings.png)

### 生成提交信息
![生成](docs/images/generate.png)

## 🚀 快速开始

### 1. 安装 CLI 工具

选择一个支持的 CLI 工具：

#### Claude CLI（推荐）
```bash
npm install -g @anthropic-ai/claude-cli
claude /login
```

#### Codex CLI
```bash
npm install -g @openai/codex-cli
codex login
```

#### Cursor Agent
```bash
npm install -g @cursor/agent
cursor-agent login
```

### 2. 安装插件

**从市场安装（即将推出）**
- 打开 IntelliJ IDEA
- 前往 `设置` → `插件` → `市场`
- 搜索 "SubMsg"
- 点击 `安装`

**从发布版本安装**
- 从 [Releases](https://github.com/hwglhj/submsg/releases) 下载最新的 `.zip` 文件
- 打开 IntelliJ IDEA
- 前往 `设置` → `插件` → `⚙️` → `从磁盘安装插件...`
- 选择下载的 `.zip` 文件

### 3. 配置

1. 打开 `设置` → `工具` → `SubMsg - AI Commit`
2. 选择 **CLI 模式** 或 **API 模式**
3. CLI 模式配置：
   - 选择你的 CLI（例如 Claude）
   - 输入模型名称（例如 `claude-sonnet-4-6`）
   - 点击 `测试` 验证
4. API 模式配置：
   - 点击 `添加` 创建新的 API 配置
   - 填写 Base URL、API Key 和 Model
   - 点击 `测试` 验证

### 4. 使用

1. 修改一些代码
2. 打开 VCS 提交窗口
3. 点击 **"AI 生成提交信息"** 按钮
4. 审查和编辑生成的信息
5. 提交！

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

- **IntelliJ IDEA**: 2021.1 或更高版本（社区版 / 旗舰版）
- **Java**: 11 或更高版本
- **操作系统**: Windows 10/11, macOS 10.14+, Linux

### Windows 特别优化

SubMsg 针对 Windows 进行了优化：
- ✅ 支持 `.exe`、`.cmd`、`.bat` 可执行文件
- ✅ Windows 路径检测（Program Files、AppData 等）
- ✅ 使用 `where` 命令检测 CLI
- ✅ 正确的 PATH 分隔符（`;` vs `:`）
- ✅ 支持 nvm-windows

## 🔧 高级配置

### 自定义提示词

在"附加提示词"字段添加项目特定要求：

```
遵循 conventional commits 格式。
使用 emoji 前缀：✨ feat、🐛 fix、📝 docs、♻️ refactor
主题行保持在 50 字符以内。
```

### API 模式

支持任何 OpenAI 兼容的 API：

- **OpenAI**: `https://api.openai.com/v1`
- **Anthropic**: `https://api.anthropic.com/v1`
- **Azure OpenAI**: `https://your-resource.openai.azure.com/openai/deployments/your-deployment`
- **本地 LLM**: `http://localhost:1234/v1`

## 🐛 故障排除

### Windows CLI 检测失败

1. 确认 CLI 已正确安装：
   ```bash
   claude --version
   # 或
   codex --version
   ```

2. 如果命令未找到，添加到 PATH：
   - npm 全局路径通常在 `%APPDATA%\npm`
   - 或使用完整路径配置

3. 重启 IntelliJ IDEA

### 模型列表获取失败

1. 检查网络连接
2. 确认 CLI 已登录：
   ```bash
   claude /login
   # 或
   codex login
   ```
3. 手动输入模型名称

### API 模式连接失败

1. 检查 Base URL 格式（需要包含完整路径，如 `/v1`）
2. 验证 API Key 是否正确
3. 检查网络是否可以访问 API 地址

## 🤝 贡献

欢迎贡献！请随时提交 Pull Request。

1. Fork 本仓库
2. 创建特性分支（`git checkout -b feature/amazing-feature`）
3. 提交更改（`git commit -m '✨ Add amazing feature'`）
4. 推送到分支（`git push origin feature/amazing-feature`）
5. 打开 Pull Request

## 📝 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- 基于 [@l-ning](https://github.com/l-ning) 的 [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) 项目
- 感谢 Anthropic、OpenAI 等公司提供的优秀 AI CLI 工具

## 📮 联系方式

- **作者**: hwglhj
- **问题反馈**: [GitHub Issues](https://github.com/hwglhj/submsg/issues)
- **讨论**: [GitHub Discussions](https://github.com/hwglhj/submsg/discussions)

## 🗺️ 路线图

- [ ] 发布到 JetBrains 市场
- [ ] 支持更多 AI 提供商
- [ ] 添加提交信息模板
- [ ] 多语言支持
- [ ] 批量生成提交信息

---

⭐ 如果觉得这个项目有用，请给个 star！
