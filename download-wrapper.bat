@echo off
echo 正在下载 Gradle Wrapper...
cd /d "%~dp0"
mkdir gradle\wrapper 2>nul

echo 下载 gradle-wrapper.jar...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v7.5.1/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle/wrapper/gradle-wrapper.jar'}"

if exist gradle\wrapper\gradle-wrapper.jar (
    echo gradle-wrapper.jar 下载成功！
    echo 文件大小：
    dir gradle\wrapper\gradle-wrapper.jar | find "gradle-wrapper.jar"
) else (
    echo 下载失败！
)

pause
