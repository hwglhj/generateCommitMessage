# SubMsg v2.0.0 - 构建和测试指南

## 📋 目录

1. [构建准备](#构建准备)
2. [构建插件](#构建插件)
3. [测试指南](#测试指南)
4. [发布流程](#发布流程)
5. [故障排除](#故障排除)

---

## 🛠️ 构建准备

### 系统要求

- **操作系统**：Windows 10/11, macOS 10.15+, Linux
- **JDK**：Java 11 或更高版本
- **Gradle**：7.0+（使用 Gradle Wrapper，无需手动安装）
- **IntelliJ IDEA**：2021.1+ （用于测试）

### 检查环境

**检查 Java 版本**：
```bash
java -version
# 应输出：java version "11" 或更高
```

**检查 Gradle**：
```bash
cd D:\subMsg
./gradlew --version
# Windows 使用：gradlew.bat --version
```

**检查项目结构**：
```bash
ls -la D:\subMsg
# 应包含：
# - src/
# - build.gradle.kts
# - gradlew
# - gradlew.bat
```

---

## 🔨 构建插件

### 方法 1：使用命令行（推荐）

**Windows**：
```bash
cd D:\subMsg

# 清理旧构建
gradlew.bat clean

# 构建插件
gradlew.bat buildPlugin

# 查看构建结果
dir build\distributions\
```

**macOS/Linux**：
```bash
cd /path/to/subMsg

# 清理旧构建
./gradlew clean

# 构建插件
./gradlew buildPlugin

# 查看构建结果
ls -lh build/distributions/
```

**预期输出**：
```
build/distributions/
├── SubMsg-2.0.0.zip         # 插件安装包
└── SubMsg-2.0.0-sources.jar # 源码包（可选）
```

### 方法 2：使用 IntelliJ IDEA

1. 用 IntelliJ IDEA 打开项目
2. 打开 Gradle 工具窗口（View → Tool Windows → Gradle）
3. 展开 `SubMsg → Tasks → intellij`
4. 双击 `buildPlugin`
5. 等待构建完成
6. 在 `build/distributions/` 目录查看结果

### 方法 3：使用 IDE Run Configuration

1. 在 IDEA 中，点击 `Run → Edit Configurations...`
2. 点击 `+` → `Gradle`
3. 配置：
   - Name: `Build SubMsg Plugin`
   - Gradle project: `SubMsg`
   - Tasks: `clean buildPlugin`
4. 点击 `OK`
5. 点击运行按钮 ▶️

---

## 🧪 测试指南

### 1. 安装测试

**在测试环境中安装**：

1. 准备干净的测试环境
   - 新安装的 IntelliJ IDEA
   - 或创建新的配置目录

2. 安装插件
   - `Settings → Plugins → ⚙️ → Install Plugin from Disk...`
   - 选择 `build/distributions/SubMsg-2.0.0.zip`
   - 点击 `OK`

3. 重启 IDEA

4. 验证安装
   - `Settings → Plugins` 中能看到 "SubMsg"
   - 版本号显示为 "2.0.0"

### 2. 功能测试清单

#### 2.1 基本配置测试

**语言切换**：
- [ ] 打开 `Settings → Tools → SubMsg`
- [ ] 默认显示中文界面
- [ ] 切换到 "English"
- [ ] 点击 Apply，提示需要重启
- [ ] 重启后界面变为英文
- [ ] 切换回 "中文"，重启后恢复中文

**标签页布局**：
- [ ] 能看到两个标签页："AI Commit" 和 "AI Inline"
- [ ] 点击标签页能正常切换
- [ ] 两个标签页内容正确显示

#### 2.2 AI Commit 功能测试

**CLI 模式**：
1. [ ] 选择 "使用模式" → "CLI 模式"
2. [ ] 选择一个已安装的 CLI（如 Claude）
3. [ ] 点击 "刷新模型" 按钮
4. [ ] 模型列表正确加载
5. [ ] 选择一个模型
6. [ ] 点击 "测试" 按钮
7. [ ] 显示测试成功消息

**生成 Commit Message**：
1. [ ] 在项目中修改一些代码
2. [ ] 打开 VCS 提交界面（Ctrl+K / Cmd+K）
3. [ ] 选择要提交的文件
4. [ ] 点击 "AI Generate Commit Message" 按钮
5. [ ] 等待生成（显示进度条）
6. [ ] 查看生成的提交信息
7. [ ] 提交信息合理、规范

**API 模式**：
1. [ ] 选择 "使用模式" → "API 模式"
2. [ ] 点击 "添加" 创建新配置
3. [ ] 填写配置信息（OpenAI/Anthropic 等）
4. [ ] 点击 "测试" 验证连接
5. [ ] 保存配置
6. [ ] 在下拉框中选择该配置
7. [ ] 使用该配置生成 Commit Message

#### 2.3 AI Inline 功能测试

**启用功能**：
1. [ ] 切换到 "AI Inline" 标签页
2. [ ] 勾选 "启用 AI Inline Reference"
3. [ ] 选择 "使用模式" → "CLI 模式"
4. [ ] 选择 CLI 和模型
5. [ ] 点击 "测试" 验证
6. [ ] 保存配置

**代码补全**：
1. [ ] 在编辑器中打开一个代码文件
2. [ ] 开始输入代码（如函数名）
3. [ ] 等待 500ms（触发延迟）
4. [ ] 补全列表中出现 AI 建议（标记为 "AI"）
5. [ ] 选择一个 AI 建议
6. [ ] 代码正确插入

**配置调整**：
1. [ ] 调整触发延迟滑块（100-2000ms）
2. [ ] 调整最大 Token 数（50-500）
3. [ ] 在自定义提示词中输入文本
4. [ ] 保存配置
5. [ ] 验证新配置生效

**API 模式**：
1. [ ] 切换到 "API 模式"
2. [ ] 在 API 配置下拉框中选择一个配置
3. [ ] 测试代码补全
4. [ ] 验证 API 模式正常工作

#### 2.4 多语言测试

**中文界面**：
- [ ] 所有标签和按钮显示中文
- [ ] 错误消息显示中文
- [ ] 通知消息显示中文
- [ ] 右键菜单显示中文

**英文界面**：
- [ ] 所有标签和按钮显示英文
- [ ] 错误消息显示英文
- [ ] 通知消息显示英文
- [ ] 右键菜单显示英文

**消息测试**：
1. [ ] 触发各种成功消息
2. [ ] 触发各种错误消息
3. [ ] 触发各种警告消息
4. [ ] 所有消息都正确翻译

### 3. 跨平台测试

**Windows 测试**：
- [ ] CLI 检测正常
- [ ] 模型列表加载正常
- [ ] 路径识别正确（支持 Windows 路径）
- [ ] 命令执行正常
- [ ] UI 显示正常

**macOS 测试**：
- [ ] CLI 检测正常（Homebrew 路径）
- [ ] 模型列表加载正常
- [ ] 快捷键正常（Cmd 键）
- [ ] UI 显示正常

**Linux 测试**：
- [ ] CLI 检测正常（/usr/local/bin 等）
- [ ] 模型列表加载正常
- [ ] UI 显示正常

### 4. 压力测试

**频繁操作**：
- [ ] 连续多次生成 Commit Message
- [ ] 连续多次触发 AI Inline 补全
- [ ] 快速切换标签页
- [ ] 快速切换语言
- [ ] 多次添加/删除 API 配置

**大文件测试**：
- [ ] 在大文件中触发补全（1000+ 行）
- [ ] 提交大量文件（50+ 个文件）
- [ ] 长时间运行（1+ 小时）

**边界情况**：
- [ ] 没有安装任何 CLI
- [ ] 网络断开（API 模式）
- [ ] API Key 无效
- [ ] 模型不存在
- [ ] 空的自定义提示词
- [ ] 极端的触发延迟（100ms / 2000ms）
- [ ] 极端的 Token 数（50 / 500）

### 5. 性能测试

**响应时间**：
- [ ] 打开设置界面 < 1 秒
- [ ] 刷新模型列表 < 5 秒
- [ ] 生成 Commit Message < 10 秒（取决于模型）
- [ ] AI Inline 补全 < 3 秒

**资源占用**：
- [ ] 内存占用合理（< 100MB）
- [ ] CPU 占用合理（空闲时 < 5%）
- [ ] 不影响 IDE 性能

---

## 📦 发布流程

### 1. 发布前检查

**代码检查**：
```bash
# 检查是否有未提交的更改
git status

# 检查版本号
grep "version" build.gradle.kts
# 应显示：version = "2.0.0"

grep "version" src/main/resources/META-INF/plugin.xml
# 应显示：<version>2.0.0</version>
```

**文档检查**：
- [ ] README_v2.md 已更新
- [ ] CHANGELOG_v2.0.md 已完成
- [ ] 所有指南文档已完善
- [ ] 链接和示例正确

**测试检查**：
- [ ] 所有功能测试通过
- [ ] 跨平台测试通过
- [ ] 性能测试通过
- [ ] 边界情况测试通过

### 2. 创建 Git Tag

```bash
# 提交所有更改
git add .
git commit -m "Release v2.0.0"

# 创建 tag
git tag -a v2.0.0 -m "SubMsg v2.0.0 - AI Inline Reference + Multilang Support"

# 推送到远程
git push origin main
git push origin v2.0.0
```

### 3. 创建 GitHub Release

1. 访问 GitHub 项目页面
2. 点击 "Releases" → "Create a new release"
3. 填写信息：
   - **Tag**: v2.0.0
   - **Title**: SubMsg v2.0.0 - AI Inline Reference
   - **Description**: 复制 CHANGELOG_v2.0.md 的内容
4. 上传文件：
   - `SubMsg-2.0.0.zip`
   - （可选）`SubMsg-2.0.0-sources.jar`
5. 点击 "Publish release"

### 4. 更新文档

**更新主 README**：
```bash
# 用 README_v2.md 替换 README.md
cp README_v2.md README.md
git add README.md
git commit -m "Update README for v2.0.0"
git push
```

**添加下载链接**：
在 README.md 中添加：
```markdown
## 下载

- [最新版本 v2.0.0](https://github.com/hwglhj/submsg/releases/tag/v2.0.0)
- [所有版本](https://github.com/hwglhj/submsg/releases)
```

### 5. 发布公告

**在 GitHub Discussions 发帖**：
```markdown
# SubMsg v2.0.0 正式发布！

很高兴宣布 SubMsg v2.0.0 正式发布！

## 主要更新

- 🚀 新增 AI Inline Reference 功能
- 🌍 支持中英文界面切换
- ⚙️ 支持自定义 API 配置
- 🎨 全新标签页 UI 设计

## 下载

[点击下载 SubMsg-2.0.0.zip](...)

## 文档

- [完整更新日志](./CHANGELOG_v2.0.md)
- [AI Inline 使用指南](./AI_INLINE_GUIDE.md)
- [多语言功能指南](./MULTILANG_GUIDE.md)

欢迎使用和反馈！
```

---

## 🔍 故障排除

### 构建失败

**问题**：`gradlew buildPlugin` 失败

**解决方案**：
```bash
# 1. 清理缓存
gradlew.bat clean

# 2. 刷新依赖
gradlew.bat --refresh-dependencies

# 3. 检查 Java 版本
java -version

# 4. 检查 Gradle 版本
gradlew.bat --version

# 5. 查看详细错误
gradlew.bat buildPlugin --stacktrace
```

### 插件无法安装

**问题**：IDEA 提示插件不兼容

**解决方案**：
1. 检查 plugin.xml 中的 `sinceBuild` 和 `untilBuild`
2. 确保 IDEA 版本 >= 2021.1
3. 尝试在更新的 IDEA 版本中安装

**问题**：安装后无法启用

**解决方案**：
1. 查看 IDEA 日志：`Help → Show Log in Explorer`
2. 搜索 "SubMsg" 相关错误
3. 检查依赖是否正确

### 功能不工作

**问题**：CLI 检测失败

**解决方案**：
```bash
# Windows
where claude
where codex

# macOS/Linux
which claude
which codex

# 手动指定 CLI 路径
# 在设置中输入完整路径
```

**问题**：API 连接失败

**解决方案**：
1. 测试网络连接
   ```bash
   curl https://api.openai.com/v1/models
   ```
2. 检查 API Key 是否有效
3. 检查 Base URL 是否正确
4. 查看 IDEA 日志获取详细错误

**问题**：AI Inline 不触发

**解决方案**：
1. 检查是否已启用
2. 检查触发延迟设置
3. 尝试手动触发（Ctrl+Space）
4. 查看日志中的错误信息

---

## 📊 测试报告模板

```markdown
# SubMsg v2.0.0 测试报告

**测试人员**：[姓名]
**测试时间**：[日期]
**测试环境**：
- OS: Windows 11 / macOS / Linux
- IDEA: 2024.2
- JDK: Java 11

## 功能测试结果

### AI Commit
- [ ] 通过 / [ ] 失败
- 备注：

### AI Inline
- [ ] 通过 / [ ] 失败
- 备注：

### 多语言
- [ ] 通过 / [ ] 失败
- 备注：

### API 配置
- [ ] 通过 / [ ] 失败
- 备注：

## 发现的问题

1. [问题描述]
   - 重现步骤：
   - 预期结果：
   - 实际结果：
   - 严重程度：高/中/低

## 性能测试

- 响应时间：[数据]
- 资源占用：[数据]
- 稳定性：[评价]

## 总体评价

[通过 / 需要修复]

## 建议

[改进建议]
```

---

## ✅ 发布检查清单

### 构建阶段
- [ ] 代码编译成功
- [ ] 单元测试通过
- [ ] 插件打包成功
- [ ] 文件大小合理（< 5MB）

### 测试阶段
- [ ] 安装测试通过
- [ ] 功能测试通过
- [ ] 跨平台测试通过
- [ ] 性能测试通过
- [ ] 压力测试通过

### 文档阶段
- [ ] README 已更新
- [ ] CHANGELOG 已完成
- [ ] 所有指南已完善
- [ ] 链接全部有效

### 发布阶段
- [ ] Git tag 已创建
- [ ] GitHub Release 已创建
- [ ] 文件已上传
- [ ] 发布公告已发布

### 后续支持
- [ ] 监控 Issues
- [ ] 响应用户反馈
- [ ] 收集改进建议
- [ ] 计划下一版本

---

**准备就绪，可以发布！🚀**
