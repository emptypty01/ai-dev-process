param(
    [switch]$Build,
    [string]$LogDirectory = 'D:/plan&exec&test/20261024-AI研发流程本地平台/log'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$platformRoot = 'D:/docker/ai-dev-platform'
$processLog = [System.IO.Path]::GetFullPath($LogDirectory)
New-Item -ItemType Directory -Path $processLog -Force | Out-Null
$localDir = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Path $localDir -Force | Out-Null
function Get-EnvValue([string]$Name) {
    $line = Get-Content -LiteralPath "$platformRoot/.env" | Where-Object { $_.StartsWith("$Name=") } | Select-Object -First 1
    if (-not $line) { throw "Missing setting: $Name" }
    return $line.Substring($Name.Length + 1)
}
if ($Build) {
    Push-Location "$projectRoot/backend"
    try { mvn -B -ntp -DskipTests package; if ($LASTEXITCODE -ne 0) { throw 'Backend build failed' } } finally { Pop-Location }
}
$jarFile = Join-Path $projectRoot 'backend/target/order-demo-0.1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jarFile)) { throw 'Run this script with -Build first' }
$backendConnection = Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue
if (-not $backendConnection) {
    $env:DB_PASSWORD = Get-EnvValue 'MYSQL_PASSWORD'
    try {
        $javaSettings = & java -XshowSettings:properties -version 2>&1
        $javaHomeLine = ($javaSettings | Select-String 'java.home =').Line
        if (-not $javaHomeLine) { throw 'Cannot resolve Java runtime path' }
        $resolvedJavaHome = $javaHomeLine.Split('=', 2)[1].Trim()
        $javaCommand = Join-Path $resolvedJavaHome 'bin/java.exe'
        $runtimeJar = Join-Path $localDir 'order-demo-runtime.jar'
        Copy-Item -LiteralPath $jarFile -Destination $runtimeJar -Force
        $backendProcess = Start-Process -FilePath $javaCommand -ArgumentList @('-Xms128m', '-Xmx384m', '-jar', $runtimeJar) -WorkingDirectory "$projectRoot/backend" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$processLog/demo-backend.out.log" -RedirectStandardError "$processLog/demo-backend.err.log"
        $backendProcess.Id | Set-Content -LiteralPath "$localDir/backend.pid"
    } finally { Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue }
} else { Write-Output 'Port 8081 already in use; backend start skipped.' }
$frontendConnection = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
if (-not $frontendConnection) {
    $nodeCommand = (Get-Command node).Source
    $viteFile = Join-Path $projectRoot 'frontend/node_modules/vite/bin/vite.js'
    $frontendProcess = Start-Process -FilePath $nodeCommand -ArgumentList @($viteFile, '--host', '127.0.0.1', '--port', '5173', '--strictPort') -WorkingDirectory "$projectRoot/frontend" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$processLog/demo-frontend.out.log" -RedirectStandardError "$processLog/demo-frontend.err.log"
    $frontendProcess.Id | Set-Content -LiteralPath "$localDir/frontend.pid"
} else { Write-Output 'Port 5173 already in use; frontend start skipped.' }
Write-Output 'Backend http://localhost:8081/api/health ; frontend http://localhost:5173'
