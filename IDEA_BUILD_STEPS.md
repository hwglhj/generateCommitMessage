# IntelliJ IDEA 构建插件详细步骤

## 📋 准备工作

**项目位置**：`D:\subMsg`  
**目标**：构建 SubMsg-2.0.0.zip 插件包

---

## 🔨 方法：使用 IDEA 的 Gradle 工具

### 第一步：打开项目

1. 启动 IntelliJ IDEA
2. 选择 **File → Open**
3. 导航到 `D:\subMsg` 目录
4. 点击 **OK**

### 第二步：等待项目加载

IDEA 会自动：
- ✅ 检测到这是一个 Gradle 项目
- ✅ 自动下载 Gradle（如果需要）
- ✅ 自动下载 JDK 11（IDEA 自带）
- ✅ 开始索引项目文件

**等待时间**：首次打开约 2-5 分钟

你会在右下角看到进度条：
```
Building...
Indexing...
Resolving dependencies...
```

### 第三步：打开 Gradle 工具窗口

**方法 A**：通过菜单
- View → Tool Windows → Gradle

**方法 B**：快捷键
- 点击右侧边栏的 **Gradle** 按钮
- 或者按 `Ctrl+Shift+A` 搜索 "Gradle"

### 第四步：刷新 Gradle 项目（可选但推荐）

在 Gradle 工具窗口中：
1. 点击左上角的 🔄 **Reload All Gradle Projects** 按钮
2. 等待刷新完成（约 30 秒 - 2 分钟）

这一步会确保所有依赖都正确下载。

### 第五步：构建插件

在 Gradle 工具窗口中：

1. **展开项目树**：
   ```
   SubMsg
   └── Tasks
       └── intellij
           ├── buildPlugin          ← 双击这个
           ├── runIde
           ├── buildSearchableOptions
           └── ...
   ```

2. **双击 `buildPlugin`**

3. **观察构建过程**：
   - 底部会出现 **Build** 窗口
   - 显示构建进度
   ```
   > Task :compileKotlin
   > Task :compileJava
   > Task :processResources
   > Task :classes
   > Task :jar
   > Task :buildPlugin
   ```

4. **等待完成**：
   - 首次构建：约 3-10 分钟（下载依赖）
   - 后续构建：约 30 秒 - 2 分钟

### 第六步：查看构建结果

构建成功后，你会看到：

**在 Build 窗口**：
```
BUILD SUCCESSFUL in 2m 34s
```

**插件位置**：
```
D:\subMsg\build\distributions\SubMsg-2.0.0.zip
```

### 第七步：查找构建的插件

**方法 A**：通过文件浏览器
1. 打开 `D:\subMsg\build\distributions\`
2. 找到 `SubMsg-2.0.0.zip`
3. 文件大小约 100-500 KB

**方法 B**：在 IDEA 中查看
1. 在 IDEA 左侧的 Project 面板
2. 展开 `build → distributions`
3. 右键 `SubMsg-2.0.0.zip`
4. 选择 **Show in Explorer**

---

## 🚀 安装插件到 IDEA

### 步骤：

1. **打开插件设置**
   - `File → Settings`（或 `Ctrl+Alt+S`）
   - 选择 `Plugins`

2. **安装插件**
   - 点击 ⚙️ 图标（右上角）
   - 选择 **Install Plugin from Disk...**
   - 导航到 `D:\subMsg\build\distributions\`
   - 选择 `SubMsg-2.0.0.zip`
   - 点击 **OK**

3. **确认安装**
   - IDEA 会显示插件信息
   - 点击 **OK**

4. **重启 IDEA**
   - 点击 **Restart IDE** 按钮
   - 等待 IDEA 重启

5. **验证安装**
   - `Settings → Plugins`
   - 搜索 "SubMsg"
   - 应该看到：
     ```
     ✅ SubMsg v2.0.0
     Installed
     ```

---

## ✅ 测试插件

### 1. 打开配置界面

- `File → Settings → Tools → SubMsg`
- 或者 `Ctrl+Alt+S` → 搜索 "SubMsg"

### 2. 检查界面

**应该看到**：

```
┌──────────────────────────────────┐
│ SubMsg                            │
│                                  │
│ 语言: [中文 ▼]                    │
│                                  │
│ ┌─ AI Commit ─┬─ AI Inline ─┐  │
│ │                            │  │
│ │ 使用模式: [CLI 模式 ▼]      │  │
│ │                            │  │
│ │ CLI: [Claude ▼]            │  │
│ │ 模型: [claude-sonnet-4-6 ▼]│  │
│ │                            │  │
│ │ [测试]  [刷新模型]          │  │
│ └────────────────────────────┘  │
└──────────────────────────────────┘
```

### 3. 测试功能

#### 测试 AI Commit：
1. 选择 CLI 模式
2. 选择一个 CLI（如果已安装）
3. 点击"测试"按钮

#### 测试 AI Inline：
1. 切换到 **AI Inline** 标签页
2. 查看所有配置选项
3. 调整触发延迟滑块

#### 测试多语言：
1. 切换语言到 **English**
2. 点击 **Apply**
3. 重启 IDEA
4. 再次打开配置，界面应该是英文

---

## 🔍 故障排除

### 问题 1：构建失败，提示找不到 Kotlin 编译器

**解决方案**：
1. 在 Gradle 窗口点击 🔄 刷新
2. 等待依赖下载完成

### 问题 2：构建很慢

**原因**：首次构建需要下载：
- Gradle 依赖
- IntelliJ Platform SDK
- Kotlin 标准库

**建议**：耐心等待，后续构建会很快。

### 问题 3：找不到 Gradle 窗口

**解决方案**：
- View → Tool Windows → Gradle
- 或者按 `Ctrl+Shift+A` 搜索 "Gradle"

### 问题 4：构建成功但找不到 zip 文件

**检查位置**：
```
D:\subMsg\build\distributions\
```

**文件名可能是**：
- `SubMsg-2.0.0.zip`
- 或其他版本号

### 问题 5：安装后插件无法启用

**检查 IDEA 版本**：
- Help → About
- 需要 2021.1 或更高版本

---

## 📝 快速命令清单

**如果已经打开过项目**：

1. 打开 IDEA
2. 打开最近项目：`D:\subMsg`
3. 打开 Gradle 窗口（右侧边栏）
4. 双击 `Tasks → intellij → buildPlugin`
5. 等待完成
6. 查看 `build\distributions\SubMsg-2.0.0.zip`

**总耗时**：约 1-3 分钟（非首次构建）

---

## 🎯 构建选项说明

### buildPlugin
- **用途**：构建可安装的插件 zip 包
- **输出**：`build/distributions/SubMsg-2.0.0.zip`
- **推荐**：用于正式安装

### runIde
- **用途**：启动测试 IDEA 实例，插件已加载
- **输出**：新的 IDEA 窗口
- **推荐**：用于快速测试

### clean
- **用途**：清理构建缓存
- **使用**：构建出问题时运行

---

## 💡 提示

1. **首次构建会比较慢**，请耐心等待
2. **后续构建很快**，通常 30 秒内完成
3. **使用 runIde** 可以不构建直接测试
4. **构建前可以先 clean** 确保干净构建

---

## 📞 需要帮助？

如果遇到问题：
1. 查看 IDEA 底部的 **Build** 窗口错误信息
2. 查看 **Event Log**（右下角）的警告
3. 检查本文档的"故障排除"部分

---

**现在请按照上述步骤在 IDEA 中构建插件！** 🚀

构建完成后告诉我，我们继续测试！
