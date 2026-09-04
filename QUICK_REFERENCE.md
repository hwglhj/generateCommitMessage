# 🚀 SubMsg 快速参考

## 安装（3 步）

```bash
# 1. 安装 CLI（以 Claude 为例）
npm install -g @anthropic-ai/claude-cli
claude /login

# 2. 在 IDEA 中安装插件
Settings → Plugins → Install from Disk → 选择 SubMsg-1.0.0.zip

# 3. 配置插件
Settings → Other Settings → AI Commit 设置
```

## 使用（1 步）

```
修改代码 → Git → Commit → 点击 "Generate Commit Message" 按钮 ✨
```

或使用快捷键：`Ctrl + Alt + G`

## 两种模式

### CLI 模式（推荐）
- ✅ 无需 API Key
- ✅ 使用本地 CLI 工具
- ✅ 支持：Claude、Codex、Cursor、OpenCode、Gemini、Grok、OMP

### API 模式
- ✅ 支持任何 OpenAI 兼容 API
- ✅ 需要配置 API Key
- ✅ 可配置多个 API 提供商

## 故障排除

### 问题：显示"未检测到 CLI"

**解决方案**：

```bash
# Windows: 检查 CLI 是否在 PATH 中
where claude

# 如果没找到，重新安装
npm install -g @anthropic-ai/claude-cli

# 或者在插件设置中指定完整路径
C:\Users\你的用户名\AppData\Roaming\npm\claude.cmd
```

### 问题：API 调用失败

**解决方案**：

1. 检查 API Key 是否正确
2. 检查 Base URL 是否正确
3. 检查网络连接
4. 点击"测试"按钮验证配置

## 支持的 CLI 工具

| CLI | 安装命令 | 登录命令 |
|-----|---------|----------|
| Claude | `npm i -g @anthropic-ai/claude-cli` | `claude /login` |
| Codex | `npm i -g @openai/codex-cli` | `codex login` |
| Cursor | 下载安装 Cursor Agent | `cursor-agent login` |
| Gemini | `npm i -g @antigravity/agy` | `agy login` |
| Grok | `npm i -g @x-ai/grok-cli` | `grok login` |
| OMP | 参考 OMP 文档 | - |
| OpenCode | 参考 OpenCode 文档 | - |

## 常用设置

### 自定义提示词

在"附加提示词"中添加：

```
请使用以下格式生成提交信息：

<类型>(<范围>): <主题>

<详细描述>

类型：feat, fix, docs, style, refactor, test, chore
```

### 思考等级

- **跟随默认**：使用 CLI 默认设置
- **low**：快速响应，简单场景
- **medium**：平衡速度和质量
- **high**：深度思考，复杂场景
- **xhigh/max**：最深度思考

## 快捷键

| 操作 | Windows/Linux | macOS |
|------|---------------|-------|
| 生成提交信息 | `Ctrl + Alt + G` | `Cmd + Alt + G` |
| 打开设置 | `Ctrl + Alt + S` | `Cmd + ,` |

## 提交信息格式

默认遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```
<type>: <subject>

[optional body]

[optional footer]
```

**类型**：
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式（不影响代码运行）
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

## 最佳实践

1. **小步提交**：每次提交只包含一个逻辑变更
2. **有意义的 diff**：确保 diff 清晰表达了变更内容
3. **审查生成的消息**：AI 生成后，检查是否准确
4. **自定义提示词**：根据团队规范调整提示词

## 文档链接

- 📖 完整安装指南：`INSTALL_GUIDE.md`
- 🏗️ 开发文档：`DEVELOPMENT.md`
- 🤝 贡献指南：`CONTRIBUTING.md`
- 🚀 发布指南：`PUBLISH_GUIDE.md`

## 获取帮助

- 🐛 报告问题：https://github.com/hwglhj/submsg/issues
- 💬 讨论：https://github.com/hwglhj/submsg/discussions
- 📧 邮件：hwglhj@example.com

---

**版本**: 1.0.0  
**更新**: 2024-09-03

💡 提示：更多详细信息请查看完整文档！
