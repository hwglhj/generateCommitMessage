# AI Inline Reference 使用指南

## 📖 简介

AI Inline Reference 是 SubMsg v2.0 引入的全新功能，提供实时的 AI 驱动代码补全建议。

### 主要特点
- 🚀 **实时补全**：输入代码时自动提供智能建议
- 🌐 **全语言支持**：适用于所有编程语言
- 🔧 **灵活配置**：支持 CLI 和 API 两种模式
- 🎯 **上下文感知**：分析光标前后代码，提供精准建议
- ✨ **自定义提示词**：可配置专属的补全风格

---

## 🚀 快速开始

### 1. 启用功能

1. 打开 `Settings → Tools → SubMsg`
2. 切换到 **AI Inline** 标签页
3. 勾选 **启用 AI Inline Reference**
4. 点击 `Apply` 保存

### 2. 选择模式

#### CLI 模式（推荐）
- ✅ 无需 API Key
- ✅ 使用本地 CLI 工具
- ✅ 隐私保护更好

**配置步骤**：
1. 选择 **使用模式** → `CLI`
2. 选择已安装的 CLI（如 Claude CLI）
3. 选择模型（如 `claude-sonnet-4-6`）
4. 点击 **测试** 验证配置

#### API 模式
- ✅ 支持多种 API 提供商
- ✅ 灵活切换不同配置
- ✅ 可使用自定义 API

**配置步骤**：
1. 选择 **使用模式** → `API`
2. 点击 **添加** 创建新的 API 配置
3. 填写配置信息（名称、Base URL、API Key、模型）
4. 点击 **测试** 验证连接
5. 在下拉框中选择激活的配置

---

## ⚙️ 详细配置

### 基本设置

| 配置项 | 说明 | 推荐值 |
|--------|------|--------|
| 启用 AI Inline Reference | 是否启用补全功能 | ✅ 启用 |
| 使用模式 | CLI / API | CLI（更简单） |
| 触发延迟 | 输入后多久触发（毫秒） | 500-1000ms |
| 最大 Token 数 | 单次补全的最大长度 | 100-200 |

### CLI 模式配置

#### 支持的 CLI
| CLI | 命令 | 推荐模型 |
|-----|------|----------|
| Claude CLI | `claude` | claude-sonnet-4-6 |
| Codex CLI | `codex` | gpt-5.5 |
| Cursor Agent | `cursor-agent` | auto |
| Gemini CLI | `agy` | Gemini 3.1 Pro (High) |
| Grok CLI | `grok` | grok-4.1 |
| OMP CLI | `omp` | 按配置 |

#### 安装 CLI 示例

**Claude CLI**（推荐）：
```bash
# 使用 npm 安装
npm install -g @anthropic-ai/claude-cli

# 登录
claude /login

# 验证
claude --version
```

**Codex CLI**：
```bash
npm install -g @openai/codex-cli
codex login
```

**Gemini CLI (agy)**：
```bash
pip install antigravity-cli
agy login
```

### API 模式配置

#### API 配置表单

```
名称：         [例如：OpenAI GPT-4          ]
提供商：       [OpenAI Compatible ▼        ]
Base URL：    [https://api.openai.com/v1   ]
API Key：     [sk-...                      ]
模型：         [gpt-4                      ] [获取模型列表]
推理兼容：     [□] 推理模型兼容性
```

#### 常见 API 配置

**OpenAI**：
```
名称：OpenAI GPT-4
提供商：OpenAI Compatible
Base URL：https://api.openai.com/v1
API Key：sk-your-api-key
模型：gpt-4-turbo
```

**Anthropic Claude**：
```
名称：Anthropic Claude
提供商：Anthropic
Base URL：https://api.anthropic.com/v1
API Key：sk-ant-your-api-key
模型：claude-3-5-sonnet-20241022
推理兼容：✅
```

**DeepSeek**：
```
名称：DeepSeek Coder
提供商：OpenAI Compatible
Base URL：https://api.deepseek.com/v1
API Key：your-deepseek-key
模型：deepseek-coder
```

**Moonshot AI**：
```
名称：Moonshot
提供商：OpenAI Compatible
Base URL：https://api.moonshot.cn/v1
API Key：your-moonshot-key
模型：moonshot-v1-8k
```

**本地 Ollama**：
```
名称：本地 Ollama
提供商：OpenAI Compatible
Base URL：http://localhost:11434/v1
API Key：ollama
模型：codellama:7b
```

**LM Studio**：
```
名称：LM Studio
提供商：OpenAI Compatible
Base URL：http://localhost:1234/v1
API Key：lm-studio
模型：local-model
```

---

## 🎨 自定义提示词

### 默认提示词
```
You are a professional code completion assistant.
Analyze the code context and provide accurate completions.

Requirements:
- Keep completions concise and relevant
- Follow the project's coding style
- Prioritize readability
```

### 自定义示例

**Python 项目**：
```
You are a Python code assistant following PEP 8 standards.

Context: This is a Django web application project.

Guidelines:
- Use type hints for all functions
- Add docstrings for classes and functions
- Follow Django best practices
- Prefer list comprehensions over loops
```

**Java 企业项目**：
```
You are a Java enterprise application code assistant.

Context: Spring Boot microservice with clean architecture.

Guidelines:
- Use proper design patterns (Factory, Builder, etc.)
- Add comprehensive JavaDoc comments
- Follow SOLID principles
- Prefer immutable objects
- Use Optional instead of null
```

**前端 React 项目**：
```
You are a React/TypeScript code assistant.

Context: Modern React application using hooks and functional components.

Guidelines:
- Use TypeScript strict mode
- Prefer functional components with hooks
- Follow React best practices
- Use proper prop types
- Keep components small and focused
```

---

## 💡 使用技巧

### 1. 触发补全
- **自动触发**：输入代码后等待触发延迟时间
- **手动触发**：按 `Ctrl+Space`（Windows/Linux）或 `Cmd+Space`（macOS）
- **强制刷新**：按 `Ctrl+Shift+Space` 重新获取建议

### 2. 选择建议
- **键盘**：使用 `↑` `↓` 选择，`Enter` 确认
- **鼠标**：直接点击建议项
- **预览**：悬停在建议上查看完整内容

### 3. 优化建议质量

#### 提供足够的上下文
```kotlin
// ❌ 上下文不足
fun process

// ✅ 提供清晰上下文
data class User(val id: Int, val name: String)

fun processUser
```

#### 使用有意义的命名
```python
# ❌ 命名不清晰
def calc(a, b):

# ✅ 命名清晰
def calculate_total_price(items: List[Item], discount_rate: float):
```

#### 添加注释说明意图
```java
// ❌ 无注释
public void handle

// ✅ 有注释说明
/**
 * Handle user authentication request
 * @param username the username
 * @param password the password
 */
public void handleAuthentication
```

### 4. 性能优化

#### 调整触发延迟
- **高性能机器**：300-500ms
- **普通机器**：500-1000ms
- **低性能机器**：1000-2000ms

#### 减少 Token 数
- **快速补全**：50-100 tokens
- **标准补全**：100-200 tokens
- **详细补全**：200-500 tokens

---

## 🔍 故障排除

### 问题 1：补全不工作

**症状**：输入代码后没有任何建议

**排查步骤**：
1. 检查 AI Inline 是否启用
   - `Settings → Tools → SubMsg → AI Inline`
   - 确认 "启用 AI Inline Reference" 已勾选

2. 检查 CLI/API 配置
   - 点击 **测试** 按钮验证
   - 查看 IDEA 日志：`Help → Show Log in Explorer`

3. 检查网络连接（API 模式）
   - 确保可以访问 API 地址
   - 检查防火墙设置

**解决方案**：
```bash
# 重新安装 CLI（CLI 模式）
npm install -g @anthropic-ai/claude-cli
claude /login

# 测试 API 连接（API 模式）
curl -H "Authorization: Bearer YOUR_API_KEY" \
     https://api.openai.com/v1/models
```

### 问题 2：建议质量差

**症状**：AI 提供的建议不相关或不准确

**解决方案**：
1. **优化提示词**：添加项目特定的规范和要求
2. **提供更多上下文**：在光标位置前后添加更多相关代码
3. **切换模型**：尝试更强大的模型（如 GPT-4, Claude Opus）
4. **增加 Token 数**：允许 AI 生成更长的补全

### 问题 3：响应太慢

**症状**：等待很久才出现建议

**解决方案**：
1. **增加触发延迟**：1000-2000ms
2. **减少 Token 数**：50-100 tokens
3. **切换更快的模型**：
   - Claude: `claude-haiku-4-5`（快）
   - OpenAI: `gpt-3.5-turbo`（快）
   - Gemini: `Gemini 3.5 Flash`（快）
4. **使用本地模型**：Ollama + 小模型

### 问题 4：API 配额不足

**症状**：API 返回 429 错误或配额已用完

**解决方案**：
1. **检查 API 配额**：登录提供商控制台查看
2. **切换到 CLI 模式**：使用本地 CLI 无配额限制
3. **添加多个 API 配置**：在不同配置间切换
4. **增加触发延迟**：减少 API 调用频率

---

## 📊 最佳实践

### 1. 工作流建议

**场景 A：编写新功能**
1. 先写函数签名和注释
2. 让 AI 补全函数体
3. 审查并修改 AI 建议
4. 添加测试用例

**场景 B：重构代码**
1. 添加目标注释说明重构意图
2. 逐个函数让 AI 提供重构建议
3. 对比原实现和 AI 建议
4. 选择最优方案

**场景 C：学习新技术**
1. 在注释中描述想要实现的功能
2. 让 AI 提供实现示例
3. 学习 AI 使用的模式和技巧
4. 自己实现并对比

### 2. 团队协作

**统一配置**
- 导出配置：`Settings → Tools → SubMsg → Export Settings`
- 分享给团队：将配置文件加入代码仓库
- 团队成员导入：`Import Settings`

**统一提示词**
```
项目名称：XYZ 企业管理系统

技术栈：
- 后端：Spring Boot 3 + Java 17
- 前端：React 18 + TypeScript 5
- 数据库：PostgreSQL 15

编码规范：
- 使用阿里巴巴 Java 编码规范
- 前端遵循 Airbnb React 规范
- 所有函数必须有文档注释
- 优先使用函数式编程风格
```

### 3. 隐私保护

**敏感项目建议**
- ✅ 使用本地 CLI
- ✅ 使用私有部署的 API
- ✅ 避免补全包含敏感信息的代码
- ✅ 定期审查日志

**不推荐**
- ❌ 在公共 API 中补全包含密钥的代码
- ❌ 补全包含用户隐私数据的代码
- ❌ 在敏感项目中使用未知的 API 服务

---

## 🎓 示例

### 示例 1：Kotlin 数据类

**输入**：
```kotlin
// 用户实体类
data class User
```

**AI 建议**：
```kotlin
data class User(
    val id: Long,
    val username: String,
    val email: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

### 示例 2：Python 异步函数

**输入**：
```python
# 异步获取用户信息
async def fetch_user
```

**AI 建议**：
```python
async def fetch_user(user_id: int) -> Optional[User]:
    """
    异步获取用户信息
    
    Args:
        user_id: 用户 ID
        
    Returns:
        User 对象，如果不存在返回 None
    """
    async with httpx.AsyncClient() as client:
        response = await client.get(f"/api/users/{user_id}")
        if response.status_code == 200:
            return User(**response.json())
        return None
```

### 示例 3：React 组件

**输入**：
```typescript
// 用户列表组件
const UserList: React.FC
```

**AI 建议**：
```typescript
const UserList: React.FC<UserListProps> = ({ users, onUserClick }) => {
  return (
    <div className="user-list">
      {users.map(user => (
        <div 
          key={user.id} 
          className="user-item"
          onClick={() => onUserClick(user)}
        >
          <h3>{user.name}</h3>
          <p>{user.email}</p>
        </div>
      ))}
    </div>
  );
};
```

---

## 📚 相关资源

### 官方文档
- [SubMsg GitHub](https://github.com/hwglhj/submsg)
- [更新日志](./CHANGELOG_v2.0.md)
- [多语言指南](./MULTILANG_GUIDE.md)

### CLI 工具文档
- [Claude CLI](https://docs.anthropic.com/claude/docs/claude-cli)
- [OpenAI Codex](https://platform.openai.com/docs/guides/codex)
- [Cursor Agent](https://cursor.sh/docs)

### API 提供商
- [OpenAI API](https://platform.openai.com/)
- [Anthropic API](https://console.anthropic.com/)
- [DeepSeek](https://platform.deepseek.com/)
- [Moonshot AI](https://platform.moonshot.cn/)

---

## 💬 反馈

如有问题或建议，欢迎：
- 提交 GitHub Issue
- 发送邮件至 hwglhj@example.com
- 参与讨论和改进

---

**祝编码愉快！🚀**
