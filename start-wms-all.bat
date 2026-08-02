@echo off
chcp 65001 >nul
setlocal
title WMS 一键启动

set "BASE=C:\Users\Administrator\AppData\Roaming\TRAE SOLO CN\ModularData\ai-agent\work-mode-projects\6a68473465e5efa65cc7706e"
set "JAVA=%BASE%\runtime\jdk17\jdk-17.0.19+10\bin\java.exe"
set "MYSQLD=%BASE%\runtime\mariadb\mariadb-10.11.11-winx64\bin\mysqld.exe"
set "MYSQL_INI=%BASE%\runtime\mariadb-data\my.ini"
set "REDIS=%BASE%\runtime\redis\redis-server.exe"
set "JAR=%BASE%\ruoyi-wms-backend\ruoyi-admin-wms\target\ruoyi-admin-wms.jar"
set "BACKEND_DIR=%BASE%\ruoyi-wms-backend\ruoyi-admin-wms"
set "FRONTEND_DIR=%BASE%\ruoyi-wms-frontend"

echo ========================================
echo   RuoYi-WMS 一键启动
echo ========================================
echo.

if not exist "%JAVA%" (
  echo [错误] 找不到 JDK: %JAVA%
  pause
  exit /b 1
)
if not exist "%MYSQLD%" (
  echo [错误] 找不到 MariaDB: %MYSQLD%
  pause
  exit /b 1
)
if not exist "%MYSQL_INI%" (
  echo [错误] 找不到 MariaDB 配置: %MYSQL_INI%
  pause
  exit /b 1
)
if not exist "%REDIS%" (
  echo [错误] 找不到 Redis: %REDIS%
  pause
  exit /b 1
)
if not exist "%JAR%" (
  echo [错误] 找不到后端 jar: %JAR%
  pause
  exit /b 1
)

echo [1/4] 检查 MariaDB (3306)...
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if %errorlevel% == 0 (
  echo   MariaDB 已运行
) else (
  echo   正在启动 MariaDB...
  start "WMS MariaDB" /D "%BASE%\runtime\mariadb\mariadb-10.11.11-winx64\bin" "%MYSQLD%" --defaults-file="%MYSQL_INI%" --console
  timeout /t 6 /nobreak >nul
)
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if not %errorlevel% == 0 (
  echo [错误] MariaDB 启动失败，请查看弹出的 MariaDB 窗口日志。
  pause
  exit /b 1
)

echo.
echo [2/4] 检查 Redis (6379)...
netstat -ano | findstr ":6379" | findstr "LISTENING" >nul
if %errorlevel% == 0 (
  echo   Redis 已运行
) else (
  echo   正在启动 Redis...
  start "WMS Redis" /D "%BASE%\runtime\redis" "%REDIS%"
  timeout /t 3 /nobreak >nul
)
netstat -ano | findstr ":6379" | findstr "LISTENING" >nul
if not %errorlevel% == 0 (
  echo [错误] Redis 启动失败，请查看弹出的 Redis 窗口日志。
  pause
  exit /b 1
)

echo.
echo [3/4] 检查 WMS 后端 (8080)...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if %errorlevel% == 0 (
  echo   后端已运行
) else (
  echo   正在启动后端...
  start "WMS Backend" /D "%BACKEND_DIR%" "%JAVA%" -jar "%JAR%" --spring.profiles.active=dev
  timeout /t 12 /nobreak >nul
)
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if not %errorlevel% == 0 (
  echo [错误] 后端启动失败，请查看弹出的 WMS Backend 窗口日志。
  pause
  exit /b 1
)

echo.
echo [4/4] 检查 WMS 前端 (80)...
netstat -ano | findstr ":80" | findstr "LISTENING" >nul
if %errorlevel% == 0 (
  echo   前端已运行
) else (
  echo   正在启动前端...
  start "WMS Frontend" /D "%FRONTEND_DIR%" cmd /k "npm run dev"
  timeout /t 8 /nobreak >nul
)

echo.
echo ========================================
echo WMS 已启动
echo 前端地址: http://localhost
echo 后端地址: http://localhost:8080
echo 接口文档: http://localhost:8080/swagger-ui/index.html
echo ========================================
echo.
start "" "http://localhost"
pause
