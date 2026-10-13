# Generate Commit Message

[简体中文](README_CN.md) | English

IntelliJ IDEA plugin based on [l-ning/cli-tools-idea](https://github.com/l-ning/cli-tools-idea), maintained by hwglhj.

## Features

- Generate a Git commit message from the changes selected in the commit panel, including untracked files.
- CLI mode: Claude, Codex, Cursor Agent, OpenCode, Gemini/agy, Grok, OMP and Pi. Reuse the CLI's authentication, select or type a model, and choose supported thinking levels. Pi (`pi`) and OMP (`omp`) are separate CLI tools and reuse their own configured providers and credentials.
- API mode: multiple OpenAI-compatible, Anthropic and custom OpenAI-compatible configurations. Each site can keep multiple models; the commit-panel menu switches by site > model. Add, edit, delete and activate configurations; fetch models or enter one manually; test a connection. API keys use IntelliJ PasswordSafe and are excluded from plugin XML settings.
- Export and import all API sites as versioned JSON. API keys are omitted by default; exporting them requires an explicit checkbox and confirmation. Same-ID imports overwrite site fields, while imports without a key preserve the local PasswordSafe key.
- Additional prompt, read-only built-in prompt, recent commit style reference (last 10), configurable diff limit (default 5000, minimum 500).
- Chinese/English interface and independent commit message language. Saving the interface language refreshes the settings page.
- Eight CLI terminal entries with individual visibility controls and high permission switches where the CLI supports one.
- Open the current file at its cursor position in VS Code, Cursor, Antigravity or Windsurf.
- Windows executable and npm launcher detection, npm/nvm PATH support, cancellation and process timeouts.
- A quick model switcher beside the commit-panel lightning action. Opening the API menu refreshes every site's full model list in the background; the active site is marked with a check and its current model. Generation failures open the same list so you can switch and retry.

AI Inline and REST API Search are not part of this plugin.

## Setup

Requires IntelliJ IDEA 2022.2 or later and the bundled Terminal plugin. The build targets platform 222 and Java 17; newer IDE versions still require installation testing.

Install the ZIP from `build/distributions/` using Settings > Plugins > Install Plugin from Disk, then restart the IDE. Configure Settings > Tools > Generate Commit Message, which contains **AI Commit** and **CLI Tools** tabs.

CLI mode requires a separately installed and authenticated CLI. The optional executable path applies to the selected AI Commit CLI. Model candidates may include a public catalog or built-in fallback; the Test button verifies the selected model against the configured CLI. API mode does not require a local CLI. Custom APIs must implement the OpenAI chat-completions protocol; this is not universal provider support. Providers without `/models` can use a manually entered model. The API settings toolbar includes Windows-compatible JSON import/export controls.

High permission options pass each supported CLI's own approval flags. Pi inherits the current process permissions and therefore has no separate high permission switch. These options do not launch Windows as Administrator. API keys follow the IDE's PasswordSafe storage policy, including memory-only mode when configured in the IDE.

## Build On Windows

Use JDK 17 and Gradle 7.4.2 already installed on this machine:

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test --no-daemon
& 'D:\gradle-7.4.2\bin\gradle.bat' buildPlugin --no-daemon
```

The artifact is `build/distributions/generateCommitMessage-1.0.0.zip`. No Git write operations are needed for a local build. Test temporary files use `tmp/tests/`.

See [installation and acceptance](INSTALL_GUIDE.md), [feature map](FEATURE_SUMMARY.md), [verification record](FINAL_SUMMARY.md), and [document index](DOCS_INDEX.md). Build and automated test results do not establish live provider availability or compatibility with every CLI version.

## License

See [LICENSE](LICENSE) and the original project's attribution.
