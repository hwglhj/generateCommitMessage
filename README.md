# SubMsg - AI-Powered Git Commit Message Generator

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/hwglhj/submsg)
[![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)](https://github.com/hwglhj/submsg)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

> 基于 [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) 项目改进，专为 Windows 优化的 IntelliJ IDEA AI Commit 插件

[English](README.md) | [简体中文](README_CN.md)

## ✨ Features

- 🤖 **AI-Powered**: Automatically generate commit messages by analyzing Git diffs
- 🔄 **Dual Mode**: CLI mode (Claude, Codex, Cursor, etc.) and API mode (OpenAI, Anthropic, etc.)
- 🎯 **Multi-CLI Support**: Claude CLI, Codex CLI, Cursor Agent, OpenCode, Gemini (agy), Grok, OMP
- 🎨 **Custom Prompts**: Add project-specific commit message conventions
- 💻 **Windows Optimized**: Perfect support for Windows paths, command detection, and model lists
- ⚡ **Lightweight**: Clean codebase, focused on core functionality

## 📸 Screenshots

### Settings Interface
![Settings](docs/images/settings.png)

### Generate Commit Message
![Generate](docs/images/generate.png)

## 🚀 Quick Start

### 1. Install a CLI Tool

Choose one of the supported CLI tools:

#### Claude CLI (Recommended)
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

### 2. Install Plugin

**From Marketplace (Coming Soon)**
- Open IntelliJ IDEA
- Go to `Settings` → `Plugins` → `Marketplace`
- Search for "SubMsg"
- Click `Install`

**From Release**
- Download the latest `.zip` from [Releases](https://github.com/hwglhj/submsg/releases)
- Open IntelliJ IDEA
- Go to `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
- Select the downloaded `.zip` file

### 3. Configure

1. Open `Settings` → `Tools` → `SubMsg - AI Commit`
2. Select **CLI Mode** or **API Mode**
3. For CLI mode:
   - Select your CLI (e.g., Claude)
   - Enter model name (e.g., `claude-sonnet-4-6`)
   - Click `Test` to verify
4. For API mode:
   - Click `Add` to create a new API configuration
   - Fill in Base URL, API Key, and Model
   - Click `Test` to verify

### 4. Use

1. Make some code changes
2. Open VCS commit window
3. Click **"AI Generate Commit Message"** button
4. Review and edit the generated message
5. Commit!

## 🛠️ Supported CLIs

| CLI | ID | Default Model | Thinking Levels |
|-----|----|--------------:|-----------------|
| **Claude CLI** | `claude` | claude-sonnet-4-6 | low, medium, high, xhigh, max |
| **Codex CLI** | `codex` | gpt-5.5 | minimal, low, medium, high, xhigh |
| **Cursor Agent** | `cursor` | auto | - |
| **OpenCode** | `opencode` | (configured) | - |
| **Gemini (agy)** | `gemini` | Gemini 3.1 Pro (High) | (in model name) |
| **Grok** | `grok` | grok-4.1 | - |
| **OMP** | `omp` | (configured) | off, minimal, low, medium, high, xhigh, max, auto |

## 📋 System Requirements

- **IntelliJ IDEA**: 2021.1 or later (Community / Ultimate)
- **Java**: 11 or later
- **OS**: Windows 10/11, macOS 10.14+, Linux

### Windows-Specific

SubMsg is optimized for Windows with:
- ✅ Support for `.exe`, `.cmd`, `.bat` executables
- ✅ Windows path detection (Program Files, AppData, etc.)
- ✅ `where` command for CLI detection
- ✅ Proper PATH separator (`;` vs `:`)
- ✅ nvm-windows support

## 🔧 Advanced Configuration

### Custom Prompts

Add project-specific requirements in the "Custom Prompt" field:

```
Follow conventional commits format.
Use emoji prefixes: ✨ feat, 🐛 fix, 📝 docs, ♻️ refactor
Keep subject line under 50 characters.
```

### API Mode

Supports any OpenAI-compatible API:

- **OpenAI**: `https://api.openai.com/v1`
- **Anthropic**: `https://api.anthropic.com/v1`
- **Azure OpenAI**: `https://your-resource.openai.azure.com/openai/deployments/your-deployment`
- **Local LLM**: `http://localhost:1234/v1`

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m '✨ Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Based on [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) by [@l-ning](https://github.com/l-ning)
- Inspired by the amazing AI CLI tools from Anthropic, OpenAI, and others

## 📮 Contact

- **Author**: hwglhj
- **Issues**: [GitHub Issues](https://github.com/hwglhj/submsg/issues)
- **Discussions**: [GitHub Discussions](https://github.com/hwglhj/submsg/discussions)

## 🗺️ Roadmap

- [ ] Publish to JetBrains Marketplace
- [ ] Support more AI providers
- [ ] Add commit message templates
- [ ] Multi-language support
- [ ] Batch commit message generation

---

⭐ If you find this project useful, please consider giving it a star!
