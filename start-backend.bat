@echo off
setlocal
cd /d "%~dp0ruoyi-wms-backend"
where javac >nul 2>nul
if errorlevel 1 (
  echo 未检测到 JDK 编译器 javac。请先安装 JDK 17 并配置 JAVA_HOME/PATH。
  pause
  exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 (
  echo 未检测到 Maven。请先安装 Maven 3.8+ 并配置 PATH。
  pause
  exit /b 1
)
mvn -DskipTests package
if errorlevel 1 (
  echo 后端打包失败，请检查上方 Maven 日志。
  pause
  exit /b 1
)
java -jar ruoyi-admin-wms\target\ruoyi-admin-wms.jar
