# 手动模型选择自动生成实施计划

目标：手动模型菜单选中即生成，保持已有成功关闭、失败保留的交互，发布 1.0.5 后非强制关机。

## 实施与验收

- [x] 将 QuickModelAction 的菜单创建与显示分离，使用真实菜单、提交面板上下文及本地 HTTP 服务编写入口回归，观察未发请求的失败。
- [x] 从 GenerateCommitMessageAction 提取共用请求工厂；CommitGenerationRequest 接受已有面板。手动菜单第一次选择时创建请求并立即启动，后续复用，保留无选择和输入保护。
- [x] 验证 API/CLI、当前模型、选中文件、连续失败、取消及重复点击；更新中英文提示和使用说明。
- [x] 更新 1.0.5 版本和发布说明；使用临时官方直连初始化脚本运行 Gradle `test verifyPlugin generateUpdatePluginsXml --console=plain`（117 项测试通过），包与清单元数据一致。
- [ ] 提交并推送代码及 v1.0.5 标签至 `https://github.com/hwglhj/generateCommitMessage.git`。跟进 Windows/Linux/macOS 构建和 IDEA 2026.1.5、2026.2、2026.2.4、2026.3 发布检查，核对正式资产。
- [ ] 更新发布记录并推送，交付下载地址后执行非强制关机，保护未保存的应用内容。
