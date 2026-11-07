# IDEA 2026 兼容与更新渠道

本轮目标版本为 IntelliJ IDEA 2026.1.5 至 2026.3。最低平台声明继续为 `222`，插件使用 Java 17 字节码，需启用 IDEA 自带的 Terminal 插件。

## 版本检查

1.0.3 起，GitHub Release 工作流使用 JetBrains Plugin Verifier 1.411 检查以下版本。每个版本独立下载官方 IDE 发行包，报告必须为 `Compatible`，全部通过后才上传正式 Release。

| IDEA 版本 | 平台构建号 | 发布要求 |
| --- | --- | --- |
| 2026.1.5 | IU-261.27258.48 | Compatible |
| 2026.2 | IU-262.8665.258 | Compatible |
| 2026.2.4 | IU-262.11879.100 | Compatible |
| 2026.3 | IU-263.7307.200 | Compatible |

各版本报告保存在 [Release 工作流](https://github.com/hwglhj/generateCommitMessage/actions/workflows/release.yml) 的 `compatibility-版本号` 产物中。兼容检查检验类和方法的链接；完整交互验收按 [安装与验收](../INSTALL_GUIDE.md) 执行。

2026-11-07 本机 Windows 上的 2026.1.5、2026.3 已对升级前的同一功能代码检查通过，报告在 `tmp/idea-compat/baseline-offline/`；仍有部分 Popup、Action 和 Terminal API 的废弃警告。`runPluginVerifier` 也配置了上述四个目标，供本地维护使用。

## “Unable to identify where … was installed from” 的原因

这条提示来自 IDEA 的插件更新来源检查。从磁盘安装或从旧 IDE 迁移的插件，如果不在 Marketplace 或已配置的自定义仓库中，IDEA 就无法确定应该从哪里获取更新。

本机 2026.3 日志已确认插件 1.0.1 加载成功；它的更新来源记录缺失。插件的 GitHub 项目主页和 `vendor` 地址不属于 IDEA 可读取的更新仓库。

## 配置更新来源

1. 1.0.3 的正式 Release 同时提供 `generateCommitMessage-1.0.3.zip` 和 `updatePlugins.xml`。
2. 在 IDEA 的 **设置 > 插件 > 齿轮 > 管理插件仓库** 中添加：

   ```text
   https://github.com/hwglhj/generateCommitMessage/releases/latest/download/updatePlugins.xml
   ```

3. 在 **Installed / 已安装 > Generate Commit Message** 的更新来源选择中，选中该自定义仓库，再应用设置。
4. 检查插件详情页是否显示该更新来源；后续更新由 IDEA 从同一仓库获取。

仍可直接从磁盘安装 ZIP 使用插件。首次磁盘安装或跨版本迁移后，需要在 IDEA 中选择一次更新来源。

## 生成发布产物

```powershell
& 'D:\gradle-7.4.2\bin\gradle.bat' test verifyPlugin generateUpdatePluginsXml --console=plain
```

产物位于 `build/distributions/`。更新清单从构建后的 `plugin.xml` 读取插件 ID、版本和兼容范围，下载地址指向同版本 GitHub Release 的 ZIP。改版本时无需另手工修改清单。

Release 工作流在测试、结构检查和四个版本的兼容检查成功后上传这两个文件，并检查标签版本对应的 ZIP 存在。
