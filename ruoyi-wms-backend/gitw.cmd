@echo off
set "PROJECT_ROOT=%~dp0"
for %%I in ("%PROJECT_ROOT%..") do set "WORKSPACE_ROOT=%%~fI"

set "PATH=%WORKSPACE_ROOT%\.tools\mingit\cmd;%PATH%"

"%WORKSPACE_ROOT%\.tools\mingit\cmd\git.exe" %*
