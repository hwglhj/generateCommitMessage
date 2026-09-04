# Changelog

All notable changes to the SubMsg project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-09-03

### Added
- Initial release of SubMsg
- AI-powered Git commit message generation
- Dual mode support: CLI mode and API mode
- Support for 7 CLI tools: Claude CLI, Codex CLI, Cursor Agent, OpenCode, Gemini (agy), Grok, OMP
- Custom prompt configuration
- Windows-optimized CLI detection
- Model list auto-fetch from OpenRouter
- Connection testing for both CLI and API modes
- Thinking level support for compatible CLIs

### Changed
- Complete UI redesign with improved layout
- Refactored from cli-tools-idea 2.0.7
- Removed unnecessary features (terminal, inline edit, etc.)
- Simplified codebase focusing on core commit message generation

### Fixed
- Windows CLI detection issues (path detection, executable extensions)
- Windows PATH separator handling (`;` vs `:`)
- UI layout issues in settings panel
- API configuration dialog display issues

## [Unreleased]

### Planned
- JetBrains Marketplace publication
- Support for more AI providers
- Commit message templates
- Multi-language UI support
- Batch commit message generation
- Commit message history
- Custom commit message format templates

---

## Credits

Based on [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) by [@l-ning](https://github.com/l-ning)

## Links

- [GitHub Repository](https://github.com/hwglhj/submsg)
- [Issue Tracker](https://github.com/hwglhj/submsg/issues)
- [Discussions](https://github.com/hwglhj/submsg/discussions)
