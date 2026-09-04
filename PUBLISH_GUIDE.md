# 🚀 SubMsg 发布到 GitHub 快速指南

## 前置准备

### 1. 确认本地环境

```bash
# 检查 Git 版本
git --version

# 检查是否在项目根目录
cd D:/subMsg
ls -la
```

### 2. 在 GitHub 创建仓库

1. 访问 https://github.com/new
2. 填写信息：
   - **Repository name**: `submsg`
   - **Description**: `AI-powered Git commit message generator for IntelliJ IDEA`
   - **Visibility**: Public
   - **不要**初始化 README、.gitignore 或 LICENSE（本地已有）
3. 点击 "Create repository"

## 发布步骤

### 步骤 1：初始化 Git 仓库

```bash
cd D:/subMsg

# 初始化仓库
git init

# 检查状态
git status
```

### 步骤 2：添加文件

```bash
# 添加所有文件
git add .

# 查看将要提交的文件
git status

# 应该看到：
# - 11 个 .kt 源文件
# - plugin.xml
# - build.gradle.kts
# - 所有 Markdown 文档
# - GitHub Actions 配置
# - .gitignore
# - LICENSE
```

### 步骤 3：第一次提交

```bash
git commit -m "🎉 Initial release: SubMsg 1.0.0

- Complete UI redesign with optimized layout
- Full Windows compatibility (CLI detection, path handling)
- Support for 7 CLI tools and unlimited API providers
- Comprehensive documentation (EN + CN)
- Ready for production use

Based on cli-tools-idea 2.0.7 by @l-ning
Author: hwglhj"
```

### 步骤 4：设置主分支

```bash
# 重命名为 main 分支
git branch -M main
```

### 步骤 5：关联远程仓库

```bash
# 添加远程仓库（替换为你的用户名）
git remote add origin https://github.com/hwglhj/submsg.git

# 验证远程仓库
git remote -v
```

### 步骤 6：推送代码

```bash
# 首次推送
git push -u origin main
```

如果需要输入凭据：
- **Username**: 你的 GitHub 用户名
- **Password**: 使用 Personal Access Token（不是密码）

#### 创建 Personal Access Token（如果需要）

1. 访问 https://github.com/settings/tokens
2. 点击 "Generate new token" → "Generate new token (classic)"
3. 勾选权限：
   - `repo` (完整仓库访问权限)
   - `workflow` (GitHub Actions)
4. 生成并复制 token
5. 在 git push 时使用 token 作为密码

### 步骤 7：创建 Release Tag

```bash
# 创建带注释的标签
git tag -a v1.0.0 -m "Release version 1.0.0

🎉 First stable release of SubMsg

Features:
- AI-powered commit message generation
- CLI mode: Claude, Codex, Cursor, OpenCode, Gemini, Grok, OMP
- API mode: OpenAI, Anthropic, and any compatible API
- Windows-optimized CLI detection
- Custom prompts support
- Comprehensive documentation

Changes:
- Complete UI redesign
- Code refactoring and simplification
- Windows compatibility improvements
- Full documentation suite (10 docs, ~19k words)

Based on cli-tools-idea 2.0.7
Author: hwglhj
License: MIT"

# 推送标签
git push origin v1.0.0
```

### 步骤 8：等待自动构建

推送 tag 后，GitHub Actions 会自动：
1. 检出代码
2. 构建插件
3. 运行验证
4. 创建 Release
5. 上传 `SubMsg-1.0.0.zip`

查看构建状态：
- 访问 https://github.com/hwglhj/submsg/actions

### 步骤 9：完善 Release 信息（可选）

1. 访问 https://github.com/hwglhj/submsg/releases
2. 找到 `v1.0.0` Release
3. 点击 "Edit release"
4. 添加截图或其他信息
5. 点击 "Update release"

## 验证发布

### 检查清单

- [ ] 代码已推送到 GitHub
- [ ] README 正确显示
- [ ] Actions 构建成功
- [ ] Release 已创建
- [ ] zip 文件可下载
- [ ] 文档链接正常工作

### 测试下载

1. 访问 https://github.com/hwglhj/submsg/releases/latest
2. 下载 `SubMsg-1.0.0.zip`
3. 在 IDEA 中安装测试

```bash
# 或使用命令行下载
curl -L -o SubMsg-1.0.0.zip \
  https://github.com/hwglhj/submsg/releases/download/v1.0.0/SubMsg-1.0.0.zip
```

## 后续维护

### 日常开发流程

```bash
# 1. 创建新分支
git checkout -b feature/new-feature

# 2. 修改代码
# ... 编码 ...

# 3. 提交更改
git add .
git commit -m "✨ feat: add new feature"

# 4. 推送分支
git push origin feature/new-feature

# 5. 创建 Pull Request
# 在 GitHub 网页上操作

# 6. 合并后删除分支
git checkout main
git pull
git branch -d feature/new-feature
```

### 发布新版本

```bash
# 1. 更新版本号
# - build.gradle.kts: version = "1.1.0"
# - plugin.xml: <version>1.1.0</version>

# 2. 更新 CHANGELOG.md
# 添加新版本的更新内容

# 3. 提交版本更新
git add .
git commit -m "🔖 chore: bump version to 1.1.0"
git push

# 4. 创建新标签
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin v1.1.0

# 5. GitHub Actions 自动构建和发布
```

## 常见问题

### Q: git push 失败，提示 "Permission denied"

A: 确认你的 Personal Access Token 有正确的权限：
```bash
# 重新添加远程仓库
git remote remove origin
git remote add origin https://YOUR_TOKEN@github.com/hwglhj/submsg.git
git push -u origin main
```

### Q: Actions 构建失败

A: 检查以下问题：
1. `gradlew` 是否有执行权限
2. Gradle 配置是否正确
3. Java 版本是否匹配
4. 查看 Actions 日志获取详细错误

### Q: 如何删除错误的 Release？

A:
```bash
# 1. 删除远程标签
git push --delete origin v1.0.0

# 2. 删除本地标签
git tag -d v1.0.0

# 3. 在 GitHub 网页删除 Release
# https://github.com/hwglhj/submsg/releases
# 点击对应 Release 的 "Delete"

# 4. 重新创建正确的标签
git tag -a v1.0.0 -m "..."
git push origin v1.0.0
```

### Q: 如何设置仓库默认分支为 main？

A: 访问仓库 Settings：
1. https://github.com/hwglhj/submsg/settings/branches
2. Default branch 选择 `main`
3. 点击更新

## 推广你的项目

### 1. 添加 Topics

在仓库页面点击 ⚙️ Settings → Topics，添加：
- `intellij-plugin`
- `git-commit`
- `ai`
- `claude`
- `commit-message`
- `developer-tools`

### 2. 社交媒体分享

发布到：
- Twitter/X
- Reddit (r/IntelliJIDEA, r/programming)
- Hacker News
- Product Hunt

### 3. 提交到列表

- [Awesome IntelliJ IDEA Plugins](https://github.com/JetBrains/awesome-intellij-plugins)
- [Awesome AI Tools](https://github.com/mahseema/awesome-ai-tools)

### 4. 撰写博客

写一篇介绍文章：
- 为什么创建这个插件
- 技术实现细节
- 使用教程和最佳实践

## 🎉 完成！

恭喜！你的项目现在已经：
- ✅ 开源在 GitHub
- ✅ 有完整的文档
- ✅ 配置了自动构建
- ✅ 可供全世界开发者使用

**项目地址**: https://github.com/hwglhj/submsg

**下载地址**: https://github.com/hwglhj/submsg/releases

---

有问题？查看 [GitHub Docs](https://docs.github.com/) 或在 [Discussions](https://github.com/hwglhj/submsg/discussions) 提问。
