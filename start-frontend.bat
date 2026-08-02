@echo off
setlocal
cd /d "%~dp0ruoyi-wms-frontend"
if not exist node_modules (
  npm install --registry=https://registry.npmmirror.com
)
npm run dev
