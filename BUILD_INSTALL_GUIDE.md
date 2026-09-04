# SubMsg v2.0.0 - 构建安装说明

## 🚨 重要提示

项目需要 **Java 11 或更高版本**才能构建。

当前系统 Java 版本：**Java 8**  
需要的版本：**Java 11+**

---

## 方法一：使用 IntelliJ IDEA 构建（推荐）

### 步骤：

1. **用 IDEA 打开项目**
   - File → Open → 选择 `D:\subMsg` 目录
   - 等待 IDEA 加载和索引

2. **IDEA 会自动下载正确的 JDK**
   - IDEA 自带 JDK 管理
   - 项目会使用 IDEA 内置的 JDK 11

3. **构建插件**
   - 打开 Gradle 工具窗口（View → Tool Windows → Gradle）
   - 展开 `SubMsg → Tasks → intellij`
   - 双击 `buildPlugin`
   - 等待构建完成（首次需要下载依赖，约 5-10 分钟）

4. **查看构建结果**
   - 构建成功后，插件在：`build/distributions/SubMsg-2.0.8-win.zip`
   - 或 `build/distributions/SubMsg-2.0.0.zip`（取决于 build.gradle.kts 中的版本号）

---

## 方法二：安装 Java 11 后使用命令行

### 步骤：

1. **下载并安装 Java 11**
   
   **选项 A：Oracle JDK 11**
   - 访问：https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html
   - 下载 Windows x64 Installer
   - 安装到默认位置

   **选项 B：OpenJDK 11（推荐，免费）**
   - 访问：https://adoptium.net/
   - 下载 Temurin 11 (LTS)
   - 选择 Windows x64 .msi 安装包
   - 安装时勾选 "Set JAVA_HOME variable"

2. **设置 JAVA_HOME 环境变量**
   ```
   JAVA_HOME=C:\Program Files\Java\jdk-11
   ```
   或根据你的安装路径调整

3. **添加到 PATH**
   ```
   %JAVA_HOME%\bin
   ```

4. **验证 Java 版本**
   ```bash
   java -version
   # 应该显示：java version "11.x.x"
   ```

5. **构建插件**
   ```bash
   cd D:\subMsg
   ./gradlew clean buildPlugin
   ```

6. **查看构建结果**
   ```bash
   dir build\distributions\
   # 应该看到 SubMsg-2.0.0.zip
   ```

---

## 方法三：使用 IDEA 直接测试（无需构建）

### 如果只是想测试功能：

1. **用 IDEA 打开项目**
   - File → Open → `D:\subMsg`

2. **运行插件（调试模式）**
   - 打开 Gradle 工具窗口
   - 展开 `SubMsg → Tasks → intellij`
   - 双击 `runIde`
   - IDEA 会启动一个新的 IDEA 实例，插件已自动加载

3. **在新 IDEA 窗口中测试**
   - Settings → Tools → SubMsg
   - 测试所有功能

---

## 安装插件到 IDEA

### 从构建文件安装：

1. **打开 IDEA**
2. **进入插件设置**
   - File → Settings → Plugins
   - 或按 `Ctrl+Alt+S` → Plugins

3. **安装插件**
   - 点击 ⚙️ 图标
   - 选择 "Install Plugin from Disk..."
   - 选择 `build/distributions/SubMsg-2.0.0.zip`
   - 点击 OK

4. **重启 IDEA**
   - 点击 "Restart IDE" 按钮
   - 等待重启完成

5. **验证安装**
   - Settings → Plugins
   - 搜索 "SubMsg"
   - 应该看到已安装，版本 2.0.0

6. **配置插件**
   - Settings → Tools → SubMsg
   - 选择语言（中文/English）
   - 配置 AI Commit 或 AI Inline
   - 开始使用！

---

## 快速验证清单

安装后检查以下功能：

### AI Commit
- [ ] Settings → Tools → SubMsg 能正常打开
- [ ] 能看到两个标签页：AI Commit 和 AI Inline
- [ ] 语言切换正常（中文/English）
- [ ] CLI 选择下拉框能显示选项
- [ ] 模型选择下拉框能显示选项
- [ ] 点击"测试"按钮有反应

### AI Inline
- [ ] 切换到 AI Inline 标签页
- [ ] 能看到"启用 AI Inline Reference"复选框
- [ ] 能看到触发延迟滑块
- [ ] 能看到最大 Token 输入框
- [ ] 能看到自定义提示词文本框

### 多语言
- [ ] 默认显示中文界面
- [ ] 切换到 English 后重启，界面变为英文
- [ ] 切换回中文后重启，界面恢复中文

---

## 常见问题

### Q1: Gradle 构建失败，提示 Java 版本问题
**A**: 需要 Java 11+。使用方法一（IDEA 构建）最简单。

### Q2: IDEA 构建很慢
**A**: 首次构建需要下载依赖，耐心等待。后续构建会很快。

### Q3: 找不到构建的插件文件
**A**: 查看 `build/distributions/` 目录，文件名可能是 `SubMsg-2.0.8-win.zip`。

### Q4: 安装后插件无法启用
**A**: 检查 IDEA 版本是否 >= 2021.1。

---

## 推荐方法

**最简单**：使用方法三（IDEA runIde）直接测试  
**最快**：使用方法一（IDEA buildPlugin）构建  
**最标准**：使用方法二（安装 Java 11）命令行构建  

---

## 下一步

构建/安装完成后：
1. 阅读 [QUICKSTART_5MIN.md](./QUICKSTART_5MIN.md) 快速上手
2. 阅读 [AI_INLINE_GUIDE.md](./AI_INLINE_GUIDE.md) 了解详细功能
3. 测试所有功能
4. 提供反馈

---

**祝使用愉快！🚀**
