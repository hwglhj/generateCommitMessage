# SubMsg - 5 分钟快速上手

> 🚀 最快的方式开始使用 AI 驱动的代码补全和提交信息生成

---

## 📦 第一步：安装插件

### 方法 A：从 Release 下载（推荐）
1. 下载 [SubMsg-2.0.0.zip](https://github.com/hwglhj/submsg/releases/tag/v2.0.0)
2. 打开 IntelliJ IDEA
3. `Settings → Plugins → ⚙️ → Install Plugin from Disk...`
4. 选择下载的 zip 文件
5. 重启 IDEA

### 方法 B：从源码构建
```bash
git clone https://github.com/hwglhj/submsg.git
cd submsg
./gradlew buildPlugin
# 插件在 build/distributions/SubMsg-2.0.0.zip
```

---

## ⚡ 第二步：安装 CLI（推荐使用 Claude）

```bash
# 使用 npm 安装 Claude CLI
npm install -g @anthropic-ai/claude-cli

# 登录（会在浏览器中打开）
claude /login

# 验证安装
claude --version
```

**其他 CLI 选项**：
- Codex: `npm install -g @openai/codex-cli`
- Gemini: `pip install antigravity-cli`
- Grok: `npm install -g grok-cli`

---

## ⚙️ 第三步：配置插件

1. 打开 `Settings → Tools → SubMsg`
2. **AI Commit 标签页**：
   - 使用模式：选择 `CLI 模式`
   - CLI：选择 `Claude`
   - 模型：选择 `claude-sonnet-4-6`
   - 点击 **测试** 验证
3. 点击 **Apply** 保存

---

## 🎯 第四步：使用功能

### 生成 Commit Message

1. 修改一些代码
2. 打开 VCS 提交界面（`Ctrl+K` / `Cmd+K`）
3. 选择要提交的文件
4. 点击 **AI Generate Commit Message** 按钮
5. 等待生成
6. 查看并编辑提交信息
7. 提交代码 ✅

**示例输出**：
```
feat: add user authentication module

- Implement JWT token generation and validation
- Add login and register endpoints
- Create user session management
- Add unit tests for auth service
```

### 使用 AI Inline（可选）

1. 在 `Settings → Tools → SubMsg` 中
2. 切换到 **AI Inline** 标签页
3. 勾选 **启用 AI Inline Reference**
4. 使用模式：选择 `CLI 模式`
5. 配置 CLI 和模型
6. 点击 **测试** 验证
7. 保存配置

现在开始编码时，输入后会自动显示 AI 补全建议！

---

## 🌍 切换语言（可选）

**切换到英文界面**：
1. `Settings → Tools → SubMsg`
2. 语言：选择 `English`
3. 点击 `Apply`
4. 重启 IDEA

**切换回中文**：
- 语言：选择 `中文`

---

## 🎓 更多功能

### API 模式（使用自己的 API）

1. 在 AI Commit 标签页中
2. 使用模式：选择 `API 模式`
3. 点击 **添加** 创建 API 配置：
   - 名称：`OpenAI GPT-4`
   - 提供商：`OpenAI Compatible`
   - Base URL：`https://api.openai.com/v1`
   - API Key：输入你的 key
   - 模型：`gpt-4-turbo`
4. 点击 **测试** 验证
5. 保存并选择该配置

### 自定义提示词

在配置界面的"自定义提示词"框中输入：

```
生成简洁的中文 commit message，遵循以下规范：
- 使用 feat/fix/docs/style/refactor/test/chore 前缀
- 第一行简短描述，不超过 50 字
- 详细说明使用要点列表
- 使用中文
```

---

## 🔍 故障排除

### CLI 检测失败

**Windows**：
```bash
where claude
# 应输出：C:\Users\...\npm\claude.cmd
```

**macOS/Linux**：
```bash
which claude
# 应输出：/usr/local/bin/claude
```

如果检测失败，在配置中手动输入 CLI 完整路径。

### API 连接失败

1. 测试网络：`curl https://api.openai.com/v1/models`
2. 检查 API Key 是否正确
3. 检查 Base URL 是否包含 `/v1`

### AI Inline 不工作

1. 确认已勾选"启用 AI Inline Reference"
2. 尝试手动触发：`Ctrl+Space`
3. 检查触发延迟设置
4. 查看 IDEA 日志：`Help → Show Log in Explorer`

---

## 📚 详细文档

- [完整 README](./README_v2.md) - 详细功能介绍
- [AI Inline 指南](./AI_INLINE_GUIDE.md) - 深入使用教程
- [多语言指南](./MULTILANG_GUIDE.md) - 语言切换说明
- [更新日志](./CHANGELOG_v2.0.md) - 版本更新详情

---

## 💬 获得帮助

- **GitHub Issues**: https://github.com/hwglhj/submsg/issues
- **Discussions**: https://github.com/hwglhj/submsg/discussions
- **Email**: hwglhj@example.com

---

## ⭐ 支持项目

如果 SubMsg 对你有帮助，请：
1. 给项目点个 Star ⭐
2. 分享给其他开发者
3. 提供反馈和建议

---

**现在开始，让 AI 成为你的编码伙伴！🚀**

---

## 🎯 快速链接

| 功能 | 快捷键 | 说明 |
|------|--------|------|
| 打开提交界面 | `Ctrl+K` / `Cmd+K` | 开始提交代码 |
| 触发补全 | `Ctrl+Space` | 手动触发 AI 补全 |
| 打开设置 | `Ctrl+Alt+S` / `Cmd+,` | 配置插件 |

---

## ✅ 完成检查清单

- [ ] 插件已安装
- [ ] CLI 已安装并登录
- [ ] 配置已完成
- [ ] 测试成功
- [ ] 生成了第一个 Commit Message
- [ ] （可选）启用了 AI Inline
- [ ] 阅读了相关文档

**全部完成？恭喜！你已经成功上手 SubMsg！🎉**
