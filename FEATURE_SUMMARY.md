# 当前功能映射

有效需求来源：[Claude 历史需求恢复清单](docs/claude-history/2026-09-06-recovered-requirements.md)。

| 范围 | 当前实现 |
| --- | --- |
| AI Commit CLI/API 路由、提示词、Diff、近期提交、输出清洗 | `AiCommitService`、`AiCommitPrompts` |
| API 协议、模型、错误处理与取消 | `ApiClient`、`ApiProviderConfig` |
| API 多站点、多模型、导入导出及密钥 | `ApiConfigurationModel`、`ApiModePanel`、`ApiConfigDialog`、`ApiSiteTransfer`、`ApiSiteExportDialog`、`ApiCredentials` |
| Windows 检测、npm 启动、PATH、进程超时、Pi 官方安装目录 | `SdkManager`、`CliProcess` |
| 模型搜索与思考等级、设置保存重置 | `ProviderModelPanel`、`ProviderModelSelection`、`AiCommitPanel` |
| 提交面板按站点分组快速切换模型、打开菜单刷新各站点模型、激活站点标记、失败后自动弹出模型列表 | `QuickModelAction`、`CommitGenerationTask`、`ApiClient` |
| 统一双标签设置、界面语言、外部编辑器选择 | `com.fs.aicommit.GenerateCommitMessageConfigurable` |
| 全局中英文资源 | `SubMsgBundle`、`messages/SubMsgBundle*.properties` |
| Claude、Codex、Cursor、OpenCode、Gemini/agy、Grok、OMP、Pi 的 AI Commit 路由 | `SdkDefinition`、`AiCommitService`、`ProviderModelPanel` |
| CLI 终端、图标与按工具能力显示的高权限开关 | `terminal` 包 |
| 外部编辑器行列跳转 | `ShowInFinderAction`、`ExternalEditor` |

仅保留 AI Commit、CLI Tools 和外部编辑器跳转。AI Inline、API Search 已取消；历史文档中的相关功能和完成比例不代表当前版本。

Pi（`pi`）与 OMP（`omp`）是两个独立 CLI。Pi 通过 `pi --list-models` 读取当前认证环境中的模型，并使用无会话、无工具的 print 模式生成提交信息。

测试与真实环境验证范围见 [FINAL_SUMMARY.md](FINAL_SUMMARY.md)。
