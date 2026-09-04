# 👋 欢迎使用 SubMsg！

> AI-powered Git commit message generator for IntelliJ IDEA

## 🎯 这是什么？

SubMsg 是一个 IntelliJ IDEA 插件，使用 AI 自动生成高质量的 Git 提交信息。

支持两种模式：
- **CLI 模式**：使用本地安装的 AI CLI 工具（无需 API Key）
- **API 模式**：直接调用 AI API（需要 API Key）

## ⚡ 快速开始（3 分钟）

### 1. 安装 CLI 工具（以 Claude 为例）

```bash
npm install -g @anthropic-ai/claude-cli
claude /login
```

### 2. 安装插件

1. 下载 [SubMsg-1.0.0.zip](https://github.com/hwglhj/submsg/releases/latest)
2. 打开 IntelliJ IDEA
3. `Settings` → `Plugins` → ⚙️ → `Install Plugin from Disk...`
4. 选择下载的 zip 文件
5. 重启 IDE

### 3. 开始使用

1. 修改代码
2. `Git` → `Commit`
3. 点击 **"Generate Commit Message"** 按钮 ✨

完成！AI 会自动生成提交信息。

## 📚 文档导航

### 👤 我是用户

**新手入门**：
1. [快速参考](QUICK_REFERENCE.md) - 5 分钟上手
2. [安装指南](INSTALL_GUIDE.md) - 详细安装和配置
3. [README 中文版](README_CN.md) - 完整功能介绍

**遇到问题**：
- 查看 [故障排除](INSTALL_GUIDE.md#故障排除)
- 提交 [Issue](https://github.com/hwglhj/submsg/issues)

### 👨‍💻 我是开发者

**了解项目**：
1. [项目结构](PROJECT_STRUCTURE.md) - 代码组织和架构
2. [开发文档](DEVELOPMENT.md) - API 和技术细节
3. [贡献指南](CONTRIBUTING.md) - 如何参与贡献

**准备开发**：
```bash
git clone https://github.com/hwglhj/submsg.git
cd submsg
./gradlew buildPlugin
```

### 🚀 我要发布/维护

1. [发布指南](PUBLISH_GUIDE.md) - 发布到 GitHub
2. [发布检查清单](RELEASE_CHECKLIST.md) - 发布前检查
3. [项目总结](PROJECT_SUMMARY.md) - 完整的项目信息

## ✨ 核心特性

- 🤖 **AI 驱动**：使用最新的 AI 模型生成提交信息
- 🔧 **双模式**：CLI（无需 API Key）+ API（灵活配置）
- 💻 **Windows 优化**：完美支持 Windows 10/11
- 🌍 **跨平台**：Windows、macOS、Linux
- 🎨 **优雅界面**：重新设计的配置界面
- 📝 **自定义提示词**：按团队规范定制
- 🚀 **开箱即用**：安装即可使用

## 🛠️ 支持的工具

### CLI 模式（7 种）

| CLI | 安装命令 |
|-----|---------|
| Claude | `npm i -g @anthropic-ai/claude-cli` |
| Codex | `npm i -g @openai/codex-cli` |
| Cursor | 下载 Cursor Agent |
| Gemini | `npm i -g @antigravity/agy` |
| Grok | `npm i -g @x-ai/grok-cli` |
| OMP | 参考 OMP 文档 |
| OpenCode | 参考 OpenCode 文档 |

### API 模式（无限）

支持任何 OpenAI 兼容的 API：
- OpenAI (GPT-4, GPT-3.5)
- Anthropic (Claude)
- Azure OpenAI
- 自建 API 服务
- ...

## 🎬 使用演示

```bash
# 1. 修改代码
echo "console.log('hello');" > test.js
git add test.js

# 2. 打开提交对话框
# Git → Commit (Ctrl+K)

# 3. 点击 "Generate Commit Message" 按钮

# 4. AI 自动生成：
# "feat: add console log to test.js"
```

## 💡 最佳实践

1. **小步提交**：每次只提交一个逻辑变更
2. **清晰的 diff**：确保代码变更易于理解
3. **审查消息**：检查 AI 生成的消息是否准确
4. **自定义提示词**：根据团队规范调整

## 🐛 遇到问题？

### 常见问题

**Q: 显示"未检测到 CLI"**

A: 检查 CLI 是否已安装并在 PATH 中：
```bash
# Windows
where claude

# macOS/Linux
which claude
```

**Q: API 调用失败**

A: 检查：
- API Key 是否正确
- Base URL 是否正确
- 网络连接是否正常
- 点击"测试"按钮验证

**更多问题**：查看 [完整故障排除指南](INSTALL_GUIDE.md#故障排除)

## 🤝 参与贡献

欢迎贡献！

1. Fork 项目
2. 创建分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

详见 [贡献指南](CONTRIBUTING.md)

## 📄 开源协议

本项目基于 [MIT License](LICENSE) 开源。

基于 [@l-ning](https://github.com/l-ning) 的 [cli-tools-idea](https://github.com/l-ning/cli-tools-idea) 项目修改。

## 🔗 链接

- **GitHub**: https://github.com/hwglhj/submsg
- **Issues**: https://github.com/hwglhj/submsg/issues
- **Releases**: https://github.com/hwglhj/submsg/releases

## 📞 联系方式

- **作者**: hwglhj
- **邮箱**: hwglhj@example.com

---

## 📋 文档索引

### 用户文档
- [README (English)](README.md)
- [README (中文)](README_CN.md)
- [快速参考](QUICK_REFERENCE.md)
- [安装指南](INSTALL_GUIDE.md)
- [更新日志](CHANGELOG.md)

### 开发者文档
- [项目结构](PROJECT_STRUCTURE.md)
- [开发文档](DEVELOPMENT.md)
- [贡献指南](CONTRIBUTING.md)

### 项目管理
- [项目总结](PROJECT_SUMMARY.md)
- [完成报告](PROJECT_COMPLETE.md)
- [发布指南](PUBLISH_GUIDE.md)
- [发布检查清单](RELEASE_CHECKLIST.md)

---

**版本**: 1.0.0  
**更新**: 2024-09-03  
**状态**: ✅ 生产就绪

🎉 **开始使用 SubMsg，让 AI 帮你写提交信息！**
