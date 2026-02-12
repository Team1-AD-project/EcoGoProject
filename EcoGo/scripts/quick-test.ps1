# EcoGo Quick Test Script - PowerShell Version

Write-Host "==========================================" -ForegroundColor Green
Write-Host "EcoGo Quick Test" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

# Step 1: Build Application
Write-Host "[1/5] Building application..." -ForegroundColor Cyan
& .\mvnw.cmd clean package -DskipTests
if ($LASTEXITCODE -eq 0) {
    Write-Host "OK: Application built successfully" -ForegroundColor Green
} else {
    Write-Host "ERROR: Application build failed" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 2: Build Docker Image
Write-Host "[2/5] Building Docker image..." -ForegroundColor Cyan
docker build -t ecogo:test .
if ($LASTEXITCODE -eq 0) {
    Write-Host "OK: Docker image built successfully" -ForegroundColor Green
} else {
    Write-Host "ERROR: Docker image build failed" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 3: Start Monitoring Stack
Write-Host "[3/5] Starting monitoring stack..." -ForegroundColor Cyan
Push-Location monitoring
docker-compose up -d
Pop-Location
Write-Host "Waiting for services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 20
Write-Host "OK: Monitoring stack started" -ForegroundColor Green
Write-Host ""

# Step 4: Start Application
Write-Host "[4/5] Starting application..." -ForegroundColor Cyan
docker stop ecogo-test 2>$null
docker rm ecogo-test 2>$null

docker run -d `
  --name ecogo-test `
  --network monitoring_ecogo-monitoring `
  -p 8091:8090 `
  -e SPRING_DATA_MONGODB_URI=mongodb://ecogo-mongodb:27017/EcoGo `
  -e SPRING_PROFILES_ACTIVE=test `
  ecogo:test

Write-Host "Waiting for application to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 25
Write-Host "OK: Application started" -ForegroundColor Green
Write-Host ""

# Step 5: Test Endpoints
Write-Host "[5/5] Testing API endpoints..." -ForegroundColor Cyan
try {
    $health = Invoke-WebRequest -Uri "http://localhost:8091/actuator/health" -UseBasicParsing
    if ($health.Content -like "*UP*") {
        Write-Host "OK: Health check: UP" -ForegroundColor Green
    } else {
        Write-Host "WARN: Health check response: " -NoNewline -ForegroundColor Yellow
        Write-Host $health.Content
    }
} catch {
    Write-Host "WARN: Health check failed to connect" -ForegroundColor Yellow
    Write-Host "Error: $_" -ForegroundColor Yellow
}

try {
    $metrics = Invoke-WebRequest -Uri "http://localhost:8091/actuator/prometheus" -UseBasicParsing
    $lines = ($metrics.Content -split "`n").Count
    if ($lines -gt 10) {
        Write-Host "OK: Prometheus metrics: $lines lines" -ForegroundColor Green
    } else {
        Write-Host "WARN: Prometheus metrics: $lines lines (less than expected)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "WARN: Prometheus metrics failed to connect" -ForegroundColor Yellow
    Write-Host "Error: $_" -ForegroundColor Yellow
}
Write-Host ""

# Summary
Write-Host "==========================================" -ForegroundColor Green
Write-Host "Test Complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

Write-Host "Access URLs:" -ForegroundColor Cyan
Write-Host "  Application Health: http://localhost:8091/actuator/health"
Write-Host "  Prometheus:         http://localhost:9090"
Write-Host "  Grafana:            http://localhost:3000 (admin/admin)"
Write-Host ""

Write-Host "View Logs:" -ForegroundColor Cyan
Write-Host "  docker logs ecogo-test"
Write-Host ""

Write-Host "Cleanup Resources:" -ForegroundColor Cyan
Write-Host "  docker stop ecogo-test"
Write-Host "  docker rm ecogo-test"
Write-Host "  cd monitoring"
Write-Host "  docker-compose down"
Write-Host "  cd .."
Write-Host ""

Write-Host "Check running containers:" -ForegroundColor Cyan
Write-Host "  docker ps"
