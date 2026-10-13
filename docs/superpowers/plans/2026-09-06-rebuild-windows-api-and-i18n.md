# Generate Commit Message 重建实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按已恢复的 Claude 需求重建可在 Windows 使用的 IntelliJ 插件，保留 CLI Tools 与 AI Commit，补齐第三方 API 生成、全局界面语言和 Windows CLI 检测，并通过测试与插件打包验收。

**Architecture:** `AiCommitService` 只负责生成流程和后端路由；CLI 检测/环境集中在 `SdkManager`；第三方 API 配置与 HTTP 调用由独立的 `ApiProviderConfig`/`ApiClient` 负责；设置面板通过 `AiCommitSettings` 与 `CliToolsSettings` 持久化；界面文本统一经 `SubMsgBundle` 读取资源包。

**Tech Stack:** Kotlin 1.8.21、IntelliJ Platform 2022.2.5、Swing/IntelliJ UI、JDK 17、Gradle 7.x、JUnit 4。

---

## 执行记录（2026-09-07）

原始任务步骤保留在下方，当前验收状态以此记录和 [FINAL_SUMMARY.md](../../../FINAL_SUMMARY.md) 为准。

- [x] Task 1：API 数据结构、两种协议、模型获取、参数校验和错误脱敏已实现并测试。使用 JDK 17 `HttpClient.sendAsync` 以支持取消，替代原计划的 `HttpURLConnection`；密钥改用 PasswordSafe，XML 不持久化密钥。
- [x] Task 2：CLI/API 路由和提示词参数接入；API 不检测 CLI；补齐空选择保护、Diff 插入行比较与进程取消。
- [x] Task 3：Windows PATH、npm/nvm、原生与批处理启动兼容；参数传递和超时测试通过；本机 Claude/Codex 版本检测通过。
- [x] Task 4：两个设置标签页、API 多配置、模型输入及刷新测试、附加提示和增强选项接入。配置状态测试通过；真实 Swing 事件回归验证模型输入保留空格及光标位置；正式设置入口通过无头搜索索引加载。
- [x] Task 5：全局中英文及独立输出语言，保存后刷新当前设置页；补齐插件自有弹窗、消息框及可取消任务的按钮语言；资源缺失和重复键静态检查通过。
- [x] Task 6：当前文档与插件描述统一；42 项测试通过；ZIP 重新生成并检查注册、资源和旧类清理。最终审查发现的 API URL 转义丢失和 Windows 普通批处理重复转义均已先复现、后修复，并加入回归测试。
- [ ] 人工 IDEA 界面验收，以及实际第三方 API/CLI 账号请求验证。其他 5 个 CLI 本机未检测到，未安装依赖。

验证详情、版本信息与安装包散列见 [交付记录](../../../FINAL_SUMMARY.md)。下列原始测试规划中的场景并非均为独立 UI 自动化测试，实际覆盖范围以该交付记录为准。

### Task 1: API 数据模型与 HTTP 客户端

**Files:**
- Create: `src/main/kotlin/com/fs/aicommit/ApiProviderConfig.kt`
- Create: `src/main/kotlin/com/fs/aicommit/ApiClient.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitSettings.kt`
- Test: `src/test/kotlin/com/fs/aicommit/ApiProviderConfigTest.kt`

- [ ] 写测试：默认 API 配置、配置字段复制/持久化、Base URL 规范化、缺少 Key/模型的校验。
- [ ] 运行对应测试，确认测试因类型/方法不存在而失败。
- [ ] 实现配置数据类和应用级持久化字段：多个配置、活动配置 ID、名称、提供商、Base URL、API Key、模型、推理兼容性。
- [ ] 实现 OpenAI-compatible `POST /chat/completions` 与 Anthropic `POST /messages` 请求，使用 JDK `HttpURLConnection`，不记录 API Key，解析纯文本结果和常见错误。
- [ ] 再运行测试，确认全部通过。

### Task 2: AI Commit API 路由与生成流程

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitService.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitPrompts.kt`
- Test: `src/test/kotlin/com/fs/aicommit/AiCommitServiceTest.kt`

- [ ] 写测试：CLI 模式仍走 CLI；API 模式不检测 CLI，向 API 客户端传递 diff、近期提交、附加提示词和输出语言；API 空响应/错误转换为可读异常。
- [ ] 运行测试确认先失败。
- [ ] 在设置中增加 `mode`（CLI/API）和兼容旧配置的默认值；在 `generate` 中按模式路由；保留 diff 截断、近期提交和输出清洗。
- [ ] 为 API 模式增加连接测试和模型列表调用的服务接口，供 UI 使用。
- [ ] 运行测试确认通过。

### Task 3: Windows CLI 检测和进程环境

**Files:**
- Modify: `src/main/kotlin/com/fs/aicommit/SdkManager.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitService.kt`
- Test: `src/test/kotlin/com/fs/aicommit/SdkManagerTest.kt`

- [ ] 写测试：Windows 下候选路径包含 `%APPDATA%\npm`、`%LOCALAPPDATA%\Programs`、Program Files 及 `.cmd/.bat/.exe`；PATH 使用 `File.pathSeparator`；非 Windows 行为保持 Unix 兼容。
- [ ] 运行测试确认先失败。
- [ ] 实现操作系统分支、`where`/`which`、Windows shell fallback、候选路径扫描和 PATH 合并；子进程环境使用平台分隔符，不硬编码 `/bin/zsh` 或冒号。
- [ ] 运行测试确认通过，并静态检查 7 个 CLI 的命令仍可执行。

### Task 4: API/CLI 配置 UI 和统一设置页

**Files:**
- Create: `src/main/kotlin/com/fs/aicommit/ApiConfigDialog.kt`
- Create: `src/main/kotlin/com/fs/aicommit/ApiModePanel.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitPanel.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/GenerateCommitMessageConfigurable.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

- [ ] 写 UI 状态测试：API 配置增删改、活动配置切换、模式切换、增强选项保存/重置，测试按钮调用正确服务。
- [ ] 运行测试确认先失败。
- [ ] 在 AI Commit 标签页加入 CLI/API 模式切换；API 面板提供多配置表格、添加/编辑/删除、激活配置、测试连接和获取模型；沿用两个最终标签页 `AI Commit`/`CLI Tools`。
- [ ] 按 IDEA 间距规范调整布局，限制 Mode 下拉宽度，保证小窗口滚动；所有配置 apply/reset/isModified 对称。
- [ ] 运行测试确认通过并静态检查 plugin.xml 仅注册最终配置入口。

### Task 5: 全局界面语言

**Files:**
- Create: `src/main/kotlin/com/fs/i18n/SubMsgBundle.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitSettings.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/AiCommitPanel.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/GenerateCommitMessageConfigurable.kt`
- Modify: `src/main/kotlin/com/fs/terminal/CliToolsPanel.kt`
- Modify: `src/main/kotlin/com/fs/terminal/CliToolsConfigurable.kt`
- Modify: `src/main/kotlin/com/fs/aicommit/GenerateCommitMessageAction.kt`
- Modify: `src/main/kotlin/com/fs/ShowInFinderAction.kt`
- Modify: `src/main/resources/messages/SubMsgBundle.properties`
- Modify: `src/main/resources/messages/SubMsgBundle_zh.properties`
- Test: `src/test/kotlin/com/fs/i18n/SubMsgBundleTest.kt`

- [ ] 写测试：默认中文、英文切换、未知语言回退中文、参数化消息。
- [ ] 运行测试确认先失败。
- [ ] 实现持久化的全局界面语言设置和资源包访问；将用户可见的设置页、按钮、通知、错误、CLI 工具描述迁移到 bundle；保留 `outputLanguage` 作为提交内容语言，二者独立。
- [ ] 运行测试确认通过。

### Task 6: 回归清理与交付验证

**Files:**
- Modify: `README.md`, `README_CN.md`, `INSTALL_GUIDE.md`, `FEATURE_SUMMARY.md`, `FINAL_SUMMARY.md`, `CHANGELOG_v2.0.md`（统一最终范围）
- Modify: `src/main/resources/META-INF/plugin.xml`

- [ ] 删除/改写旧文档中把 AI Inline、API Search 或未实现 API 模式宣称为当前完成的内容。
- [ ] 运行 `gradlew.bat test`，确认测试结果。
- [ ] 运行 `D:\gradle-7.4.2\bin\gradle.bat buildPlugin`（使用 JDK 17），确认插件 ZIP 生成。
- [ ] 检查 ZIP 内容、plugin.xml、类加载注册和工作区 diff；不执行 `git add/commit/push/merge`。
