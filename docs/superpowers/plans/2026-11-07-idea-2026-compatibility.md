# IDEA 2026 兼容与更新来源实施记录

**目标：** 校验用户要求的版本范围，并为插件提供真实更新渠道。

**方案：** 继续使用现有 Gradle 构建和 GitHub Release，生成与最终插件描述符一致的更新清单。用户后续要求发布新版，由发布工作流完成四个版本的兼容检查。1.0.2 因跨平台测试断言失败未发布；修复后提升为 1.0.3，保留原标签。

- [x] 检查 2026.3 日志、更新来源实现及已安装版本，确认提示来源。
- [x] 使用官方 Plugin Verifier 校验当前 ZIP 与本机 2026.1.5、2026.3，保存 `tmp/idea-compat/baseline-offline/` 报告。
- [x] 验收现有产物，确认缺少 `build/distributions/updatePlugins.xml`。
- [x] 在 `build.gradle.kts` 增加 `generateUpdatePluginsXml`；清单读取 `patchPluginXml` 的输出并依赖 `buildPlugin`。
- [x] 更新 `.github/workflows/release.yml`，测试后发布明确版本的 ZIP 与清单；更新构建工作流的配套检查和产物。
- [x] 执行 `test verifyPlugin generateUpdatePluginsXml --console=plain`：105 项测试全部通过；核对 ZIP 内 ID、版本、兼容范围和清单下载文件名通过。
- [x] 更新中英文 README、安装说明、验证记录，区分二进制兼容结果与更新仓库上线状态。
- [x] 1.0.2 本机构建、105 项测试和插件结构检查通过；发布工作流配置四版本校验。
- [x] 提交并推送到用户指定的 GitHub 项目 `master`，创建 `v1.0.2` 发布标签；该次发布任务在测试阶段失败。
- [x] 通过 CI 失败注释确认 Linux、macOS 仅 Pi 路径分隔符断言失败；Windows 构建和测试通过。
- [x] 修复跨平台断言，1.0.3 本机 105 项测试、插件结构和更新清单元数据检查通过。
- [ ] 推送修复并创建 `v1.0.3` 标签。
- [ ] 等待发布流程及四版本兼容校验完成，检查远程更新清单与安装包可下载。
- [ ] 通过 IDEA 更新来源设置接入仓库并确认提示消失。

本机检查命令（使用已有环境）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
& 'D:\gradle-7.4.2\bin\gradle.bat' test verifyPlugin generateUpdatePluginsXml --console=plain
```

发布流程使用 GitHub 的运行器完成缺少版本的检查，不向用户本机安装额外 IDEA。用户当前配置的 `origin` 是其他 Git 服务；本次发布使用明确的 GitHub 地址，不修改已有远程配置。
