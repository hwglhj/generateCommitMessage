# Changelog

All notable changes to the Generate Commit Message plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-10-16

### Added
- Persistent per-site API model caches with model count and last-success time
- Independent model refresh actions in the quick popup and API settings page
- In-place quick-popup list replacement after a successful refresh

### Changed
- Opening the API quick switcher now reads the saved cache immediately instead of refreshing every site
- Successful refreshes replace stale cache entries with the current model and the latest remote result

### Fixed
- Failed, cancelled, empty and stale refresh results no longer discard the previous cache
- Switching models while a refresh is running no longer restores the request's old model
- Quick model popup class loading on IntelliJ IDEA 2026.1 after removal of the obsolete `KeepingPopupOpenAction` internal API

## [1.0.0] - 2026-09-05

### Added
- AI-powered Git commit message generation from the changes selected in the commit panel
- Support for 7 CLI tools: Claude CLI, Codex CLI, Cursor Agent, OpenCode, Gemini (agy), Grok, OMP
- Unified settings page: `Settings → Tools → Generate Commit Message` with `AI Commit` and `CLI Tools` tabs
- Enhanced AI Commit options: recent-commit style reference (last 10), max diff length, output language (Chinese / English)
- Additional prompt appended to the built-in prompt, with a read-only viewer for the built-in prompt
- CLI Tools sidebar: one tool window per CLI, per-tool icon toggle and high-permission startup flags
- External editor jump action (VS Code / Cursor / Antigravity)
- Model list fetching and connection test per CLI, thinking level support for compatible CLIs
- Unit tests for prompt assembly and settings migration

### Changed
- Project renamed from cli-tools-idea / SubMsg to Generate Commit Message (plugin id `com.fs.generateCommitMessage`)
- Build requires JDK 17

### Removed
- API Search feature
- AI Inline (inline edit / inline completion) feature

### Fixed
- Windows CLI detection issues (path detection, executable extensions)
- Windows PATH separator handling (`;` vs `:`)

---

## Credits

Based on [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) by [@l-ning](https://github.com/l-ning)
