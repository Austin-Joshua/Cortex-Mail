# Starts the full stack on Windows: Spring Boot backend + Vite frontend.
#
#   .\run-dev.ps1              start both, Ctrl-C stops both
#   .\run-dev.ps1 -Bypass      also open a demo session without Google OAuth
#
# PowerShell equivalent of run-dev.sh, which needs bash (WSL or Git Bash).

param([switch]$Bypass)

$ErrorActionPreference = 'Stop'
$Root     = $PSScriptRoot
$Backend  = Join-Path $Root 'nexora\backend'
$Frontend = Join-Path $Root 'nexora\frontend'
$Logs     = Join-Path $Root '.dev-logs'

function Ok   ($m) { Write-Host "  ok    $m" -ForegroundColor Green }
function Info ($m) { Write-Host "  ..    $m" -ForegroundColor Cyan }
function Warn ($m) { Write-Host "  warn  $m" -ForegroundColor Yellow }
function Die  ($m) { Write-Host "  fail  $m" -ForegroundColor Red; exit 1 }

Write-Host ''
Write-Host 'Velocity - full stack' -ForegroundColor White
Write-Host ''

# ── prerequisites ────────────────────────────────────────────────────────────
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Die 'java not found (need JDK 17+)' }
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { Die 'node not found (need Node 18+)' }

# The JVM prints "Picked up JAVA_TOOL_OPTIONS: ..." before the version line when
# that variable is set, so match the version line rather than taking the first.
$javaOut  = (& java -version 2>&1 | Out-String)
$verLine  = ($javaOut -split "`n" | Where-Object { $_ -match '(java|openjdk) version' } | Select-Object -First 1)
if ($verLine -match '"(?:1\.)?(\d+)') { $javaMajor = [int]$Matches[1] } else { Die "could not read Java version from: $javaOut" }
if ($javaMajor -lt 17) { Die "Java $javaMajor found, need 17+" }
Ok "java $javaMajor, node $(node -v)"

# ── backend env ──────────────────────────────────────────────────────────────
$EnvFile = Join-Path $Backend '.env'
if (-not (Test-Path $EnvFile)) {
    Copy-Item (Join-Path $Backend '.env.example') $EnvFile
    Warn 'created nexora\backend\.env from the example - add your real keys to it'
}

# The backend reads .env itself on startup, so values need not be exported here.
# Read it only to warn about anything still unset.
$envMap = @{}
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
        $i = $line.IndexOf('=')
        $envMap[$line.Substring(0, $i).Trim()] = $line.Substring($i + 1).Trim().Trim('"', "'")
    }
}

function Unset ($k) { -not $envMap[$k] -or $envMap[$k] -like 'your*' }

if (Unset 'JWT_SECRET')     { Die 'JWT_SECRET is unset in nexora\backend\.env (needs 32+ characters)' }
if (Unset 'ENCRYPTION_KEY') { Die 'ENCRYPTION_KEY is unset in nexora\backend\.env (needs exactly 16 characters)' }
if (Unset 'GOOGLE_CLIENT_ID') {
    Warn "GOOGLE_CLIENT_ID unset - 'Sign in with Google' will return 503"
    if (-not $Bypass) { Warn 'run .\run-dev.ps1 -Bypass to explore without OAuth' }
}
if (Unset 'GEMINI_API_KEY') { Warn 'GEMINI_API_KEY unset - AI falls back to keyword matching' }
else { Ok 'gemini key present' }

if ($Bypass) { $env:DEV_BYPASS_ENABLED = 'true' }

$port = if ($envMap['PORT']) { $envMap['PORT'] } else { '8080' }
New-Item -ItemType Directory -Force -Path $Logs | Out-Null

$procs = @()
function Stop-All {
    Write-Host ''
    Info 'shutting down'
    foreach ($p in $script:procs) {
        if ($p -and -not $p.HasExited) {
            # mvnw forks a child JVM; kill the whole tree or the port stays bound.
            & taskkill /PID $p.Id /T /F 2>&1 | Out-Null
        }
    }
    Ok 'stopped'
}

try {
    # ── backend ──────────────────────────────────────────────────────────────
    Info "starting backend on :$port"
    $mvnw = Join-Path $Backend 'mvnw.cmd'
    $procs += Start-Process -FilePath $mvnw -ArgumentList '-q','spring-boot:run' `
        -WorkingDirectory $Backend -PassThru -NoNewWindow `
        -RedirectStandardOutput (Join-Path $Logs 'backend.log') `
        -RedirectStandardError  (Join-Path $Logs 'backend.err.log')

    $up = $false
    foreach ($i in 1..90) {
        try {
            Invoke-WebRequest "http://localhost:$port/actuator/health" -UseBasicParsing -TimeoutSec 2 | Out-Null
            $up = $true; break
        } catch { Start-Sleep -Seconds 1 }
    }
    if (-not $up) {
        Get-Content (Join-Path $Logs 'backend.log') -Tail 30 -ErrorAction SilentlyContinue
        Die "backend did not become healthy - see $Logs\backend.log"
    }
    Ok "backend healthy at http://localhost:$port"

    # ── frontend ─────────────────────────────────────────────────────────────
    $feEnv = Join-Path $Frontend '.env'
    if (-not (Test-Path $feEnv)) { Copy-Item (Join-Path $Frontend '.env.example') $feEnv }
    if (-not (Test-Path (Join-Path $Frontend 'node_modules'))) {
        Info 'installing frontend dependencies (first run, takes a minute)'
        Push-Location $Frontend
        & npm install --silent
        Pop-Location
        if ($LASTEXITCODE -ne 0) { Die 'npm install failed' }
    }

    Info 'starting frontend on :5173'
    $procs += Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','npm run dev' `
        -WorkingDirectory $Frontend -PassThru -NoNewWindow `
        -RedirectStandardOutput (Join-Path $Logs 'frontend.log') `
        -RedirectStandardError  (Join-Path $Logs 'frontend.err.log')

    $up = $false
    foreach ($i in 1..60) {
        try { Invoke-WebRequest 'http://localhost:5173/' -UseBasicParsing -TimeoutSec 2 | Out-Null; $up = $true; break }
        catch { Start-Sleep -Seconds 1 }
    }
    if (-not $up) {
        Get-Content (Join-Path $Logs 'frontend.log') -Tail 30 -ErrorAction SilentlyContinue
        Die "frontend did not start - see $Logs\frontend.log"
    }
    Ok 'frontend ready'

    Write-Host ''
    Write-Host '  open  http://localhost:5173' -ForegroundColor White
    if ($Bypass) {
        Write-Host ''
        Write-Host '  demo session (no Google needed):' -ForegroundColor Yellow
        Write-Host "        http://localhost:$port/api/auth/bypass"
        Write-Host '        Its 5 emails carry PRE-WRITTEN summaries, not AI output.'
    }
    Write-Host ''
    Write-Host "  logs   $Logs"
    Write-Host ''
    Write-Host '  Ctrl-C to stop both' -ForegroundColor Cyan
    Write-Host ''

    while ($true) {
        Start-Sleep -Seconds 1
        foreach ($p in $procs) { if ($p.HasExited) { Warn 'a service exited'; return } }
    }
}
finally { Stop-All }
