# 手动模型选择自动生成实施计划

目标：手动模型菜单选中即生成，保持已有成功关闭、失败保留的交互，发布 1.0.5 后非强制关机。

## 实施与验收

- [x] 将 QuickModelAction 的菜单创建与显示分离，使用真实菜单、提交面板上下文及本地 HTTP 服务编写入口回归，观察未发请求的失败。
- [x] 从 GenerateCommitMessageAction 提取共用请求工厂；CommitGenerationRequest 接受已有面板。手动菜单第一次选择时创建请求并立即启动，后续复用，保留无选择和输入保护。
- [x] 验证 API/CLI、当前模型、选中文件、连续失败、取消及重复点击；更新中英文提示和使用说明。
- [x] 更新 1.0.5 版本和发布说明；使用临时官方直连初始化脚本运行 Gradle `test verifyPlugin generateUpdatePluginsXml --console=plain`（117 项测试通过），包与清单元数据一致。
- [x] 提交 `cd6a3a5` 及 v1.0.5 标签已推送至 `https://github.com/hwglhj/generateCommitMessage.git`。Windows/Linux/macOS 构建全部成功，IDEA 2026.1.5、2026.2、2026.2.4、2026.3 发布检查全部通过；正式 ZIP 及固定更新清单已下载并核对。
- [x] 更新 FINAL_SUMMARY.md、IDE_COMPATIBILITY.md 的正式发布记录和下载链接。

发布后收尾：提交并推送本次记录，交付下载地址后安排非强制关机。延迟由独立 PowerShell 进程完成，再执行 `shutdown.exe /s /t 0`，不使用 `/f`；未保存的应用可阻止关机。安排及执行结果保存在本机 `tmp/release-1.0.5/13-shutdown.log`。
