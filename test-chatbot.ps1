<#
.SYNOPSIS
    EcoGo Chatbot Knowledge Base Test Script
.DESCRIPTION
    Tests chatbot RAG Q&A covering all knowledge base topics.
    Also prints an Android manual test guide.
.PARAMETER BaseUrl
    Backend URL, default http://localhost:8090
.EXAMPLE
    .\test-chatbot.ps1
    .\test-chatbot.ps1 -BaseUrl "http://47.129.124.55:8090"
#>
param(
    [string]$BaseUrl = "http://localhost:8090"
)

$chatEndpoint = "$BaseUrl/api/v1/mobile/chatbot/chat"
$healthEndpoint = "$BaseUrl/api/v1/mobile/chatbot/health"

function Write-Pass  { param($msg) Write-Host "  [PASS] " -NoNewline -ForegroundColor Green;  Write-Host $msg }
function Write-Fail  { param($msg) Write-Host "  [FAIL] " -NoNewline -ForegroundColor Red;    Write-Host $msg }
function Write-Info  { param($msg) Write-Host "  [INFO] " -NoNewline -ForegroundColor Cyan;   Write-Host $msg }
function Write-Title { param($msg) Write-Host "`n=== $msg ===" -ForegroundColor Yellow }

# Health check
Write-Title "0. Health Check"
try {
    $health = Invoke-RestMethod -Uri $healthEndpoint -Method GET -TimeoutSec 15
    if ($health.data -match "running") {
        Write-Pass "Chatbot module is running"
    } else {
        Write-Fail "Unexpected health response: $($health | ConvertTo-Json -Compress)"
    }
} catch {
    Write-Fail "Health check failed: $($_.Exception.Message)"
    Write-Host "`n[!] Backend may not be running. Continuing anyway...`n" -ForegroundColor Red
}

# Build test cases
$ids          = @()
$groups       = @()
$messages     = @()
$keywords     = @()
$descriptions = @()

function Add-Test($id, $group, $msg, $kws, $desc) {
    $script:ids          += $id
    $script:groups       += $group
    $script:messages     += $msg
    $script:keywords     += ($kws -join "|")
    $script:descriptions += $desc
}

# Group 1: NUS Shuttle Routes
Add-Test 1  "NUS-Shuttle-Routes"  "NUS D1 D2 shuttle bus routes stops"              "D1|A1|shuttle"                "Shuttle route query"
Add-Test 2  "NUS-Shuttle-Routes"  "Tell me about the A1 and A2 bus routes"          "A1|A2|Kent Ridge"             "A1/A2 route query"
Add-Test 3  "NUS-Shuttle-Routes"  "NUS shuttle route E express UTown"               "UTown"                        "Route E express"

# Group 2: NUS Shuttle Tips
Add-Test 4  "NUS-Shuttle-Tips"    "When are the buses most crowded on campus?"       "peak|crowded"                 "Peak hours"
Add-Test 5  "NUS-Shuttle-Tips"    "NUS shuttle weekend Saturday Sunday service"      "Saturday|Sunday"              "Weekend bus"

# Group 3: NUS Buildings
Add-Test 6  "NUS-Buildings"       "Where is the School of Computing located?"        "COM|Computing"                "SoC location"
Add-Test 7  "NUS-Buildings"       "NUS Business School BIZ faculty building"         "BIZ|Business"                 "Business School"
Add-Test 8  "NUS-Buildings"       "NUS Faculty of Engineering E1 E2 buildings"       "Engineering"                  "Engineering buildings"

# Group 4: NUS Food
Add-Test 9  "NUS-Food"            "NUS campus canteen food Deck Frontier dining"     "canteen|Deck"                 "Campus canteens"
Add-Test 10 "NUS-Food"            "UTown Fine Food canteen dining"                   "Fine Food|UTown"              "UTown dining"

# Group 5: NUS Libraries
Add-Test 11 "NUS-Libraries"       "NUS library Central Library CLB study"            "Central Library|CLB"          "Library locations"
Add-Test 12 "NUS-Libraries"       "NUS study spots library late night"               "study|Library"                "Study spots"

# Group 6: NUS Sports
Add-Test 13 "NUS-Sports"          "Does NUS have a swimming pool?"                   "pool|swim|USC"                "Swimming pool"
Add-Test 14 "NUS-Sports"          "NUS MPSH badminton sports facility"               "MPSH|badminton"               "Badminton courts"

# Group 7: NUS Residential
Add-Test 15 "NUS-Residential"     "Tell me about UTown residences"                   "Cinnamon|Tembusu"             "UTown residences"
Add-Test 16 "NUS-Residential"     "NUS PGP residence Prince George Park travel"      "PGP"                          "PGP residence"

# Group 8: EcoGo Rewards
Add-Test 17 "EcoGo-Rewards"       "How do I earn green points?"                      "points|walking"               "Earning points"
Add-Test 18 "EcoGo-Rewards"       "EcoGo points voucher VIP rewards spend"           "voucher|VIP|points"           "Spending points"

# Group 9: EcoGo Chatbot Guide
Add-Test 19 "EcoGo-Chatbot"       "What can the LiNUS chatbot assistant do?"         "bus|book|travel"              "LiNUS capabilities"

# Group 10: EcoGo App
Add-Test 20 "EcoGo-App"           "EcoGo app home screen routes community features"  "Home|Route"                   "App how-to"

# Group 11: NUS Sustainability
Add-Test 21 "NUS-Sustainability"  "What is NUS doing for sustainability?"             "solar|carbon|green"           "NUS green plan"

# Group 12: NUS Safety
Add-Test 22 "NUS-Safety"          "What is the campus emergency number?"              "6874|security|emergency"      "Emergency contacts"
Add-Test 23 "NUS-Safety"          "NUS campus security night safety patrol"           "security|night|patrol"        "Night safety"

# Group 13: Singapore Cycling
Add-Test 24 "SG-Cycling"          "Singapore shared bike Anywheel cycling rental"     "Anywheel|cycling"             "Shared bike"
Add-Test 25 "SG-Cycling"          "NUS campus cycling bike rack path"                 "bike|cycling"                 "Campus cycling"

# Group 14: EcoGo Challenges
Add-Test 26 "EcoGo-Challenges"    "EcoGo challenge streak weekly monthly activity"    "challenge|streak"             "Challenges"

# Group 15: Kent Ridge MRT
Add-Test 27 "Kent-Ridge-MRT"      "Kent Ridge MRT shuttle bus NUS campus connection"  "Kent Ridge|shuttle"           "MRT to campus"

# Group 16: Weather
Add-Test 28 "Weather"             "NUS campus hot weather sheltered travel tips"       "hot|shelter"                  "Hot weather tips"
Add-Test 29 "Weather"             "Singapore rain umbrella sheltered walkway travel"   "rain|umbrella"                "Rainy day tips"

# Group 17: Regression - existing knowledge
Add-Test 30 "Carbon-Footprint"    "What is carbon footprint?"                         "carbon|emission"              "Carbon basics"
Add-Test 31 "SG-Fares"            "Singapore MRT fare EZ-Link price SGD"              "MRT|fare|SGD"                 "MRT fares"
Add-Test 32 "Green-Travel"        "What is green travel?"                             "green|carbon|walking"         "Green travel basics"
Add-Test 33 "EZ-Link"             "How do I use an EZ-Link card?"                     "EZ-Link|MRT"                  "EZ-Link usage"
Add-Test 34 "NUS-Campus"          "NUS campus COM3 UTown shuttle D1 walking"          "COM3|UTown|D1"                "Campus route"
Add-Test 35 "EcoGo-Features"      "EcoGo app features tracking route points"          "tracking|route|point"         "EcoGo features"

# Group 18: Chatbot Interactive
Add-Test 36 "Chatbot-Menu"        "menu"                                              "help"                         "Main menu"
Add-Test 37 "Bus-Arrivals"        "Bus Arrivals"                                      "stop|bus"                     "Bus arrivals"
Add-Test 38 "Travel-Advice"       "Travel Advice"                                     "go|travel"                    "Travel advice"

# Run tests
$passed = 0
$failed = 0
$errors = 0
$failedList = @()

$currentGroup = ""

for ($i = 0; $i -lt $ids.Count; $i++) {
    $id    = $ids[$i]
    $group = $groups[$i]
    $msg   = $messages[$i]
    $kwStr = $keywords[$i]
    $desc  = $descriptions[$i]
    $kwArr = $kwStr -split "\|"

    if ($group -ne $currentGroup) {
        $currentGroup = $group
        Write-Title $group
    }

    Write-Host "`n  #$id [$desc]" -ForegroundColor White
    Write-Host "  Q: `"$msg`"" -ForegroundColor Gray

    $body = "{`"message`":`"$msg`"}"

    try {
        $response = Invoke-RestMethod -Uri $chatEndpoint -Method POST `
            -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -TimeoutSec 20

        $text = ""
        if ($response.data -and $response.data.assistant -and $response.data.assistant.text) {
            $text = $response.data.assistant.text
        } elseif ($response.data -and $response.data.reply) {
            $text = $response.data.reply
        }

        if ([string]::IsNullOrWhiteSpace($text)) {
            Write-Fail "Empty response"
            $failed++
            $failedList += "#$id $desc - Empty response"
            continue
        }

        $preview = if ($text.Length -gt 180) { $text.Substring(0, 180) + "..." } else { $text }
        Write-Info "A: $preview"

        # Check citations
        $citCount = 0
        if ($response.data -and $response.data.assistant -and $response.data.assistant.citations) {
            $citCount = $response.data.assistant.citations.Count
            if ($citCount -gt 0) {
                $citTitles = ($response.data.assistant.citations | ForEach-Object { $_.title }) -join ", "
                Write-Info "Citations($citCount): $citTitles"
            }
        }

        # Check keywords (case-insensitive)
        $missing = @()
        foreach ($kw in $kwArr) {
            if ($text -notmatch "(?i)$([regex]::Escape($kw))") {
                $missing += $kw
            }
        }

        if ($missing.Count -eq 0) {
            $extra = if ($citCount -gt 0) { " + $citCount citation(s)" } else { "" }
            Write-Pass "All $($kwArr.Count) keywords matched$extra"
            $passed++
        } else {
            Write-Fail "Missing keywords: $($missing -join ', ')"
            $failed++
            $failedList += "#$id $desc - Missing: $($missing -join ', ')"
        }

    } catch {
        Write-Fail "HTTP Error: $($_.Exception.Message)"
        $errors++
        $failedList += "#$id $desc - ERROR: $($_.Exception.Message)"
    }
}

# Summary
Write-Title "TEST SUMMARY"
$total = $passed + $failed + $errors
Write-Host "  Total:  $total" -ForegroundColor White
Write-Host "  Passed: $passed" -ForegroundColor Green
if ($failed -gt 0) { Write-Host "  Failed: $failed" -ForegroundColor Red } else { Write-Host "  Failed: 0" -ForegroundColor Green }
if ($errors -gt 0) { Write-Host "  Errors: $errors" -ForegroundColor Red } else { Write-Host "  Errors: 0" -ForegroundColor Green }

$pct = if ($total -gt 0) { [math]::Round(($passed / $total) * 100, 1) } else { 0 }
if ($pct -ge 80)    { Write-Host "  Pass Rate: $pct%" -ForegroundColor Green }
elseif ($pct -ge 60) { Write-Host "  Pass Rate: $pct%" -ForegroundColor Yellow }
else                  { Write-Host "  Pass Rate: $pct%" -ForegroundColor Red }

if ($failedList.Count -gt 0) {
    Write-Host "`n  --- Failed / Error Cases ---" -ForegroundColor Red
    foreach ($f in $failedList) { Write-Host "    $f" -ForegroundColor Red }
}

# Android Manual Test Guide
Write-Title "ANDROID FRONTEND MANUAL TEST GUIDE"
Write-Host ""
Write-Host "  Type these in the Chat tab on your Android device:"
Write-Host "  ================================================================"
Write-Host "  #  | Message                                | Expect"
Write-Host "  ================================================================"
Write-Host "   1 | menu                                   | 4 buttons shown"
Write-Host "   2 | When is the next bus at COM3?          | Real-time arrival"
Write-Host "   3 | Book a trip from Science to UTown      | Booking flow"
Write-Host "   4 | What stops does D1 go to?              | D1 stops listed"
Write-Host "   5 | Where can I eat on campus?             | Canteen names"
Write-Host "   6 | Where is School of Computing?          | COM1/2/3 info"
Write-Host "   7 | How do I earn green points?            | Points per mode"
Write-Host "   8 | Is there a gym at UTown?               | UTown gym/SRC"
Write-Host "   9 | Where can I study late at night?       | 24/7 study spots"
Write-Host "  10 | What is the NUS emergency number?      | 6874 1616"
Write-Host "  11 | How do I rent a shared bike?           | Anywheel/SG Bike"
Write-Host "  12 | What challenges can I join?            | Weekly/monthly"
Write-Host "  13 | How to get from Kent Ridge MRT?        | D2/A1/A2/E shuttle"
Write-Host "  14 | What about traveling when it rains?    | Umbrella, shelter"
Write-Host "  15 | Tell me about UTown residences         | 4 colleges listed"
Write-Host "  16 | What is carbon footprint?              | Emission comparisons"
Write-Host "  17 | How much does MRT cost?                | SGD fare range"
Write-Host "  18 | What is NUS doing for sustainability?  | Solar, green plan"
Write-Host "  19 | Show my profile                        | Profile card"
Write-Host "  20 | Bus Arrivals                           | Stop selection"
Write-Host "  ================================================================"
Write-Host ""
Write-Host "  CHECK: 1) Relevant answer  2) Citations shown  3) UI actions work  4) < 5s load"
Write-Host ""
Write-Host "  Done!" -ForegroundColor Green
Write-Host ""
