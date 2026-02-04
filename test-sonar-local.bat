@echo off
REM SonarQube Local Test Script for Windows
REM This script helps you test SonarQube scan locally with detailed logs

echo =========================================
echo SonarQube Local Test Script
echo =========================================
echo.

REM Check if SONAR_HOST_URL is set
if "%SONAR_HOST_URL%"=="" (
    echo ERROR: SONAR_HOST_URL is not set
    echo Please set it using: set SONAR_HOST_URL=https://sonarcloud.io
    echo.
    pause
    exit /b 1
)

REM Check if SONAR_TOKEN is set
if "%SONAR_TOKEN%"=="" (
    echo ERROR: SONAR_TOKEN is not set
    echo Please set it using: set SONAR_TOKEN=your_token_here
    echo.
    pause
    exit /b 1
)

echo Configuration:
echo   SONAR_HOST_URL: %SONAR_HOST_URL%
echo   SONAR_TOKEN: [HIDDEN]
echo.
echo Starting SonarQube scan with detailed logging...
echo.

REM Run SonarQube scan with detailed logging
mvn clean verify sonar:sonar -X -e ^
  -Dsonar.projectKey=team1-ad-project ^
  -Dsonar.sources=src/main ^
  -Dsonar.tests=src/test ^
  -Dsonar.host.url=%SONAR_HOST_URL% ^
  -Dsonar.login=%SONAR_TOKEN% ^
  -Dsonar.verbose=true

echo.
echo =========================================
echo Scan completed!
echo =========================================
echo.
pause
