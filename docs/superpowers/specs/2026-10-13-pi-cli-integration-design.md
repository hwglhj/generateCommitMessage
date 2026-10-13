# Pi CLI 集成设计

## 目标

把官方 Pi Coding Agent（命令名 `pi`）完整接入插件现有 CLI 体系，使用户能够：

- 在 AI Commit 设置和快速模型菜单中选择 Pi。
- 使用 Pi 当前认证环境中的模型生成 Git 提交信息。
- 从 Pi 本机模型列表中选择 `provider/model`，并设置思考等级。
- 从 IDE 右侧工具栏打开 Pi 交互终端。
- 在 Windows 上识别官方安装器、npm、nvm、Volta 和 Bun 等常见安装位置中的 Pi 启动程序。

Pi 与现有 OMP 是两个独立 CLI。Pi 使用命令 `pi`，OMP 继续使用命令 `omp`，两者分别显示和配置。

## 官方 CLI 行为

本设计依据 Pi 官方 CLI 文档和源码中的命令定义：

- `pi --version`：检测安装并读取版本。
- `pi --print`：以非交互模式执行一次提示并输出最终文本。
- `pi --model <provider/model>`：选择模型。
- `pi --thinking <level>`：选择思考等级。
- `pi --list-models`：列出当前凭证可用的模型。
- `pi --no-session`：不保存一次性的提交信息生成会话。
- `pi --no-tools`：禁止生成提交信息时调用工具。
- `pi --no-context-files`：不加载项目中的 `AGENTS.md` 和 `CLAUDE.md`。
- `pi --no-approve`：忽略需要项目信任的本地配置和资源。

Pi 没有独立的“高权限模式”参数。它默认继承启动进程的操作系统权限，因此插件不会构造不存在的高权限命令。

## AI Commit 集成

新增 `Pi` CLI 提供商，内部 ID 为 `pi`，命令名为 `pi`。默认模型留空，使 Pi 使用自身保存的默认模型。

生成命令采用以下结构：

```text
pi --print --no-session --no-tools --no-context-files --no-approve [--model <provider/model>] [--thinking <level>] <prompt>
```

只有用户选择了非空模型时才传递 `--model`。只有用户选择了非空思考等级时才传递 `--thinking`。提示词作为独立进程参数传递，继续由现有 `CliProcess` 负责 Windows 参数转义、取消和超时处理。

支持的思考等级为：

```text
off, minimal, low, medium, high, xhigh, max
```

Pi 会根据所选模型能力限制实际等级。插件保存用户选择，不自行猜测每个模型支持的等级。

生成失败时沿用现有错误处理：认证、凭证和登录相关错误追加认证提示；从提交窗口触发的生成失败继续弹出快速模型列表。

## 模型发现

模型列表通过本机命令获取：

```text
pi --list-models
```

Pi 输出一个带表头的文本表格，前两列分别是 `provider` 和 `model`。解析器将每一行组合为 `provider/model`，跳过表头、空行和无法识别的提示文本，并去重保序。

模型列表只包含 Pi 当前认证环境中可用的模型，因此不设置静态模型兜底。命令失败、Pi 未安装或尚未认证时返回空列表，界面保留用户手工输入模型的能力。

模型获取继续在后台线程执行，不能阻塞 Swing EDT。设置面板切换到 Pi 时先显示缓存，再后台刷新；快速模型菜单使用缓存，并由现有后台刷新流程更新。

## Windows 检测

沿用统一的 CLI 检测机制，检查以下启动器名称：

```text
pi.exe, pi.cmd, pi.bat, pi
```

在现有 PATH 和常见 Node.js 全局目录之外，增加 Pi 官方 Windows 安装器使用的目录：

```text
%USERPROFILE%\.pi\agent\bin
```

官方安装器的旧式 npm 安装仍由 `%APPDATA%\npm` 和其他现有 Node.js 路径覆盖。插件不会自动安装 Pi；检测不到时显示现有的 CLI 未找到提示。

## CLI 工具窗口

在 `CliTool` 中增加 `PI`，基本命令为 `pi`。在 `plugin.xml` 注册 `PiToolWindowFactory`，并增加与现有单色工具窗口风格一致的 `pi.svg`。

CLI Tools 设置页增加 Pi 图标显示开关。高权限区域只显示确实支持额外高权限参数的工具，因此 Pi 不显示高权限复选框。为此给 `CliTool` 增加是否支持高权限模式的元数据，避免以后对其他无此参数的 CLI 显示无效配置。

旧的设置文件没有 Pi 字段时，Pi 图标默认显示。现有 CLI 的图标和高权限设置保持原值。

## 本地化和文档

中英文资源增加 Pi 的名称、说明和认证提示。插件描述、README、README_CN、FEATURE_SUMMARY 和 FINAL_SUMMARY 中的 CLI 列表加入 Pi，并明确 Pi 与 OMP 是不同工具。

## 测试

实现采用测试先行方式，至少覆盖：

1. `SdkDefinition.fromProvider("pi")` 返回 Pi 定义，默认模型为空，思考等级正确。
2. Windows 可执行文件候选包含 `pi.exe`、`pi.cmd`、`pi.bat` 和 `pi`。
3. Windows 安装目录包含 `~\.pi\agent\bin`。
4. Pi 模型表格能解析为有序、去重的 `provider/model` 列表，并跳过表头和无效行。
5. Pi 生成命令始终包含非交互、安全和无会话参数。
6. 模型和思考等级为空时不传对应参数；非空时按官方格式传递。
7. Pi CLI 工具窗口使用 `pi` 命令，且不支持高权限开关。
8. 设置状态能保存和恢复 Pi 图标显示选项。
9. 中英文资源键保持一致，插件 XML 能通过构建校验。

完成针对性测试后运行完整 `test` 和 `buildPlugin`。不安装缺失依赖，不执行任何 Git 写操作。

## 验收标准

- AI Commit 设置的 CLI 列表可选择 Pi。
- Pi 已安装且已认证时，刷新能显示其全部可用模型。
- 选择 Pi 模型和思考等级后可以生成提交信息。
- 模型留空时使用 Pi 自身默认模型。
- 生成流程不保存 Pi 会话、不调用工具，也不读取项目级上下文文件。
- 右侧边栏可打开 Pi 终端，设置页可控制其图标显隐。
- Pi 不出现无效的高权限复选框。
- Windows 官方安装器和 npm 安装的 Pi 均可被检测。
- 完整测试和插件构建通过。
