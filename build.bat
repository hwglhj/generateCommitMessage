@echo off
echo ========================================
echo SubMsg 自动构建脚本
echo ========================================
echo.
echo 检测到系统 Java 版本为 Java 8
echo 但项目需要 Java 11 或更高版本
echo.
echo 请选择以下方案之一：
echo.
echo [1] 自动下载 Java 11 并构建 (推荐)
echo [2] 手动指定 Java 11 路径
echo [3] 使用 IDEA 构建 (最简单)
echo [4] 退出
echo.
set /p choice="请输入选项 (1-4): "

if "%choice%"=="1" goto download_java
if "%choice%"=="2" goto manual_java
if "%choice%"=="3" goto idea_build
if "%choice%"=="4" goto end

:download_java
echo.
echo [步骤 1] 正在下载 OpenJDK 11...
echo.
echo 请手动下载 Java 11:
echo 1. 访问: https://adoptium.net/temurin/releases/?version=11
echo 2. 选择: Windows x64 ZIP 包
echo 3. 下载后解压到: C:\Java\jdk-11
echo 4. 再次运行本脚本选择选项 2
echo.
pause
goto end

:manual_java
echo.
set /p java_path="请输入 Java 11 安装路径 (例如: C:\Java\jdk-11): "
if not exist "%java_path%\bin\java.exe" (
    echo 错误: 找不到 Java 可执行文件
    pause
    goto end
)

echo.
echo 使用 Java: %java_path%
echo.
set JAVA_HOME=%java_path%
set PATH=%java_path%\bin;%PATH%

echo [步骤 2] 开始构建插件...
echo.
call gradlew.bat clean buildPlugin

if errorlevel 1 (
    echo.
    echo 构建失败！请检查错误信息。
    pause
    goto end
)

echo.
echo ========================================
echo 构建成功！
echo ========================================
echo.
echo 插件位置: build\distributions\SubMsg-2.0.0.zip
echo.
echo 下一步：
echo 1. 打开 IntelliJ IDEA
echo 2. Settings -^> Plugins -^> Install from Disk
echo 3. 选择上述 zip 文件
echo 4. 重启 IDEA
echo.
pause
goto end

:idea_build
echo.
echo ========================================
echo 使用 IDEA 构建（最简单的方法）
echo ========================================
echo.
echo 步骤：
echo 1. 用 IntelliJ IDEA 打开项目: D:\subMsg
echo 2. 打开右侧 Gradle 工具窗口
echo 3. 双击: SubMsg -^> Tasks -^> intellij -^> buildPlugin
echo 4. 等待构建完成
echo 5. 文件位置: build\distributions\SubMsg-2.0.0.zip
echo.
echo 详细说明请查看: IDEA_BUILD_STEPS.md
echo.
pause
goto end

:end
echo.
echo 感谢使用 SubMsg！
echo.
