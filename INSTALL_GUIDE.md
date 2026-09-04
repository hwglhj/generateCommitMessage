# SubMsg 安装和使用指南

## 📦 安装

### 方式一：从 JetBrains 市场安装（推荐，即将推出）

1. 打开 IntelliJ IDEA
2. 前往 `File` → `Settings` → `Plugins`
3. 切换到 `Marketplace` 标签
4. 搜索 "SubMsg"
5. 点击 `Install` 按钮
6. 重启 IDE

### 方式二：从 GitHub Release 安装

1. 访问 [Releases](https://github.com/hwglhj/submsg/releases) 页面
2. 下载最新版本的 `SubMsg-1.0.0.zip` 文件
3. 打开 IntelliJ IDEA
4. 前往 `File` → `Settings` → `Plugins`
5. 点击右上角 `⚙️` 图标
6. 选择 `Install Plugin from Disk...`
7. 选择下载的 zip 文件
8. 重启 IDE

### 方式三：从源码构建

```bash
# 克隆仓库
git clone https://github.com/hwglhj/submsg.git
cd submsg

# Windows 构建
gradlew.bat buildPlugin

# macOS/Linux 构建
./gradlew buildPlugin

# 构建产物在 build/distributions/ 目录
```

## ⚙️ 配置

### CLI 模式配置

CLI 模式使用本机已安装的 AI CLI 工具，无需配置 API Key。

#### 步骤 1：安装 CLI 工具

**Claude CLI（推荐）**
```bash
# 安装
npm install -g @anthropic-ai/claude-cli

# 登录
claude /login
```

**Codex CLI**
```bash
# 安装
npm install -g @openai/codex-cli

# 登录
codex login
```

**Cursor Agent**
```bash
# 安装
npm install -g @cursor/agent

# 登录
cursor-agent login
```

**Gemini (Antigravity)**
```bash
# 安装
npm install -g @google/antigravity

# 登录
agy login
```

**Grok**
```bash
# 安装
npm install -g @xai/grok-cli

# 登录
grok login
```

#### 步骤 2：在插件中配置

1. 打开 `Settings` → `Tools` → `SubMsg - AI Commit`
2. 在"使用模式"下拉框选择 **CLI 模式**
3. 在"CLI"下拉框选择你安装的 CLI（如 Claude CLI）
4. 在"模型"输入框输入模型名称（如 `claude-sonnet-4-6`）
   - 也可以点击"刷新模型列表"按钮自动获取
5. （可选）选择思考等级（如 `medium`）
6. 点击"测试"按钮验证配置
7. 点击"OK"保存

#### CLI 模式常用模型

| CLI | 推荐模型 |
|-----|----------|
| Claude CLI | `claude-sonnet-4-6`, `claude-opus-4-8` |
| Codex CLI | `gpt-5.5`, `gpt-5.4` |
| Cursor Agent | `auto` |
| Gemini | `Gemini 3.1 Pro (High)` |
| Grok | `grok-4.1` |

### API 模式配置

API 模式直接调用 OpenAI 兼容的 API，需要提供 API Key。

#### 步骤 1：添加 API 配置

1. 打开 `Settings` → `Tools` → `SubMsg - AI Commit`
2. 在"使用模式"下拉框选择 **API 模式**
3. 点击工具栏的 `+` 按钮添加新配置
4. 填写配置信息：
   - **名称**：自定义名称（如 "OpenAI"）
   - **提供商**：选择 "OpenAI Compatible" 或 "Anthropic"
   - **Base URL**：API 地址（必须包含完整路径）
   - **API Key**：你的 API 密钥
   - **模型**：模型名称（或点击"获取模型列表"自动获取）
5. 点击"测试"验证连接
6. 点击"OK"保存

#### 步骤 2：激活配置

1. 在"激活配置"下拉框中选择要使用的配置
2. 点击"测试"确认可用
3. 点击"OK"保存

#### API 配置示例

**OpenAI**
```
名称: OpenAI
提供商: OpenAI Compatible
Base URL: https://api.openai.com/v1
API Key: sk-...
模型: gpt-4o
```

**Anthropic**
```
名称: Anthropic
提供商: Anthropic
Base URL: https://api.anthropic.com/v1
API Key: sk-ant-...
模型: claude-3-5-sonnet-20241022
```

**Azure OpenAI**
```
名称: Azure
提供商: OpenAI Compatible
Base URL: https://your-resource.openai.azure.com/openai/deployments/your-deployment
API Key: your-api-key
模型: gpt-4
```

**本地 LLM（如 Ollama）**
```
名称: Local LLM
提供商: OpenAI Compatible
Base URL: http://localhost:11434/v1
API Key: ollama
模型: llama2
```

### 自定义提示词（可选）

在"附加提示词"文本框中可以添加项目特定的规范：

```
遵循 Angular 提交规范。
使用类型前缀：feat、fix、docs、style、refactor、test、chore。
主题行使用祈使句，首字母小写，不加句号。
body 部分说明改动原因和影响。
footer 部分引用相关 issue。
```

## 🎯 使用

### 生成提交信息

1. **修改代码**：在项目中进行一些代码修改
2. **打开提交窗口**：
   - 方式一：`Ctrl + K`（Windows/Linux）或 `Cmd + K`（macOS）
   - 方式二：右键项目 → `Git` → `Commit`
3. **生成信息**：点击提交信息输入框上方的 **"AI 生成提交信息"** 按钮
4. **等待生成**：插件会分析你的代码变更并生成提交信息
5. **审查编辑**：检查生成的信息，根据需要修改
6. **提交代码**：点击"Commit"或"Commit and Push"

### 生成效果示例

**代码变更**：
```diff
+ public class UserService {
+     public User getUserById(Long id) {
+         return userRepository.findById(id);
+     }
+ }
```

**生成的提交信息**：
```
feat: add user service with getUserById method

- Implement UserService class
- Add getUserById method to fetch user by ID
- Use userRepository for data access
```

### 快捷键（可选）

你可以为"AI 生成提交信息"操作设置快捷键：

1. 打开 `Settings` → `Keymap`
2. 搜索 "AI Generate Commit Message"
3. 右键 → `Add Keyboard Shortcut`
4. 按下你想设置的快捷键（如 `Ctrl + Alt + G`）
5. 点击"OK"

## 🐛 故障排除

### Windows CLI 检测失败

**症状**：插件提示"未检测到 claude 命令"

**解决方案**：

1. 确认 CLI 已安装：
   ```bash
   claude --version
   ```

2. 如果命令未找到，检查 PATH：
   ```bash
   # PowerShell
   $env:PATH -split ';'
   
   # 找到 npm 全局路径
   npm config get prefix
   ```

3. 添加 npm 全局路径到系统 PATH：
   - 通常在 `%APPDATA%\npm`
   - 或 `%USERPROFILE%\AppData\Roaming\npm`

4. 重启 IntelliJ IDEA

5. 如果仍然失败，在插件设置中使用完整路径：
   ```
   C:\Users\你的用户名\AppData\Roaming\npm\claude.cmd
   ```

### 模型列表获取失败

**症状**：点击"刷新模型列表"后显示"未获取到模型列表"

**可能原因**：
1. 网络连接问题
2. CLI 未登录
3. API 访问限制

**解决方案**：

1. 检查网络连接，确保能访问 OpenRouter API
2. 确认 CLI 已登录：
   ```bash
   claude /login
   ```
3. 手动输入模型名称（推荐）：
   - Claude: `claude-sonnet-4-6`
   - Codex: `gpt-5.5`
   - Cursor: `auto`

### API 模式连接失败

**症状**：测试 API 配置时提示连接失败

**解决方案**：

1. 检查 Base URL 格式：
   - ✅ `https://api.openai.com/v1`
   - ❌ `https://api.openai.com`（缺少 `/v1`）
   - ❌ `api.openai.com/v1`（缺少协议）

2. 验证 API Key：
   - OpenAI: 以 `sk-` 开头
   - Anthropic: 以 `sk-ant-` 开头
   - 确保没有多余的空格

3. 检查网络：
   ```bash
   curl https://api.openai.com/v1/models \
     -H "Authorization: Bearer YOUR_API_KEY"
   ```

4. 如果使用代理，确保 IntelliJ IDEA 代理设置正确

### 生成的提交信息质量不佳

**解决方案**：

1. 使用更强大的模型：
   - Claude: `claude-opus-4-8`（最强）
   - OpenAI: `gpt-4o`

2. 添加自定义提示词：
   ```
   生成详细的提交信息，包括：
   1. 简短的标题（50 字符以内）
   2. 详细的改动说明
   3. 改动的原因和影响
   ```

3. 分小批次提交，避免一次提交过多文件

## 💡 最佳实践

1. **小步提交**：每次提交专注于单一功能或修复
2. **审查信息**：始终检查 AI 生成的信息，确保准确
3. **自定义提示词**：根据团队规范调整提示词
4. **选择合适模型**：日常使用 Sonnet，复杂提交使用 Opus
5. **定期更新 CLI**：保持 CLI 工具为最新版本

## 📚 更多资源

- [GitHub 仓库](https://github.com/hwglhj/submsg)
- [问题反馈](https://github.com/hwglhj/submsg/issues)
- [讨论区](https://github.com/hwglhj/submsg/discussions)
- [更新日志](CHANGELOG.md)

---

如有问题，请在 [GitHub Issues](https://github.com/hwglhj/submsg/issues) 反馈。
