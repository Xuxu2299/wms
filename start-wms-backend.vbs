Set WshShell = CreateObject("WScript.Shell")

' WMS 后端后台启动脚本
' 无窗口模式运行，开机自动启动

javaHome = "C:\Users\Administrator\AppData\Roaming\TRAE SOLO CN\ModularData\ai-agent\work-mode-projects\6a68473465e5efa65cc7706e\runtime\jdk17\jdk-17.0.19+10"
jarPath = "C:\Users\Administrator\AppData\Roaming\TRAE SOLO CN\ModularData\ai-agent\work-mode-projects\6a68473465e5efa65cc7706e\ruoyi-wms-backend\ruoyi-admin-wms\target\ruoyi-admin-wms.jar"
workDir = "C:\Users\Administrator\AppData\Roaming\TRAE SOLO CN\ModularData\ai-agent\work-mode-projects\6a68473465e5efa65cc7706e\ruoyi-wms-backend"

' 构建命令
cmd = """" & javaHome & "\bin\java.exe"" -jar """ & jarPath & """ --spring.profiles.active=dev"

' 使用 0 窗口模式运行（隐藏控制台），不等待完成
WshShell.CurrentDirectory = workDir
WshShell.Run cmd, 0, False

Set WshShell = Nothing
