$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
foreach ($component in 'backend', 'frontend') {
    $pidFile = Join-Path $projectRoot ".local/$component.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) { continue }
    $savedProcessId = [int](Get-Content -LiteralPath $pidFile)
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $savedProcessId" -ErrorAction SilentlyContinue
    if ($processInfo -and $processInfo.CommandLine -like '*ai-dev-process*') {
        Stop-Process -Id $savedProcessId
        Write-Output "Stopped $component ($savedProcessId)"
    } elseif ($processInfo) { throw "PID $savedProcessId no longer belongs to this demo; not stopped" }
    Remove-Item -LiteralPath $pidFile
}
# 兼容首次通过 mise shim 启动时产生的子进程；只匹配本示例绝对路径与明确入口。
$workspacePath = [System.IO.Path]::GetFullPath($projectRoot)
$demoProcesses = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -in 'java.exe', 'node.exe' -and $_.CommandLine -like "*$workspacePath*" -and
    ($_.CommandLine -like '*order-demo-0.1.0-SNAPSHOT.jar*' -or $_.CommandLine -like '*order-demo-runtime.jar*' -or $_.CommandLine -like '*frontend*node_modules*vite*')
}
foreach ($demoProcess in $demoProcesses) { Stop-Process -Id $demoProcess.ProcessId; Write-Output "Stopped demo child ($($demoProcess.ProcessId))" }
