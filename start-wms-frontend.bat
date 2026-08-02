@echo off
chcp 65001 >nul
setlocal

set "NODE_HOME=C:\Users\Administrator\AppData\Roaming\TRAE SOLO CN\ModularData\ai-agent\vm\tools\node"
set "PATH=%NODE_HOME%;%PATH%"
cd /d "C:\Users\Administrator\AppData\Roaming\TRAE SOLO CN\ModularData\ai-agent\work-mode-projects\6a68473465e5efa65cc7706e\ruoyi-wms-frontend"
npm run dev
