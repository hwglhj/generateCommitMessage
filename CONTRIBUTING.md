# Contributing to SubMsg

Thank you for your interest in contributing to SubMsg! This document provides guidelines and instructions for contributing.

## 🤝 How to Contribute

### Reporting Bugs

If you find a bug, please create an issue with:

1. **Clear title**: Describe the issue concisely
2. **Environment**: OS, IDEA version, Java version
3. **Steps to reproduce**: Detailed steps to reproduce the issue
4. **Expected behavior**: What you expected to happen
5. **Actual behavior**: What actually happened
6. **Screenshots**: If applicable
7. **Logs**: Relevant error messages or stack traces

### Suggesting Enhancements

Feature requests are welcome! Please include:

1. **Use case**: Why is this feature needed?
2. **Proposed solution**: How should it work?
3. **Alternatives**: Other solutions you've considered
4. **Additional context**: Screenshots, mockups, etc.

### Pull Requests

1. **Fork** the repository
2. **Create a branch** from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** following the coding standards
4. **Test your changes** thoroughly
5. **Commit** with clear messages:
   ```bash
   git commit -m "✨ feat: add new feature"
   ```
6. **Push** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Open a Pull Request** with:
   - Clear description of changes
   - Reference to related issues
   - Screenshots if UI changes

## 💻 Development Setup

### Prerequisites

- **Java 11** or later
- **IntelliJ IDEA 2021.1+**
- **Gradle 7.0+** (included via wrapper)

### Building from Source

```bash
# Clone the repository
git clone https://github.com/hwglhj/submsg.git
cd submsg

# Build the plugin
./gradlew buildPlugin

# Run in sandbox IDE
./gradlew runIde
```

### Project Structure

```
submsg/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/fs/aicommit/
│   │   │       ├── AiCommitConfigurable.kt    # Main settings UI
│   │   │       ├── CliModePanel.kt            # CLI mode panel
│   │   │       ├── ApiModePanel.kt            # API mode panel
│   │   │       ├── ApiConfigDialog.kt         # API config dialog
│   │   │       ├── GenerateCommitMessageAction.kt
│   │   │       ├── AiCommitService.kt         # Core service
│   │   │       ├── SdkManager.kt              # CLI detection
│   │   │       ├── ApiClient.kt               # API client
│   │   │       ├── AiCommitSettings.kt        # Settings storage
│   │   │       ├── AiCommitPrompts.kt         # Prompt templates
│   │   │       └── ApiProviderConfig.kt       # API config model
│   │   └── resources/
│   │       └── META-INF/
│   │           └── plugin.xml
├── build.gradle.kts
├── gradle.properties
└── README.md
```

## 📝 Coding Standards

### Kotlin Style

Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use 4 spaces for indentation
- Class names in PascalCase
- Function names in camelCase
- Constants in UPPER_SNAKE_CASE
- Max line length: 120 characters

### Code Organization

```kotlin
// Good
class MyClass {
    // Constants
    companion object {
        private const val DEFAULT_VALUE = 100
    }
    
    // Properties
    private val settings = AiCommitSettings.getInstance()
    
    // Public methods
    fun publicMethod() { }
    
    // Private methods
    private fun privateMethod() { }
}
```

### Comments

- Use KDoc for public APIs:
  ```kotlin
  /**
   * Generates a commit message from git diff.
   * 
   * @param diff The git diff text
   * @return Generated commit message
   */
  fun generate(diff: String): String
  ```
- Use inline comments sparingly, only when necessary
- Keep comments up to date with code changes

### UI Components

- Use IntelliJ Platform UI components (JBLabel, JBTextField, etc.)
- Use FormBuilder for form layouts
- Apply JBUI.Borders for consistent spacing
- Support both light and dark themes

### Error Handling

```kotlin
// Good: Catch specific exceptions
try {
    apiClient.call()
} catch (e: IOException) {
    LOG.error("API call failed", e)
    notifyUser("Connection error: ${e.message}")
}

// Bad: Catch all exceptions silently
try {
    apiClient.call()
} catch (e: Exception) {
    // Silent fail
}
```

## 🧪 Testing

### Manual Testing Checklist

Before submitting a PR, test:

- [ ] CLI detection on Windows/macOS/Linux
- [ ] Model list fetching
- [ ] API connection testing
- [ ] Commit message generation
- [ ] Settings persistence
- [ ] UI layout in light/dark themes
- [ ] Error handling

### Test in Different Environments

- **Windows 10/11**
- **macOS 10.14+**
- **Ubuntu 20.04+**
- **IntelliJ IDEA 2021.1, 2022.1, 2023.1**

## 🔍 Code Review Process

1. **Automated checks**: Must pass
2. **Code review**: At least one approval required
3. **Testing**: Verify changes work as expected
4. **Documentation**: Update docs if needed

## 📋 Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `✨ feat`: New feature
- `🐛 fix`: Bug fix
- `📝 docs`: Documentation changes
- `💄 style`: Code style changes (formatting, etc.)
- `♻️ refactor`: Code refactoring
- `⚡️ perf`: Performance improvements
- `✅ test`: Adding tests
- `🔧 chore`: Build process, dependencies

### Examples

```
✨ feat(cli): add support for Grok CLI

Add detection and integration for Grok CLI tool.
Includes model fetching and authentication support.

Closes #123
```

```
🐛 fix(windows): fix CLI detection on Windows

- Support .exe, .cmd, .bat extensions
- Use 'where' command instead of 'which'
- Fix PATH separator issue

Fixes #456
```

## 📚 Resources

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Gradle Documentation](https://docs.gradle.org/)

## 🙏 Recognition

Contributors will be recognized in:
- README.md
- Release notes
- GitHub contributors page

## 📞 Questions?

Feel free to:
- Open a [Discussion](https://github.com/hwglhj/submsg/discussions)
- Ask in an [Issue](https://github.com/hwglhj/submsg/issues)

---

Thank you for contributing to SubMsg! 🎉
