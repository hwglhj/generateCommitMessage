# 重建验证记录

核验日期：2026-10-13。当前插件版本：1.0.0。

已补齐 API 模式、独立中英文界面、Windows 进程兼容、外部编辑器选择、提交面板按站点分组快速切换模型、打开菜单刷新每个站点完整模型列表、激活站点标记与失败后的模型选择，并增加 API 站点多模型保存、JSON 导入导出和可选 API Key 导出。2026-10-13 新增官方 Pi Coding Agent：AI Commit 支持 `pi --print`、`pi --list-models`、模型与思考等级选择，右侧增加 Pi 终端入口，并识别 Windows 官方安装目录。Pi（`pi`）与 OMP（`omp`）保持为两个独立 CLI。实现范围见 [FEATURE_SUMMARY.md](FEATURE_SUMMARY.md)。

## 自动化验证

使用本机 JDK 17、Gradle 7.4.2 运行：

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test buildPlugin --no-daemon --console=plain
```

完整执行通过 84 项测试，0 失败、0 错误、0 跳过。2026-10-13 重新执行测试与打包，结果为 `BUILD SUCCESSFUL`，生成了包含 Pi CLI、站点模型实时刷新、激活站点标记和站点导入导出功能的新安装包。

覆盖 API 请求与响应、脱敏、配置与密钥保存重置、XML 排除密钥、语言回退、模型状态、Diff 插入行、显式空选择、Windows npm 参数传递、进程超时和编辑器路径。新增站点模型列表迁移、模型去重、站点分组选择、版本化 JSON 导入导出、非法文件拒绝、同 ID 覆盖、PasswordSafe 密钥保留与导入，以及导出密钥确认回归。Pi 回归覆盖 SDK 定义、Windows 官方安装路径、模型表格解析、无会话无工具生成参数、模型和思考等级参数、终端命令及高权限开关能力。新增回归先复现并修复了 API 网关路径/查询参数转义丢失，以及含空格路径下普通 `.cmd`/`.bat` 启动时的重复转义。

模型输入回归使用真实 Swing 文档事件，验证含空格模型名可以连续输入，中间插入文字后保留光标位置。新增测试覆盖 API 配置快速切换、站点内多个模型、CLI 候选与思考等级保留、生成配置快照、生成成功/失败/取消/项目关闭时的任务状态，以及失败后打开模型列表。插件自有弹窗、消息框和可取消任务的按钮也已接入独立界面语言，资源及编译检查通过。上述结果不包含真实第三方服务调用或人工 IDEA 完整界面验收。

## 插件包

- 文件：[generateCommitMessage-1.0.0.zip](build/distributions/generateCommitMessage-1.0.0.zip)
- 文件大小：286723 字节。
- SHA256：`73EC88F67E52F7ECAAD37AEA47FCE46EA3156F5C269FD20D6411FB2C04ED14AB`
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

真实 API 服务、各 CLI 账号以及 IDE 内交互仍需按 [安装与验收](INSTALL_GUIDE.md) 检查。无头设置索引构建已加载正式配置入口，但不等于人工确认每种窗口大小下的布局。

设置搜索索引构建时，IntelliJ 的系统证书读取器提示本机某个证书的 SM2 曲线不受当前 JDK 支持；该警告未阻止设置索引、测试和打包，没有修改本机证书或信任设置。

未执行 Git 暂存、提交、推送或合并。当前源码和文档改动保留在工作区。
