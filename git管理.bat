@echo off
chcp 65001 >nul 2>&1
title WMS Git 版本管理工具

:: Git 路径
set GITEXE=%~dp0.tools\mingit\cmd\git.exe
set ROOT=%~dp0

:menu
cls
echo.
echo  ================================================
echo          WMS Git 版本管理工具
echo  ================================================
echo.
echo   [1] 查看历史记录 (我改过哪些版本)
echo   [2] 创建备份点   (改代码前用这个)
echo   [3] 查看当前状态 (有没有未保存的改动)
echo   [4] 回滚到上一版本 (改坏了用这个)
echo   [5] 回滚到指定版本 (输入编号回滚)
echo   [6] 查看改了什么 (跟上次比，哪里变了)
echo   [0] 退出
echo.
echo  ================================================
set /p choice=请输入数字选择:

if "%choice%"=="1" goto showlog
if "%choice%"=="2" goto backup
if "%choice%"=="3" goto status
if "%choice%"=="4" goto rollback
if "%choice%"=="5" goto rollbackspecific
if "%choice%"=="6" goto diff
if "%choice%"=="0" exit
goto menu

:showlog
cls
echo.
echo  ========== 版本历史记录 ==========
echo.
"%GITEXE%" -C "%ROOT%" log --oneline --graph -20
echo.
echo  (只显示最近20条)
echo.
pause
goto menu

:backup
cls
echo.
echo  ========== 创建备份点 ==========
echo.
set /p msg=请输入本次备份说明 (比如: 修改入库逻辑前):
echo.
"%GITEXE%" -C "%ROOT%" add -A
"%GITEXE%" -C "%ROOT%" commit -m "%msg%"
echo.
echo  ★ 备份点已创建！
echo.
pause
goto menu

:status
cls
echo.
echo  ========== 当前状态 ==========
echo.
"%GITEXE%" -C "%ROOT%" status
echo.
pause
goto menu

:rollback
cls
echo.
echo  ========== 回滚到上一版本 ==========
echo.
echo  警告: 这会丢弃所有未保存的改动，恢复到上一个提交点！
echo.
set /p confirm=确定要回滚吗？输入 Y 确认:
if /i not "%confirm%"=="Y" goto menu
echo.
"%GITEXE%" -C "%ROOT%" reset --hard HEAD~1
echo.
echo  ★ 已回滚到上一版本！
echo.
pause
goto menu

:rollbackspecific
cls
echo.
echo  ========== 回滚到指定版本 ==========
echo.
echo  最近的提交记录:
echo.
"%GITEXE%" -C "%ROOT%" log --oneline -10
echo.
set /p hash=请输入要回滚到的版本号 (前7位即可):
echo.
echo  警告: 这会丢弃该版本之后的所有改动！
set /p confirm=确定要回滚吗？输入 Y 确认:
if /i not "%confirm%"=="Y" goto menu
echo.
"%GITEXE%" -C "%ROOT%" reset --hard %hash%
echo.
echo  ★ 已回滚到版本 %hash%！
echo.
pause
goto menu

:diff
cls
echo.
echo  ========== 改动详情 ==========
echo.
"%GITEXE%" -C "%ROOT%" diff --stat
echo.
echo  (只显示文件级别的改动统计)
echo  如需看具体代码改动，请使用 AI 工具查看)
echo.
pause
goto menu
