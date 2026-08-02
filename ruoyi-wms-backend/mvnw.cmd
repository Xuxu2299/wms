@echo off
set "PROJECT_ROOT=%~dp0"
for %%I in ("%PROJECT_ROOT%..") do set "WORKSPACE_ROOT=%%~fI"

set "JAVA_HOME=%WORKSPACE_ROOT%\runtime\jdk17\jdk-17.0.19+10"
set "MAVEN_HOME=%WORKSPACE_ROOT%\.tools\apache-maven-3.9.16"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%WORKSPACE_ROOT%\.tools\mingit\cmd;%PATH%"

"%MAVEN_HOME%\bin\mvn.cmd" %*
