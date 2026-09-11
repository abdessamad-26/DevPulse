# PowerShell script for Windows dev environment
param()
$root = Split-Path -Parent $PSScriptRoot
Write-Host "Starting DevPulse local environment with Docker Compose..."
Push-Location $root
docker compose up -d --build
Start-Sleep -Seconds 10
Write-Host "To run backend locally without containers: cd backend; mvn -DskipTests spring-boot:run"
Pop-Location
