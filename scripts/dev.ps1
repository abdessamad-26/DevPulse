# PowerShell script for Windows dev environment
param()
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"

# First run: create .env from the template with a fresh, cryptographically secure JWT secret.
if (-not (Test-Path $envFile)) {
    Copy-Item (Join-Path $root ".env.example") $envFile
    $bytes = New-Object byte[] 48
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $secret = [Convert]::ToBase64String($bytes)
    $lines = Get-Content $envFile | ForEach-Object {
        if ($_ -match '^JWT_SECRET=') { "JWT_SECRET=$secret" }
        elseif ($_ -match '^DEMO_MODE=') { "DEMO_MODE=true" }   # local machine only: enables the demo account
        else { $_ }
    }
    Set-Content -Path $envFile -Value $lines
    Write-Host ".env created with a new random JWT_SECRET (demo mode enabled for local use). It is git-ignored: never commit it."
}

Write-Host "Starting DevPulse local environment with Docker Compose..."
Push-Location $root
docker compose up -d --build
Start-Sleep -Seconds 10
Write-Host "To run backend locally without containers: cd backend; set JWT_SECRET (see .env), then mvn -DskipTests spring-boot:run"
Pop-Location
