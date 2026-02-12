# ============================================================
# EcoGo Recommendation API Test Script
# Tests POST /api/v1/recommendations with various destinations
# ============================================================
# Usage: .\test-recommendations.ps1
#        .\test-recommendations.ps1 -BaseUrl "http://10.0.2.2:8090"
# ============================================================

param(
    [string]$BaseUrl = "http://localhost:8090"
)

$Endpoint = "$BaseUrl/api/v1/recommendations"
$PassCount = 0
$FailCount = 0
$TotalTests = 0

function Test-Recommendation {
    param(
        [string]$Destination,
        [string]$ExpectedTag,
        [string[]]$ExpectedKeywords,
        [string]$Description
    )

    $script:TotalTests++
    $body = @{ destination = $Destination } | ConvertTo-Json

    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "TEST $script:TotalTests: $Description" -ForegroundColor Cyan
    Write-Host "  Input: '$Destination'" -ForegroundColor Gray
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

    try {
        $response = Invoke-RestMethod -Uri $Endpoint -Method POST -ContentType "application/json" -Body $body -TimeoutSec 10

        if ($response.code -ne 200) {
            Write-Host "  FAIL: API returned code $($response.code)" -ForegroundColor Red
            $script:FailCount++
            return
        }

        $text = $response.data.text
        $tag  = $response.data.tag

        Write-Host "  Tag:  $tag" -ForegroundColor Yellow
        Write-Host "  Text: " -ForegroundColor Yellow -NoNewline
        # Print text with line breaks preserved
        $text -split "`n" | ForEach-Object {
            Write-Host "        $_" -ForegroundColor White
        }

        $passed = $true

        # Check tag
        if ($ExpectedTag -and $tag -ne $ExpectedTag) {
            # Tag might vary for walk/bus random, so just warn
            Write-Host "  [WARN] Expected tag '$ExpectedTag', got '$tag'" -ForegroundColor Yellow
        }

        # Check keywords
        foreach ($keyword in $ExpectedKeywords) {
            if ($text -notmatch [regex]::Escape($keyword)) {
                Write-Host "  [FAIL] Missing keyword: '$keyword'" -ForegroundColor Red
                $passed = $false
            }
        }

        # Check not empty
        if ([string]::IsNullOrWhiteSpace($text)) {
            Write-Host "  [FAIL] Response text is empty!" -ForegroundColor Red
            $passed = $false
        }

        if ($passed) {
            Write-Host "  PASSED" -ForegroundColor Green
            $script:PassCount++
        } else {
            Write-Host "  FAILED" -ForegroundColor Red
            $script:FailCount++
        }
    }
    catch {
        Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
        $script:FailCount++
    }
}

# ============================================================
# Run Tests
# ============================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  EcoGo Recommendation API Tests" -ForegroundColor Magenta
Write-Host "  Endpoint: $Endpoint" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

# --- Library destinations ---
Test-Recommendation -Destination "Science Library" `
    -ExpectedTag "Campus-Bus" `
    -ExpectedKeywords @("Science Library (S16)", "Bus Route", "Walk Route", "green points") `
    -Description "Science Library - should show bus + walk routes with full name"

Test-Recommendation -Destination "CLB" `
    -ExpectedTag "Walk" `
    -ExpectedKeywords @("Central Library", "Walk Route", "green points") `
    -Description "CLB (Central Library) - close, should suggest walking"

# --- Faculty destinations ---
Test-Recommendation -Destination "SoC" `
    -ExpectedTag "Campus-Bus" `
    -ExpectedKeywords @("Computing", "Bus Route", "Walk Route", "CO") `
    -Description "School of Computing - should show D1/D2 routes"

Test-Recommendation -Destination "Engineering" `
    -ExpectedTag "Campus-Bus" `
    -ExpectedKeywords @("Engineering", "Bus Route", "Walk Route") `
    -Description "Faculty of Engineering"

Test-Recommendation -Destination "FASS" `
    -ExpectedTag "Campus-Bus" `
    -ExpectedKeywords @("FASS", "Bus Route", "Walk Route") `
    -Description "FASS - Faculty of Arts"

Test-Recommendation -Destination "Business" `
    -ExpectedTag "Campus-Bus" `
    -ExpectedKeywords @("Business", "BIZ2", "Bus Route") `
    -Description "Business School"

# --- Residential ---
Test-Recommendation -Destination "UTown" `
    -ExpectedTag "Campus-Bus" `
    -ExpectedKeywords @("UTown", "Bus Route", "Walk Route") `
    -Description "UTown - multiple bus options"

Test-Recommendation -Destination "Raffles Hall" `
    -ExpectedTag "Campus-Bus" `
    -ExpectedKeywords @("Raffles Hall", "D1", "Walk Route") `
    -Description "Raffles Hall"

# --- Food ---
Test-Recommendation -Destination "The Deck" `
    -ExpectedTag "Walk" `
    -ExpectedKeywords @("Deck", "Walk Route", "green points") `
    -Description "The Deck canteen - close, walking recommended"

Test-Recommendation -Destination "Frontier" `
    -ExpectedTag "Campus-Bus" `
    -ExpectedKeywords @("Frontier", "Bus Route", "Walk Route") `
    -Description "Frontier canteen"

# --- Sports ---
Test-Recommendation -Destination "Gym" `
    -ExpectedTag "Campus-Bus" `
    -ExpectedKeywords @("USC", "Bus Route", "Walk Route", "matric") `
    -Description "Gym / Sports Centre (USC)"

# --- Transit ---
Test-Recommendation -Destination "Kent Ridge MRT" `
    -ExpectedTag "Campus-Bus" `
    -ExpectedKeywords @("Kent Ridge MRT", "Bus Route", "Walk Route") `
    -Description "Kent Ridge MRT station"

# --- Off-campus ---
Test-Recommendation -Destination "Law" `
    -ExpectedTag "Green-Transit" `
    -ExpectedKeywords @("off the main campus", "MRT") `
    -Description "Faculty of Law (Bukit Timah, off-campus)"

# --- Empty input ---
Test-Recommendation -Destination "" `
    -ExpectedTag "General" `
    -ExpectedKeywords @() `
    -Description "Empty input - should return general tips"

# --- Unknown destination ---
Test-Recommendation -Destination "Changi Airport" `
    -ExpectedTag "Eco-Tip" `
    -ExpectedKeywords @("Changi Airport") `
    -Description "Unknown destination - generic recommendation"

# ============================================================
# Summary
# ============================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  TEST SUMMARY" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  Total:  $TotalTests" -ForegroundColor White
Write-Host "  Passed: $PassCount" -ForegroundColor Green
Write-Host "  Failed: $FailCount" -ForegroundColor $(if ($FailCount -gt 0) { "Red" } else { "Green" })
Write-Host "========================================" -ForegroundColor Magenta

if ($FailCount -gt 0) {
    Write-Host ""
    Write-Host "  Some tests failed! Check the output above." -ForegroundColor Red
    exit 1
} else {
    Write-Host ""
    Write-Host "  All tests passed!" -ForegroundColor Green
    exit 0
}
