# 提交窗口快速切换模型实施记录

**目标：** 在提交信息工具栏的闪电按钮旁添加模型切换入口，并在生成失败时自动弹出同一列表。

**已确认交互：** API 模式选择已保存的「配置名 — 模型」；CLI 模式选择当前 CLI 的缓存/内置模型并保留思考等级。当前项勾选，选择后持久化，下次点击闪电生成。失败只弹列表，不自动发起第二次模型请求；取消不弹。列表末尾提供管理配置入口，未配置 API 时也能进入设置。

**实现：** QuickModelAction 注册到 Vcs.MessageActionGroup 的生成按钮之前，使用 IDEA 原生可搜索弹出菜单。统一后台任务负责运行状态、取消和失败回调。生成开始时复制配置，避免设置页修改影响在途请求。应用级配置对应应用级生成互斥；任务结束后释放，确保失败列表可选择。

**文件：** 新增 QuickModelAction.kt、CommitGenerationTask.kt；修改 AiCommitSettings.kt（快照）、GenerateCommitMessageAction.kt（任务与失败入口）、ProviderModelPanel.kt/SdkManager.kt（复用离线候选）、plugin.xml、双语资源和使用文档；新增对应回归测试。

## 检查清单

- [x] 写并运行回归：API 选择按 ID 激活、不触碰密钥；CLI 模型/思考等级、空列表、过期选项；请求快照隔离。
- [x] 实现候选构建/切换及 IDEA 弹出菜单，列表不访问网络或 PasswordSafe。
- [x] 写并运行任务回归：失败通知后弹列表、取消不弹、成功不弹、关闭项目不弹、重复点击受保护、释放幂等。
- [x] 接入生成动作和工具栏注册，保持提交输入框内容保护。
- [x] 运行完整测试和 buildPlugin，检查包内注册、资源、大小与 SHA256，更新交付记录。最终为 54 项测试通过；ZIP 为 240981 字节，SHA256 为 `62AB86A5853B07A80E48DE4C1EA3D222F92A867F5B65299C1CE8A06749A5E89F`。

验证命令：`& 'D:\gradle-7.4.2\bin\gradle.bat' test buildPlugin --no-daemon --console=plain`。临时文件使用项目 tmp/，不安装依赖、不执行 Git 写操作、不调用真实生成服务。
