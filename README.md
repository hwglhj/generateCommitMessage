# Generate Commit Message - AI-Powered Git Commit Message Generator

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/hwglhj/generateCommitMessage)
[![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)](https://github.com/hwglhj/generateCommitMessage)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

> Based on [cli-tools-idea](https://github.com/l-ning/cli-tools-idea), an IntelliJ IDEA plugin that generates commit messages with the AI CLIs you already have installed, optimized for Windows.

[English](README.md) | [简体中文](README_CN.md)

## ✨ Features

- 🤖 **AI-Powered**: Generate commit messages by analyzing the changes you selected in the commit panel
- 🎯 **Multi-CLI Support**: Claude, Codex, Cursor Agent, OpenCode, Gemini (agy), Grok, OMP
- 🔐 **No API Key**: Reuses each CLI's own login; nothing sensitive is stored in the IDE
- 🎨 **Custom Prompts**: Append project-specific commit conventions to the built-in prompt
- 📊 **Recent Commits Reference**: Optionally feed the last 10 commit messages to the AI to keep the style consistent
- 📏 **Diff Length Control**: Configurable maximum diff length sent to the AI
- 🌐 **Output Language**: Chinese (default) or English commit messages
- 🖥️ **CLI Tools Sidebar**: One tool window per CLI with optional high-permission startup
- 🔗 **External Editor Jump**: Open the current file in VS Code / Cursor / Antigravity
- 💻 **Windows Optimized**: `.exe`/`.cmd`/`.bat` detection, npm and nvm-windows paths

## 🚀 Quick Start

### 1. Install a CLI Tool

Install and log in to at least one supported CLI, for example:

```bash
# Claude CLI (recommended)
npm install -g @anthropic-ai/claude-code
claude /login

# Codex CLI
npm install -g @openai/codex
codex login
```

### 2. Install the Plugin

- Build it: `./gradlew buildPlugin` produces `build/distributions/generateCommitMessage-1.0.0.zip`
- Open IntelliJ IDEA → `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
- Select the `.zip` file and restart the IDE

### 3. Configure

Open `Settings` → `Tools` → `Generate Commit Message`.

**AI Commit tab**

1. Choose the CLI and model (the model list is fetched from the CLI; you can also type any model name)
2. Pick a thinking level where the CLI supports it
3. Click `Test` to verify the CLI is installed and logged in
4. Optionally add an extra prompt (appended after the built-in prompt, which you can inspect with `View built-in prompt`)
5. Enhanced options:
   - ☑ Include recent commits as a style reference (last 10)
   - Max diff length in characters (default 5000, minimum 500)
   - Output language: Chinese or English

**CLI Tools tab**

- Toggle the sidebar icon for each CLI tool
- Enable high-permission startup flags per tool (for example `claude --permission-mode bypassPermissions`)

### 4. Use

1. Make some code changes
2. Open the VCS commit panel and select the files to include
3. Click the ⚡ **Generate Commit Message** button next to the message box
4. Review or edit the generated message, then commit

The generation runs as a cancellable background task; cancelling or failing restores whatever you had typed.

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

- **IntelliJ IDEA**: 2021.1 or later (Community / Ultimate), the Terminal plugin must be enabled
- **Java**: 17 for building
- **OS**: Windows 10/11, macOS, Linux

## 🔧 Advanced Configuration

### Custom Prompts

Add project-specific requirements in the "Additional prompt" field, for example:

```
Always use the scope "core".
Reference the Jira ticket from the branch name in the footer.
```

### Where settings are stored

- AI Commit: `ai-commit.xml` in the IDE options directory
- CLI Tools: `fs-cli-tools.xml` in the IDE options directory

## 🧑‍💻 Development

```bash
./gradlew compileKotlin        # compile
./gradlew test                 # unit tests
./gradlew buildPlugin          # build/distributions/generateCommitMessage-1.0.0.zip
./gradlew runIde               # start a sandbox IDE with the plugin
```

Design notes live in `docs/superpowers/specs/` and the implementation plan in `docs/superpowers/plans/`.

## 🤝 Contributing

Contributions are welcome. Please open a Pull Request.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Based on [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) by [@l-ning](https://github.com/l-ning)
- Inspired by the AI CLI tools from Anthropic, OpenAI, and others

---

⭐ If you find this project useful, please consider giving it a star!
