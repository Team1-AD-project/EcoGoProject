#!/bin/bash

# EcoGo Deployment Verification Script
# This script verifies that all components of the CI/CD pipeline are working correctly

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Test result tracking
declare -a RESULTS

# Helper functions
print_header() {
    echo -e "\n${BLUE}╔════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║ $1${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════╝${NC}\n"
}

print_test() {
    echo -e "${BLUE}→${NC} $1"
}

print_pass() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    ((PASSED++))
    RESULTS+=("✓ $1")
}

print_fail() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    ((FAILED++))
    RESULTS+=("✗ $1")
}

print_warning() {
    echo -e "${YELLOW}⚠ WARN${NC}: $1"
    ((WARNINGS++))
    RESULTS+=("⚠ $1")
}

# Test functions
test_java() {
    print_header "Testing Java Environment"
    print_test "Checking Java installation..."
    if command -v java >/dev/null 2>&1; then
        VERSION=$(java -version 2>&1 | grep version | cut -d'"' -f2)
        print_pass "Java installed: $VERSION"
    else
        print_fail "Java not found"
        return 1
    fi
}

test_maven() {
    print_header "Testing Maven"
    print_test "Checking Maven installation..."
    if command -v mvn >/dev/null 2>&1; then
        VERSION=$(mvn -v 2>&1 | head -1)
        print_pass "Maven installed: $VERSION"
    else
        print_fail "Maven not found"
        return 1
    fi

    print_test "Running Checkstyle..."
    if mvn checkstyle:check -q 2>/dev/null; then
        print_pass "Checkstyle passed"
    else
        print_warning "Checkstyle issues found (check: mvn checkstyle:checkstyle)"
    fi
}

test_docker() {
    print_header "Testing Docker"
    print_test "Checking Docker installation..."
    if command -v docker >/dev/null 2>&1; then
        print_pass "Docker installed"
    else
        print_fail "Docker not found"
        return 1
    fi

    print_test "Checking Docker daemon..."
    if docker ps >/dev/null 2>&1; then
        print_pass "Docker daemon running"
    else
        print_fail "Docker daemon not running"
        return 1
    fi
}

test_containers() {
    print_header "Testing Running Containers"

    print_test "Checking MongoDB..."
    if docker ps | grep -q "ecogo-mongodb"; then
        print_pass "MongoDB container running"
    else
        print_warning "MongoDB container not running"
    fi

    print_test "Checking Prometheus..."
    if docker ps | grep -q "ecogo-prometheus"; then
        print_pass "Prometheus container running"
    else
        print_warning "Prometheus container not running"
    fi

    print_test "Checking Grafana..."
    if docker ps | grep -q "ecogo-grafana"; then
        print_pass "Grafana container running"
    else
        print_warning "Grafana container not running"
    fi

    print_test "Checking EcoGo application..."
    if docker ps | grep -q "ecogo-app"; then
        print_pass "EcoGo application container running"
    else
        print_warning "EcoGo application container not running"
    fi
}

test_endpoints() {
    print_header "Testing Application Endpoints"

    # Check if container is running first
    if ! docker ps | grep -q "ecogo-app"; then
        print_warning "Application container not running, skipping endpoint tests"
        return 0
    fi

    print_test "Testing health endpoint..."
    if curl -s http://localhost:8090/actuator/health >/dev/null 2>&1; then
        HEALTH=$(curl -s http://localhost:8090/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$HEALTH" = "UP" ]; then
            print_pass "Application health endpoint: $HEALTH"
        else
            print_warning "Application health endpoint: $HEALTH (not UP)"
        fi
    else
        print_fail "Cannot reach health endpoint"
    fi

    print_test "Testing metrics endpoint..."
    if curl -s http://localhost:8090/actuator/prometheus >/dev/null 2>&1; then
        METRICS=$(curl -s http://localhost:8090/actuator/prometheus | wc -l)
        print_pass "Metrics endpoint available ($METRICS lines)"
    else
        print_warning "Cannot reach metrics endpoint"
    fi

    print_test "Testing info endpoint..."
    if curl -s http://localhost:8090/actuator/info >/dev/null 2>&1; then
        print_pass "Info endpoint available"
    else
        print_warning "Cannot reach info endpoint"
    fi
}

test_monitoring() {
    print_header "Testing Monitoring Stack"

    print_test "Testing Prometheus..."
    if curl -s http://localhost:9090/-/healthy >/dev/null 2>&1; then
        print_pass "Prometheus responding"
    else
        print_warning "Prometheus not responding (may not be running)"
    fi

    print_test "Testing Grafana..."
    if curl -s http://localhost:3000/api/health >/dev/null 2>&1; then
        print_pass "Grafana responding"
    else
        print_warning "Grafana not responding (may not be running)"
    fi

    print_test "Testing MongoDB..."
    if docker exec ecogo-mongodb mongosh --eval "db.adminCommand('ping')" >/dev/null 2>&1; then
        print_pass "MongoDB responding"
    else
        print_warning "MongoDB not responding or not running"
    fi
}

test_sast() {
    print_header "Testing SAST Tools"

    print_test "Checking SpotBugs..."
    if command -v spotbugs >/dev/null 2>&1; then
        print_pass "SpotBugs installed"
    else
        print_warning "SpotBugs not found (install with: mvn spotbugs:spotbugs)"
    fi

    print_test "Checking OWASP Dependency Check..."
    if command -v dependency-check.sh >/dev/null 2>&1; then
        print_pass "OWASP Dependency Check installed"
    else
        print_warning "OWASP Dependency Check not found"
    fi
}

test_terraform() {
    print_header "Testing Terraform"

    print_test "Checking Terraform installation..."
    if command -v terraform >/dev/null 2>&1; then
        VERSION=$(terraform version | head -1)
        print_pass "Terraform installed: $VERSION"
    else
        print_warning "Terraform not found (required for deployment)"
    fi

    if [ -d "terraform" ]; then
        print_test "Checking Terraform configuration..."
        if terraform validate -json 2>/dev/null | grep -q "valid"; then
            print_pass "Terraform configuration valid"
        else
            print_warning "Terraform configuration invalid (run: terraform validate)"
        fi
    fi
}

test_ansible() {
    print_header "Testing Ansible"

    print_test "Checking Ansible installation..."
    if command -v ansible >/dev/null 2>&1; then
        VERSION=$(ansible --version | head -1)
        print_pass "Ansible installed: $VERSION"
    else
        print_warning "Ansible not found (required for deployment)"
    fi

    if [ -f "ansible/inventory.ini" ]; then
        print_test "Checking Ansible inventory..."
        print_pass "Ansible inventory file exists"
    fi

    if [ -f "ansible/deploy.yml" ]; then
        print_test "Checking Ansible playbook..."
        print_pass "Ansible playbook file exists"
    fi
}

test_github() {
    print_header "Testing GitHub Configuration"

    print_test "Checking GitHub Actions workflow..."
    if [ -f ".github/workflows/cicd-pipeline.yml" ]; then
        print_pass "GitHub Actions workflow configured"
    else
        print_fail "GitHub Actions workflow not found"
    fi

    print_test "Checking pull request template..."
    if [ -f ".github/pull_request_template.md" ]; then
        print_pass "Pull request template configured"
    else
        print_warning "Pull request template not found"
    fi

    print_test "Checking secrets template..."
    if [ -f ".github/SECRETS-TEMPLATE.md" ]; then
        print_pass "Secrets template available"
    else
        print_warning "Secrets template not found"
    fi
}

print_summary() {
    print_header "Verification Summary"

    echo -e "${BLUE}Results:${NC}"
    for result in "${RESULTS[@]}"; do
        echo "  $result"
    done

    echo ""
    echo -e "${GREEN}Passed:${NC} $PASSED"
    echo -e "${YELLOW}Warnings:${NC} $WARNINGS"
    echo -e "${RED}Failed:${NC} $FAILED"

    if [ $FAILED -eq 0 ]; then
        echo -e "\n${GREEN}✓ All critical checks passed!${NC}"
        return 0
    else
        echo -e "\n${RED}✗ Some checks failed. Please review above.${NC}"
        return 1
    fi
}

# Main execution
main() {
    echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   EcoGo CI/CD Deployment Verification   ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"

    test_java
    test_maven
    test_docker
    test_containers
    test_endpoints
    test_monitoring
    test_sast
    test_terraform
    test_ansible
    test_github

    print_summary
}

# Run main
main
