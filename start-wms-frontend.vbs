Set WshShell = CreateObject("WScript.Shell")

' WMS 前端后台启动脚本
' 无窗口模式运行，开机自动启动

psPath = "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe"
psScript = "C:\Users\Administrator\AppData\Roaming\TRAE SOLO CN\ModularData\ai-agent\work-mode-projects\6a68473465e5efa65cc7706e\start-wms-frontend.ps1"

' 使用 -WindowStyle Hidden 隐藏 PowerShell 窗口，不等待完成
cmd = """" & psPath & """ -ExecutionPolicy Bypass -WindowStyle Hidden -File """ & psScript & """"

WshShell.Run cmd, 0, False

Set WshShell = Nothing
