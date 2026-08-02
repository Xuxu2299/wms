$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$WorkspaceRoot = Split-Path -Parent $ProjectRoot

$JdkHome = Join-Path $WorkspaceRoot 'runtime\jdk17\jdk-17.0.19+10'
$MavenBin = Join-Path $WorkspaceRoot '.tools\apache-maven-3.9.16\bin'
$GitCmd = Join-Path $WorkspaceRoot '.tools\mingit\cmd'

if (!(Test-Path (Join-Path $JdkHome 'bin\javac.exe'))) {
    throw ('JDK not found: ' + $JdkHome)
}
if (!(Test-Path (Join-Path $MavenBin 'mvn.cmd'))) {
    throw ('Maven not found: ' + $MavenBin)
}
if (!(Test-Path (Join-Path $GitCmd 'git.exe'))) {
    throw ('Git not found: ' + $GitCmd)
}

$env:JAVA_HOME = $JdkHome
$env:Path = "$JdkHome\bin;$MavenBin;$GitCmd;$env:Path"

Write-Host 'Local Java/Maven/Git environment activated.'
Write-Host ('JAVA_HOME=' + $env:JAVA_HOME)
