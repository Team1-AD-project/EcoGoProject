# SonarQube Local Test Script for PowerShell
# This script helps you test SonarQube scan locally with detailed logs

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "SonarQube Local Test Script" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check if SONAR_HOST_URL is set
if (-not $env:SONAR_HOST_URL) {
    Write-Host "ERROR: SONAR_HOST_URL is not set" -ForegroundColor Red
    Write-Host "Please set it using: `$env:SONAR_HOST_URL='https://sonarcloud.io'" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# Check if SONAR_TOKEN is set
if (-not $env:SONAR_TOKEN) {
    Write-Host "ERROR: SONAR_TOKEN is not set" -ForegroundColor Red
    Write-Host "Please set it using: `$env:SONAR_TOKEN='your_token_here'" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Configuration:" -ForegroundColor Green
Write-Host "  SONAR_HOST_URL: $env:SONAR_HOST_URL"
Write-Host "  SONAR_TOKEN: [HIDDEN]"
Write-Host ""
Write-Host "Starting SonarQube scan with detailed logging..." -ForegroundColor Yellow
Write-Host ""

# Check if using SonarCloud and add organization parameter if needed
$sonarArgs = @(
    "clean",
    "verify",
    "sonar:sonar",
    "-X",
    "-e",
    "-Dsonar.projectKey=team1-ad-project",
    "-Dsonar.sources=src/main",
    "-Dsonar.tests=src/test",
    "-Dsonar.host.url=$env:SONAR_HOST_URL",
    "-Dsonar.login=$env:SONAR_TOKEN",
    "-Dsonar.verbose=true"
)

# Add organization if using SonarCloud
if ($env:SONAR_HOST_URL -like "*sonarcloud.io*") {
    Write-Host "Detected SonarCloud - adding organization parameter" -ForegroundColor Cyan
    $sonarArgs += "-Dsonar.organization=team1-ad-project"
}

# Run Maven with arguments
& mvn $sonarArgs

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Scan completed!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to exit"
