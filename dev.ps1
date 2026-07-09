# Local dev launcher for bodiala + a provider stub.
# Also starts the PostgreSQL container (docker-compose.yml) that bodiala uses as its datasource.
#
#   .\dev.ps1              # Postgres (:5432) + hotelbeds-stub (:9091) + bodiala 'hotelbeds-stub' profile (:8080)
#                          # -> DEFAULT: full Hotelbeds flow against the fake API; the cache auto-seeds on
#                          #    startup (no manual /import), search returns a multi-destination catalog.
#   .\dev.ps1 -RezLive     # Postgres (:5432) + rezlive-stub (:9090) + bodiala 'stub' profile (:8080)
#                          # -> the (kept) RezLive path: live content/search/booking against the fake RezLive API
#   .\dev.ps1 -StaticOnly  # Postgres (:5432) + ONLY bodiala (default profile) — enough for static-data query
#
# Each app opens in its own console window; close the window (or Ctrl+C) to stop it.
param([switch]$RezLive, [switch]$StaticOnly, [switch]$Hotelbeds)

$ErrorActionPreference = "Stop"

# Ensure the Java 25 toolchain is on JAVA_HOME (the build requires exactly JDK 25).
$jdk25 = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
if (-not $env:JAVA_HOME -and (Test-Path $jdk25)) { $env:JAVA_HOME = $jdk25 }

$bodiala = $PSScriptRoot
$rezliveStub = Join-Path (Split-Path $bodiala -Parent) "rezlive-stub"
$hotelbedsStub = Join-Path (Split-Path $bodiala -Parent) "hotelbeds-stub"

function Start-App($dir, $extraArgs) {
    $runArgs = @("bootRun", "--console=plain") + $extraArgs
    Start-Process -FilePath (Join-Path $dir "gradlew.bat") -ArgumentList $runArgs -WorkingDirectory $dir
}

function Wait-ForHealth($url, $name, $seconds) {
    Write-Host "Waiting for $name ($url) ..." -ForegroundColor DarkGray
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        try { if ((Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 2).StatusCode -eq 200) { return $true } }
        catch { Start-Sleep -Seconds 2 }
    }
    Write-Warning "$name did not become healthy in ${seconds}s; starting bodiala anyway (it will retry / you can POST /import)."
    return $false
}

# bodiala's datasource is PostgreSQL running in Docker (docker-compose.yml). Bring it up and
# wait for the healthcheck before starting the app, otherwise the first boot fails to connect.
Write-Host "Starting PostgreSQL (docker compose up -d --wait) -> localhost:5432" -ForegroundColor Cyan
Push-Location $bodiala
try {
    docker compose up -d --wait
    if ($LASTEXITCODE -ne 0) { throw "docker compose up failed — is Docker Desktop running?" }
}
finally { Pop-Location }

if ($RezLive) {
    if (Test-Path (Join-Path $rezliveStub "gradlew.bat")) {
        Write-Host "Starting RezLive stub -> http://localhost:9090" -ForegroundColor Cyan
        Start-App $rezliveStub @()
        Start-Sleep -Seconds 2
    }
    else {
        Write-Warning "rezlive-stub not found next to bodiala; live API calls will return 503 without it."
    }
    Write-Host "Starting bodiala (profile: stub) -> http://localhost:8080" -ForegroundColor Cyan
    Start-App $bodiala @("--args=--spring.profiles.active=stub")
}
elseif ($StaticOnly) {
    Write-Host "Starting bodiala (default profile, static-data only) -> http://localhost:8080" -ForegroundColor Cyan
    Start-App $bodiala @()
}
else {
    # DEFAULT: Hotelbeds against the stub, cache auto-seeded on startup.
    if (Test-Path (Join-Path $hotelbedsStub "gradlew.bat")) {
        Write-Host "Starting Hotelbeds stub -> http://localhost:9091" -ForegroundColor Cyan
        Start-App $hotelbedsStub @()
        # Wait for the stub so bodiala's startup auto-import can seed the cache from it.
        Wait-ForHealth "http://localhost:9091/health" "hotelbeds-stub" 90 | Out-Null
    }
    else {
        Write-Warning "hotelbeds-stub not found next to bodiala; Hotelbeds calls will return 503 and the cache won't seed."
    }
    Write-Host "Starting bodiala (profile: hotelbeds-stub) -> http://localhost:8080" -ForegroundColor Cyan
    Start-App $bodiala @("--args=--spring.profiles.active=hotelbeds-stub")
}

Write-Host ""
Write-Host "Swagger UI:   http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Write-Host "Give the apps ~5-10s to start, then open Swagger (or the frontend on :5173)." -ForegroundColor Yellow
