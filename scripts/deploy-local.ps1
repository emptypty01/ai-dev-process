param([string]$Version = 'workspace-20261024')
$ErrorActionPreference = 'Stop'
if ($Version -notmatch '^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,100}$') { throw 'Invalid image version' }
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    if (-not (Test-Path -LiteralPath 'backend/target/order-demo-0.1.0-SNAPSHOT.jar')) { throw 'Build backend first' }
    if (-not (Test-Path -LiteralPath 'frontend/dist/index.html')) { throw 'Build frontend first' }
    docker build -f backend/Dockerfile -t "local/ai-order-backend:$Version" .
    if ($LASTEXITCODE -ne 0) { throw 'Backend image build failed' }
    docker build -f frontend/Dockerfile -t "local/ai-order-frontend:$Version" .
    if ($LASTEXITCODE -ne 0) { throw 'Frontend image build failed' }
    $env:APP_VERSION = $Version
    $passwordLine = Get-Content -LiteralPath 'D:/docker/ai-dev-platform/.env' | Where-Object { $_.StartsWith('MYSQL_PASSWORD=') }
    $env:DB_PASSWORD = $passwordLine.Substring('MYSQL_PASSWORD='.Length)
    docker compose -f deploy/compose.yaml up -d
    if ($LASTEXITCODE -ne 0) { throw 'Local deployment failed' }
    Write-Output "Deployed $Version ; http://localhost:5174"
} finally {
    Remove-Item Env:APP_VERSION,Env:DB_PASSWORD -ErrorAction SilentlyContinue
    Pop-Location
}
