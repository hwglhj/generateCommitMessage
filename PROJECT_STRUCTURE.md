# SubMsg 项目结构

## 📁 完整目录结构

```
submsg/
├── .github/
│   └── workflows/
│       ├── build.yml              # CI 自动构建
│       └── release.yml            # CD 自动发布
│
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── com/
│       │       └── fs/
│       │           └── aicommit/
│       │               ├── AiCommitConfigurable.kt       # 主设置界面
│       │               ├── CliModePanel.kt               # CLI 配置面板
│       │               ├── ApiModePanel.kt               # API 配置面板
│       │               ├── ApiConfigDialog.kt            # API 配置对话框
│       │               ├── GenerateCommitMessageAction.kt # 生成动作
│       │               ├── AiCommitService.kt            # 核心服务
│       │               ├── SdkManager.kt                 # CLI 管理器
│       │               ├── ApiClient.kt                  # API 客户端
│       │               ├── AiCommitSettings.kt           # 设置存储
│       │               ├── AiCommitPrompts.kt            # 提示词模板
│       │               └── ApiProviderConfig.kt          # API 配置模型
│       │
│       └── resources/
│           ├── META-INF/
│           │   ├── plugin.xml                   # 插件配置
│           │   ├── pluginIcon.svg               # 插件图标（亮色）
│           │   └── pluginIcon_dark.svg          # 插件图标（暗色）
│           │
│           └── icons/
│               ├── claude.svg                   # Claude 图标
│               ├── codex.svg                    # Codex 图标
│               ├── cursor.svg                   # Cursor 图标
│               ├── opencode.svg                 # OpenCode 图标（亮色）
│               ├── opencode_dark.svg            # OpenCode 图标（暗色）
│               ├── antigravity.svg              # Gemini 图标
│               ├── grok.svg                     # Grok 图标
│               └── omp.svg                      # OMP 图标
│
├── .gitignore                     # Git 忽略文件
├── LICENSE                        # MIT 开源协议
├── README.md                      # 项目介绍（英文）
├── README_CN.md                   # 项目介绍（中文）
├── INSTALL_GUIDE.md               # 安装使用指南
├── DEVELOPMENT.md                 # 开发文档
├── CONTRIBUTING.md                # 贡献指南
├── CHANGELOG.md                   # 更新日志
├── PROJECT_SUMMARY.md             # 项目完成总结
├── PUBLISH_GUIDE.md               # GitHub 发布指南
├── PROJECT_STRUCTURE.md           # 本文件
│
├── build.gradle.kts               # Gradle 构建配置
├── settings.gradle.kts            # Gradle 设置
├── gradle.properties              # Gradle 属性
├── gradlew                        # Gradle 包装器（Unix）
└── gradlew.bat                    # Gradle 包装器（Windows）
```

## 📊 文件统计

### 源代码文件（11 个）

| 文件 | 行数 | 描述 |
|------|------|------|
| AiCommitConfigurable.kt | ~150 | 主设置界面，模式切换 |
| CliModePanel.kt | ~200 | CLI 配置面板 |
| ApiModePanel.kt | ~220 | API 配置面板 |
| ApiConfigDialog.kt | ~180 | API 配置对话框 |
| GenerateCommitMessageAction.kt | ~280 | 生成提交信息动作 |
| AiCommitService.kt | ~800 | 核心业务逻辑 |
| SdkManager.kt | ~620 | CLI 检测和管理 |
| ApiClient.kt | ~200 | API 客户端 |
| AiCommitSettings.kt | ~150 | 设置持久化 |
| AiCommitPrompts.kt | ~180 | 提示词模板 |
| ApiProviderConfig.kt | ~35 | API 配置数据类 |
| **总计** | **~3,015** | |

### 文档文件（11 个）

| 文件 | 字数 | 描述 |
|------|------|------|
| README.md | ~2,000 | 英文项目介绍 |
| README_CN.md | ~2,500 | 中文项目介绍 |
| INSTALL_GUIDE.md | ~5,000 | 详细安装使用指南 |
| DEVELOPMENT.md | ~6,000 | 开发文档和架构 |
| CONTRIBUTING.md | ~3,000 | 贡献指南 |
| CHANGELOG.md | ~500 | 版本更新日志 |
| PROJECT_SUMMARY.md | ~4,000 | 项目完成总结 |
| PUBLISH_GUIDE.md | ~3,000 | GitHub 发布指南 |
| PROJECT_STRUCTURE.md | ~1,000 | 项目结构说明 |
| LICENSE | ~200 | MIT 开源协议 |
| plugin.xml | ~100 | 插件元数据 |
| **总计** | **~27,300** | |

### 配置文件（7 个）

| 文件 | 描述 |
|------|------|
| build.gradle.kts | Gradle 构建脚本 |
| settings.gradle.kts | Gradle 项目设置 |
| gradle.properties | Gradle 属性配置 |
| .gitignore | Git 忽略规则 |
| build.yml | GitHub Actions 构建 |
| release.yml | GitHub Actions 发布 |
| gradlew / gradlew.bat | Gradle 包装器 |

### 资源文件（10 个）

| 类型 | 数量 | 文件 |
|------|------|------|
| 插件图标 | 2 | pluginIcon.svg, pluginIcon_dark.svg |
| CLI 图标 | 8 | claude, codex, cursor, opencode, antigravity, grok, omp |

## 🏗️ 架构层次

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Configurable │  │ CliModePanel │  │ ApiModePanel │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                 │           │
│         └─────────────────┴─────────────────┘           │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                   Action Layer                          │
│              ┌──────────────────────────┐               │
│              │ GenerateCommitMessageAction│             │
│              └────────────┬─────────────┘               │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                  Service Layer                          │
│   ┌─────────────────┐  ┌──────────────┐                │
│   │ AiCommitService │  │  SdkManager  │                │
│   │                 │  │              │                │
│   │  ┌──────────┐   │  │  ┌────────┐ │                │
│   │  │ CLI Mode │   │  │  │Detect  │ │                │
│   │  │          ├───┼──┼─→│Execute │ │                │
│   │  └──────────┘   │  │  └────────┘ │                │
│   │                 │  └──────────────┘                │
│   │  ┌──────────┐   │                                  │
│   │  │ API Mode │   │  ┌──────────────┐               │
│   │  │          ├───┼─→│  ApiClient   │               │
│   │  └──────────┘   │  └──────────────┘               │
│   └─────────────────┘                                  │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                Configuration Layer                      │
│  ┌────────────┐  ┌────────────┐  ┌─────────────────┐   │
│  │  Settings  │  │  Prompts   │  │ ProviderConfig  │   │
│  └────────────┘  └────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## 🔄 数据流

### CLI 模式
```
用户点击 "Generate Commit Message"
    ↓
GenerateCommitMessageAction.actionPerformed()
    ↓
AiCommitService.generateCommitMessage()
    ↓
获取 Git diff
    ↓
构建提示词（System + User + 自定义）
    ↓
SdkManager.executeCliCommand()
    ↓
    • 检测 CLI 路径
    • 准备命令参数
    • 设置环境变量（PATH）
    • 执行进程
    • 读取输出
    ↓
解析返回结果
    ↓
显示在提交对话框
```

### API 模式
```
用户点击 "Generate Commit Message"
    ↓
GenerateCommitMessageAction.actionPerformed()
    ↓
AiCommitService.generateCommitMessage()
    ↓
获取 Git diff
    ↓
构建提示词
    ↓
ApiClient.generateMessage()
    ↓
    • 读取 API 配置
    • 构建 HTTP 请求
    • 发送 POST 请求
    • 处理响应
    ↓
解析 JSON 响应
    ↓
提取生成的消息
    ↓
显示在提交对话框
```

## 🎯 核心职责

### UI 层

**AiCommitConfigurable**
- 创建设置页面
- 管理 CLI/API 模式切换
- 协调子面板
- 保存/重置设置

**CliModePanel**
- CLI 选择和配置
- 模型名称输入
- 思考等级选择
- CLI 状态检测
- 模型列表获取

**ApiModePanel**
- API 配置列表管理
- 添加/删除/编辑配置
- 激活配置选择
- 连接测试

**ApiConfigDialog**
- API 配置表单
- 提供商选择
- URL/Key/Model 输入
- 表单验证

### Action 层

**GenerateCommitMessageAction**
- 注册到 VCS 菜单
- 触发提交信息生成
- 显示进度
- 处理错误
- 填充到提交对话框

### Service 层

**AiCommitService**
- 核心业务逻辑
- Git diff 获取
- 提示词构建
- CLI/API 调用路由
- 结果解析和格式化

**SdkManager**
- CLI 工具检测
- 版本信息读取
- 命令执行
- 路径搜索（Windows 优化）
- 模型列表获取

**ApiClient**
- HTTP 请求封装
- OpenAI 格式支持
- 错误处理
- 超时控制

### Configuration 层

**AiCommitSettings**
- 持久化存储
- 配置读写
- 默认值管理
- 序列化/反序列化

**AiCommitPrompts**
- 系统提示词
- 用户提示词模板
- 提示词格式化

**ApiProviderConfig**
- API 配置数据类
- JSON 序列化支持

## 📦 构建产物

执行 `./gradlew buildPlugin` 后生成：

```
build/
├── distributions/
│   └── SubMsg-1.0.0.zip          # 插件安装包
│
├── libs/
│   └── SubMsg-1.0.0.jar          # 编译后的 JAR
│
└── reports/
    ├── pluginVerifier/            # 插件验证报告
    └── tests/                     # 测试报告（如有）
```

## 🔌 插件扩展点

在 `plugin.xml` 中注册：

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- 设置页面 -->
    <projectConfigurable
        instance="com.fs.aicommit.AiCommitConfigurable"
        id="aicommit.settings"
        displayName="AI Commit 设置"/>
    
    <!-- 设置持久化 -->
    <projectService
        serviceImplementation="com.fs.aicommit.AiCommitSettings"/>
</extensions>

<actions>
    <!-- VCS 菜单动作 -->
    <action
        id="GenerateCommitMessage"
        class="com.fs.aicommit.GenerateCommitMessageAction"
        text="Generate Commit Message"
        description="Use AI to generate commit message">
        <add-to-group group-id="Vcs.MessageActionGroup" anchor="first"/>
        <keyboard-shortcut keymap="$default" first-keystroke="ctrl alt G"/>
    </action>
</actions>
```

## 🧪 测试策略

### 单元测试（计划中）
- SdkManager.detect() - CLI 检测逻辑
- ApiClient.buildRequest() - API 请求构建
- AiCommitPrompts - 提示词格式化

### 集成测试（计划中）
- CLI 命令执行
- API 请求发送
- Git diff 获取

### UI 测试（手动）
- 设置页面交互
- 模式切换
- 表单验证
- 错误提示

## 📝 编码规范

- **语言**: Kotlin 1.6.21
- **目标**: JVM 11
- **缩进**: 4 空格
- **命名**: camelCase（变量/函数），PascalCase（类）
- **注释**: KDoc 风格

## 🔐 安全考虑

1. **API Key 存储**: 使用 IntelliJ PasswordSafe API（待实现）
2. **命令注入**: 避免 shell 注入，使用 ProcessBuilder
3. **网络安全**: HTTPS only，超时控制
4. **日志**: 不记录敏感信息（API Key、Token）

## 🚀 性能优化

1. **懒加载**: UI 组件按需创建
2. **缓存**: CLI 检测结果缓存
3. **异步**: 后台线程执行耗时操作
4. **超时**: 所有外部调用设置超时

---

**最后更新**: 2024-09-03
**项目状态**: ✅ 准备发布
