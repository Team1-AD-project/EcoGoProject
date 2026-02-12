#!/bin/bash

# SonarQube Configuration Checker for GitHub Actions
# This script helps verify SonarQube/SonarCloud setup

echo "========================================="
echo "SonarQube Configuration Checker"
echo "========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if SONAR_TOKEN is set
if [ -z "$SONAR_TOKEN" ]; then
    echo -e "${RED}❌ SONAR_TOKEN is NOT configured${NC}"
    echo "   Please add SONAR_TOKEN to GitHub Secrets"
    echo "   See docs/SONARQUBE-SETUP.md for instructions"
    TOKEN_OK=false
else
    echo -e "${GREEN}✅ SONAR_TOKEN is configured${NC}"
    TOKEN_OK=true
fi

echo ""

# Check if SONAR_HOST_URL is set
if [ -z "$SONAR_HOST_URL" ]; then
    echo -e "${RED}❌ SONAR_HOST_URL is NOT configured${NC}"
    echo "   Please add SONAR_HOST_URL to GitHub Secrets"
    echo "   Example: https://sonarcloud.io"
    HOST_OK=false
else
    echo -e "${GREEN}✅ SONAR_HOST_URL is configured: ${SONAR_HOST_URL}${NC}"
    HOST_OK=true
    
    # Check if using SonarCloud
    if [[ "$SONAR_HOST_URL" == *"sonarcloud.io"* ]]; then
        echo -e "${YELLOW}   ℹ️  Using SonarCloud - organization parameter will be added${NC}"
    else
        echo -e "${YELLOW}   ℹ️  Using self-hosted SonarQube${NC}"
    fi
fi

echo ""
echo "========================================="
echo "Configuration Summary"
echo "========================================="

if [ "$TOKEN_OK" = true ] && [ "$HOST_OK" = true ]; then
    echo -e "${GREEN}✅ SonarQube is properly configured!${NC}"
    echo ""
    echo "SonarQube analysis will run on:"
    echo "  - Push events to main, develop, feature/** branches"
    echo "  - Pull requests to main, develop, feature/** branches"
    echo ""
    echo "Next steps:"
    echo "  1. Push a commit to trigger the pipeline"
    echo "  2. Check GitHub Actions → SonarQube Analysis job"
    echo "  3. View results in SonarQube/SonarCloud dashboard"
    exit 0
else
    echo -e "${RED}❌ SonarQube is NOT properly configured${NC}"
    echo ""
    echo "Please follow the setup guide:"
    echo "  📖 docs/SONARQUBE-SETUP.md"
    echo ""
    echo "Quick setup:"
    echo "  1. Go to https://sonarcloud.io (or your SonarQube server)"
    echo "  2. Create/import the EcoGo project"
    echo "  3. Generate an authentication token"
    echo "  4. Add secrets to GitHub:"
    echo "     - SONAR_TOKEN: <your-token>"
    echo "     - SONAR_HOST_URL: https://sonarcloud.io"
    exit 1
fi
