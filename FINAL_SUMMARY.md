# 重建验证记录

核验日期：2026-11-07。当前插件版本：1.0.2，包含失败后选模型自动重试、IDEA 2026 兼容检查及 GitHub 更新仓库。

已补齐 API 模式、独立中英文界面、Windows 进程兼容、外部编辑器选择、提交面板按站点分组快速切换模型、激活站点标记与失败后的模型选择，并增加 API 站点多模型保存、JSON 导入导出和可选 API Key 导出。1.0.1 将快速菜单改为读取持久化模型缓存，支持单站点刷新、刷新完成后保持弹窗并原地替换列表；失败、取消、空结果和过期请求均保留旧缓存。Pi（`pi`）与 OMP（`omp`）保持为两个独立 CLI。实现范围见 [FEATURE_SUMMARY.md](FEATURE_SUMMARY.md)。

## 自动化验证

使用本机 JDK 17、Gradle 7.4.2 运行：

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test verifyPlugin generateUpdatePluginsXml --console=plain
```

2026-11-07 本机 Windows 执行通过 105 项测试，0 失败、0 错误、0 跳过；插件结构检查及 1.0.2 安装包、更新清单构建为 `BUILD SUCCESSFUL`。本次安装包在 1.0.1 模型缓存、单站点刷新和弹窗兼容修复的基础上，包含失败弹窗选中模型后立即重试。

更新清单从最终插件描述符生成，ID、版本、兼容范围、ZIP 名和 Release URL 保持一致。两份 GitHub Actions 工作流的 YAML 解析通过。正式发布由 [Release 工作流](https://github.com/hwglhj/generateCommitMessage/actions/workflows/release.yml) 执行，远程报告随对应运行保存。

新增 8 项回归在 IntelliJ 测试环境中执行真实模型选择 Action，使用可控 HTTP 响应模拟失败与成功，检查新站点地址、新模型和原始输入是否用于重试；同时覆盖连续失败、CLI 思考等级、取消、项目关闭、防重复请求、过期选项以及普通工具栏切换。接线前测试复现了选中模型后没有发起新请求的问题，修复后通过。未使用真实第三方账号发起生成请求。

覆盖 API 请求与响应、脱敏、配置与密钥保存重置、XML 排除密钥、语言回退、模型状态、Diff 插入行、显式空选择、Windows npm 参数传递、进程超时和编辑器路径。新增站点模型列表迁移、模型去重、站点分组选择、版本化 JSON 导入导出、非法文件拒绝、同 ID 覆盖、PasswordSafe 密钥保留与导入，以及导出密钥确认回归。Pi 回归覆盖 SDK 定义、Windows 官方安装路径、模型表格解析、无会话无工具生成参数、模型和思考等级参数、终端命令及高权限开关能力。新增回归先复现并修复了 API 网关路径/查询参数转义丢失，以及含空格路径下普通 `.cmd`/`.bat` 启动时的重复转义。

模型输入回归使用真实 Swing 文档事件，验证含空格模型名可以连续输入，中间插入文字后保留光标位置。新增测试覆盖 API 配置快速切换、站点内多个模型、刷新期间切换当前模型、按站点并发保护、刷新失败保留缓存，以及嵌套 Popup 保持打开并原地替换可见列表。插件自有弹窗、消息框和可取消任务的按钮也已接入独立界面语言，资源及编译检查通过。上述结果不包含真实第三方服务调用。

## 插件包

- 文件：[generateCommitMessage-1.0.2.zip](build/distributions/generateCommitMessage-1.0.2.zip)
- 更新清单：[updatePlugins.xml](build/distributions/updatePlugins.xml)，与 ZIP 同目录生成并随正式 Release 上传。
- 包内 ID：`com.fs.generateCommitMessage`；vendor：`hwglhj`；最低平台：`222`。
- 仅注册 `com.fs.aicommit.GenerateCommitMessageConfigurable`，保留 8 个 ToolWindow、中英文资源和设置搜索索引。
- 两份未注册旧设置类已从源码和插件包移除，原文件仍可在 Git 历史中查看。

## Windows 实机检测

使用插件自身 `SdkManager.detect` 读取版本，复现脚本位于 `scripts/windows-verification/01-cli-smoke.java`，不调用模型、不修改 CLI 登录状态：

| CLI | 本机结果 |
| --- | --- |
| Claude | 2.1.261，`C:\Program Files\nodejs\claude.cmd` |
| Codex | 0.142.5，`C:\Program Files\nodejs\codex.cmd` |
| Cursor、OpenCode、Gemini/agy、Grok、OMP、Pi | 本机未检测到可用命令，未安装额外依赖 |

这证明了当前机器上的 Claude/Codex 路径检测与版本启动，不代表模型认证或生成请求已通过。

## 验收边界

使用官方 Plugin Verifier 1.411、2026.3 自带 JBR，以离线模式检查本机安装的 IDEA：

| IDEA | 构建号 | 二进制兼容检查 |
| --- | --- | --- |
| 2026.1.5 | IU-261.27258.48 | Compatible；无缺失类或方法，存在废弃 API 警告 |
| 2026.3 | IU-263.7307.200 | Compatible；无缺失类或方法，存在废弃 API 警告 |

以上是 1.0.2 发布前对同一功能代码的本机检查，原始报告保存在 `tmp/idea-compat/baseline-offline/`。两份最终报告均未报告依赖缺失。1.0.2 的正式发布流程在 GitHub 运行器上检查 2026.1.5、2026.2、2026.2.4、2026.3；全部返回 `Compatible` 后才能创建 Release，完整报告为对应运行的 `compatibility-版本号` 产物。

2026.3 日志显示原插件 1.0.1 已加载，英文安装来源提示由缺失的更新仓库记录引起。1.0.2 提供标准更新清单；从旧版本迁移的用户需要在 IDEA 中添加仓库并选择该更新来源，然后检查更新。见 [IDEA 2026 兼容与更新渠道](docs/IDE_COMPATIBILITY.md)。

真实 API 服务、各 CLI 账号以及 IDE 内完整交互仍需按 [安装与验收](INSTALL_GUIDE.md) 检查。无头设置索引构建已加载正式配置入口。

设置搜索索引构建时，IntelliJ 的系统证书读取器提示本机某个证书的 SM2 曲线不受当前 JDK 支持；该警告未阻止设置索引、测试和打包，没有修改本机证书或信任设置。
