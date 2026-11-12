# 重建验证记录

核验日期：2026-11-13。当前正式版本为 1.0.5，手动快速选择模型立即生成。

已补齐 API 模式、独立中英文界面、Windows 进程兼容、外部编辑器选择、提交面板按站点分组快速切换模型、激活站点标记与失败后的模型选择，并增加 API 站点多模型保存、JSON 导入导出和可选 API Key 导出。1.0.1 将快速菜单改为读取持久化模型缓存，支持单站点刷新、刷新完成后保持弹窗并原地替换列表；失败、取消、空结果和过期请求均保留旧缓存。Pi（`pi`）与 OMP（`omp`）保持为两个独立 CLI。实现范围见 [FEATURE_SUMMARY.md](FEATURE_SUMMARY.md)。

## 1.0.5 发布与验证（2026-11-13）

手动快速菜单选择 API 或 CLI 模型后立即生成勾选变更的提交信息，选择当前模型也会生成。手动入口与失败入口共用请求工厂和面板状态：生成中保留并禁用选择，成功关闭，失败或取消恢复原面板。打开菜单、展开站点、刷新列表及关闭菜单不生成；空选择或缺少提交面板时仅保存模型选择并提示原因。

新入口集成测试先复现了 3 个“选中模型未生成”的失败，修复后通过。使用真实 IntelliJ 菜单、提交上下文和本地 HTTP 服务验证站点/模型、勾选变更优先级、当前模型重试、空选择后补选以及提交文字保护；CLI 路径验证无初始失败也可选中即生成。

本机 `test verifyPlugin generateUpdatePluginsXml` 成功：117 项测试，0 失败、0 错误、0 跳过；1.0.5 ZIP 及更新清单版本、ID 和下载地址一致。日志为 `tmp/manual-model-generation/06-build.log`。因本地编译依赖缓存缺失且 JetBrains 缓存转发下载超时，使用临时初始化脚本 `tmp/manual-model-generation/03-direct-repository.gradle` 从官方直连地址取得同版本 SDK；编译基线仍为 2022.2.5 / 222。

发布提交 `cd6a3a5` 已推送至 GitHub `master`，标签为 `v1.0.5`。[发布任务](https://github.com/hwglhj/generateCommitMessage/actions/runs/43262826090)完成构建及 IDEA 2026.1.5、2026.2、2026.2.4、2026.3 的 Plugin Verifier 检查，全部通过后创建正式 Release。[三系统构建](https://github.com/hwglhj/generateCommitMessage/actions/runs/43262825760)的 Linux、Windows、macOS 检查也全部成功。

- 正式版本：[GitHub Release v1.0.5](https://github.com/hwglhj/generateCommitMessage/releases/tag/v1.0.5)。
- 安装包：[generateCommitMessage-1.0.5.zip](https://github.com/hwglhj/generateCommitMessage/releases/download/v1.0.5/generateCommitMessage-1.0.5.zip)。
- 固定更新仓库：[updatePlugins.xml](https://github.com/hwglhj/generateCommitMessage/releases/latest/download/updatePlugins.xml)。

正式 ZIP 和固定更新地址的清单已实际下载到 `tmp/release-1.0.5/` 并核对：版本均为 `1.0.5`，插件 ID 为 `com.fs.generateCommitMessage`，最低平台为 `222`，清单指向本次正式 ZIP。包内包含手动模型生成入口及共用请求实现。

本次未替换用户 IDEA 中的插件。用户可检查更新，或从磁盘安装正式 ZIP 后重启 IDEA。按用户要求，发布记录推送结束后安排非强制关机，不强制结束未保存的应用；安排及执行日志保存在本机 `tmp/release-1.0.5/13-shutdown.log`。行为说明见[手动模型生成设计](docs/superpowers/specs/2026-11-12-manual-model-generation-design.md)。

## 1.0.4 发布与验证（2026-11-11）

失败后选择新模型会立即重试，生成期间保留面板，成功后关闭主面板及站点子菜单；失败后恢复原面板的选择，不再重新创建弹窗。取消、手动关闭和重复点击的处理见[重试面板说明](docs/superpowers/specs/2026-11-11-retry-popup-lifecycle-design.md)。

本机 Windows 执行 `test verifyPlugin generateUpdatePluginsXml` 成功：111 项测试，0 失败、0 错误、0 跳过，生成 1.0.4 安装包及更新清单。日志为 `tmp/release-1.0.4/01-build.log`。真实 IntelliJ 弹窗的无头集成测试覆盖鼠标和键盘选择、连续失败及成功关闭；关键类在 IDEA 2026.3 类路径加载通过。

发布提交 `1ad2a20` 已推送至 GitHub `master`，标签为 `v1.0.4`。[三系统构建](https://github.com/hwglhj/generateCommitMessage/actions/runs/42952359753)在 Windows、Linux、macOS 均成功；[发布任务](https://github.com/hwglhj/generateCommitMessage/actions/runs/42952359795)完成构建及 IDEA 2026.1.5、2026.2、2026.2.4、2026.3 的 Plugin Verifier 检查，全部通过后创建正式 Release。

- 正式版本：[GitHub Release v1.0.4](https://github.com/hwglhj/generateCommitMessage/releases/tag/v1.0.4)。
- 安装包：[generateCommitMessage-1.0.4.zip](https://github.com/hwglhj/generateCommitMessage/releases/download/v1.0.4/generateCommitMessage-1.0.4.zip)。
- 固定更新仓库：[updatePlugins.xml](https://github.com/hwglhj/generateCommitMessage/releases/latest/download/updatePlugins.xml)。

正式 ZIP 和固定更新地址的清单已实际下载到 `tmp/release-1.0.4/` 并校验：版本均为 `1.0.4`，插件 ID 为 `com.fs.generateCommitMessage`，最低平台为 `222`，清单指向本次正式 ZIP；包内包含新的面板生命周期实现。

本次未替换用户 IDEA 中的插件或执行真实服务请求。用户可在 IDEA 检查更新，或从磁盘安装正式 ZIP 后重启，按 [安装与验收](INSTALL_GUIDE.md) 检查实际界面。下文保留 1.0.3 的历史记录。

## 1.0.3 自动化验证记录

使用本机 JDK 17、Gradle 7.4.2 运行：

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test verifyPlugin generateUpdatePluginsXml --console=plain
```

2026-11-08 本机 Windows 执行通过 105 项测试，0 失败、0 错误、0 跳过；插件结构检查及 1.0.3 安装包、更新清单构建为 `BUILD SUCCESSFUL`。本次安装包在 1.0.1 模型缓存、单站点刷新和弹窗兼容修复的基础上，包含失败弹窗选中模型后立即重试。

1.0.2 的发布任务因 Pi 路径测试在 Linux 上使用固定反斜杠而失败，未生成 Release。1.0.3 将该断言改为统一路径分隔符后比较，保留 Windows 路径检查，并在 CI 注释中输出具体失败用例。

更新清单从最终插件描述符生成，ID、版本、兼容范围、ZIP 名和 Release URL 保持一致。两份 GitHub Actions 工作流的 YAML 解析通过。[1.0.3 三系统构建任务](https://github.com/hwglhj/generateCommitMessage/actions/runs/42389438042)在 Windows、Linux、macOS 均成功；[正式发布任务](https://github.com/hwglhj/generateCommitMessage/actions/runs/42389438187)的构建、四版本兼容检查及 Release 上传全部成功。

新增 8 项回归在 IntelliJ 测试环境中执行真实模型选择 Action，使用可控 HTTP 响应模拟失败与成功，检查新站点地址、新模型和原始输入是否用于重试；同时覆盖连续失败、CLI 思考等级、取消、项目关闭、防重复请求、过期选项以及普通工具栏切换。接线前测试复现了选中模型后没有发起新请求的问题，修复后通过。未使用真实第三方账号发起生成请求。

覆盖 API 请求与响应、脱敏、配置与密钥保存重置、XML 排除密钥、语言回退、模型状态、Diff 插入行、显式空选择、Windows npm 参数传递、进程超时和编辑器路径。新增站点模型列表迁移、模型去重、站点分组选择、版本化 JSON 导入导出、非法文件拒绝、同 ID 覆盖、PasswordSafe 密钥保留与导入，以及导出密钥确认回归。Pi 回归覆盖 SDK 定义、Windows 官方安装路径、模型表格解析、无会话无工具生成参数、模型和思考等级参数、终端命令及高权限开关能力。新增回归先复现并修复了 API 网关路径/查询参数转义丢失，以及含空格路径下普通 `.cmd`/`.bat` 启动时的重复转义。

模型输入回归使用真实 Swing 文档事件，验证含空格模型名可以连续输入，中间插入文字后保留光标位置。新增测试覆盖 API 配置快速切换、站点内多个模型、刷新期间切换当前模型、按站点并发保护、刷新失败保留缓存，以及嵌套 Popup 保持打开并原地替换可见列表。插件自有弹窗、消息框和可取消任务的按钮也已接入独立界面语言，资源及编译检查通过。上述结果不包含真实第三方服务调用。

## 1.0.3 插件包记录

- 正式版本：[GitHub Release v1.0.3](https://github.com/hwglhj/generateCommitMessage/releases/tag/v1.0.3)。
- 安装包：[generateCommitMessage-1.0.3.zip](https://github.com/hwglhj/generateCommitMessage/releases/download/v1.0.3/generateCommitMessage-1.0.3.zip)，已实际下载验证。
- 更新清单：[updatePlugins.xml](https://github.com/hwglhj/generateCommitMessage/releases/latest/download/updatePlugins.xml)，已实际下载验证，与安装包元数据一致。
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

1.0.3 正式发布流程使用官方 Plugin Verifier 1.411 和各 IDEA 自带 JBR，检查同一个待发布 ZIP：

| IDEA | 构建号 | 二进制兼容检查 |
| --- | --- | --- |
| 2026.1.5 | IU-261.27258.48 | Compatible |
| 2026.2 | IU-262.8665.258 | Compatible |
| 2026.2.4 | IU-262.11879.100 | Compatible |
| 2026.3 | IU-263.7307.200 | Compatible |

四个版本全部返回 `Compatible` 后创建了 Release，完整报告为发布任务的 `compatibility-版本号` 产物。此前本机 2026.1.5、2026.3 对同一功能代码的检查也通过，有部分废弃及计划移除 API 警告，报告保存在 `tmp/idea-compat/baseline-offline/`。

2026.3 日志显示原插件 1.0.1 已加载，英文安装来源提示由缺失的更新仓库记录引起。1.0.3 提供标准更新清单；从旧版本迁移的用户需要在 IDEA 中添加仓库并选择该更新来源，然后检查更新。见 [IDEA 2026 兼容与更新渠道](docs/IDE_COMPATIBILITY.md)。

本机 IDEA 2026.3 已在退出状态下安装 GitHub 正式包 1.0.3，并添加更新仓库及该插件的来源记录。使用 IDEA 2026.3 自身的 XML 序列化类反序列化校验通过，已安装 JAR 的版本和 ID 校验通过。旧插件及两份配置已备份到 `tmp/idea-compat/install-1.0.3-20261108-001040/backup/`。启动 IDEA 的命令被自动审批检查拦截，尚未进行升级后的启动和通知界面复核；需要用户打开 IDEA 完成此项确认。

真实 API 服务、各 CLI 账号以及 IDE 内完整交互仍需按 [安装与验收](INSTALL_GUIDE.md) 检查。无头设置索引构建已加载正式配置入口。

设置搜索索引构建时，IntelliJ 的系统证书读取器提示本机某个证书的 SM2 曲线不受当前 JDK 支持；该警告未阻止设置索引、测试和打包，没有修改本机证书或信任设置。
