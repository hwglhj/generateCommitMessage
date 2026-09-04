# IDEA 中快速启动指南

## ✅ 当前状态

**编译状态**: ✅ 所有问题已修复，构建成功
**最后构建**: 2026-09-04

## 🚀 在 IDEA 中打开项目

### 方法 1: 直接打开
1. 启动 IntelliJ IDEA
2. 选择 `File → Open`
3. 浏览到 `D:\subMsg` 并选择该文件夹
4. 点击 OK

### 方法 2: 从欢迎界面
1. 在 IDEA 欢迎界面点击 `Open`
2. 选择 `D:\subMsg`
3. 点击 OK

## ⚙️ 首次加载步骤

1. **信任项目**
   - 当提示 "Trust and Open Gradle Project?" 时，点击 **Trust Project**

2. **等待 Gradle 同步**
   - IDEA 会自动开始 Gradle 同步
   - 右下角会显示进度条
   - 首次同步可能需要 2-5 分钟（下载依赖）

3. **等待索引**
   - 同步完成后，IDEA 会开始建立索引
   - 右下角显示 "Indexing..."
   - 等待索引完成（通常 1-2 分钟）

## 🔧 运行和测试

### 运行插件（在新的 IDEA 实例中测试）
1. 打开右侧的 **Gradle** 面板
   - 如果没有看到，点击 `View → Tool Windows → Gradle`

2. 展开：`subMsg → Tasks → intellij → runIde`

3. 双击 `runIde`
   - 会启动一个新的 IDEA 实例，插件已安装

4. 在新窗口中测试插件功能：
   - 打开一个 Git 项目
   - 修改一些代码
   - 在 VCS Commit 界面看到 "AI Generate Commit Message" 按钮

### 构建插件包
1. 在 Gradle 面板展开：`subMsg → Tasks → intellij → buildPlugin`
2. 双击 `buildPlugin`
3. 构建完成后，插件包位于：`build/distributions/SubMsg-2.0.0.zip`

### 使用命令行
```bash
# 运行插件
./gradlew runIde

# 构建插件包
./gradlew buildPlugin

# 清理构建
./gradlew clean

# 完整构建
./gradlew build
```

## 📝 配置插件设置

在测试的 IDEA 实例中：
1. 打开 `File → Settings → Tools → SubMsg`
2. 配置 CLI 模式或 API 模式
3. 测试连接

## 🐛 常见问题

### 问题 1: Gradle 同步失败
**解决方案**:
```bash
# 在项目目录运行
./gradlew clean build --refresh-dependencies
```

### 问题 2: 找不到 Gradle 面板
**解决方案**:
- `View → Tool Windows → Gradle`

### 问题 3: Java 版本错误
**检查**:
- 项目需要 Java 11 或更高
- 在 `File → Project Structure → Project` 检查 SDK 版本

### 问题 4: 插件运行时报错
**检查**:
1. 确保完整构建成功：`./gradlew build`
2. 清理并重新构建：`./gradlew clean build`
3. 查看 IDEA 日志：`Help → Show Log in Explorer`

## 📋 项目结构说明

```
D:\subMsg\
├── src/main/
│   ├── kotlin/          # Kotlin 源代码
│   └── resources/       # 资源文件（图标、配置、国际化）
├── build/               # 构建输出（自动生成）
├── .gradle/             # Gradle 缓存（自动生成）
├── .idea/               # IDEA 配置（自动生成）
├── build.gradle.kts     # Gradle 构建脚本
├── settings.gradle.kts  # Gradle 设置
└── gradle.properties    # Gradle 属性配置
```

## 🎯 开发工作流

1. **修改代码** → 保存更改
2. **测试** → 运行 `runIde` 查看效果
3. **调试** → 在代码中设置断点，以 Debug 模式运行 `runIde`
4. **构建** → 运行 `buildPlugin` 生成发布包

## 📦 发布准备

1. 确保所有测试通过
2. 更新版本号：修改 `build.gradle.kts` 中的 `version`
3. 更新 `CHANGELOG.md`
4. 运行 `./gradlew buildPlugin`
5. 上传 `build/distributions/SubMsg-2.0.0.zip` 到 JetBrains Marketplace

## ✅ 验证清单

运行以下命令确保一切正常：

```bash
# 1. 清理旧的构建
./gradlew clean

# 2. 完整构建
./gradlew build

# 3. 运行插件测试
./gradlew runIde
```

所有步骤都应该成功完成！

---

**最后更新**: 2026-09-04
**状态**: ✅ 可以正常使用
