#!/bin/bash

# EcoGo Local Deployment Script
# This script helps you deploy and test EcoGo locally

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}EcoGo Local Deployment Script${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Function to print colored messages
print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Check prerequisites
print_message "Checking prerequisites..."

command -v java >/dev/null 2>&1 || { print_error "Java is required but not installed. Aborting."; exit 1; }
command -v mvn >/dev/null 2>&1 || { print_error "Maven is required but not installed. Aborting."; exit 1; }
command -v docker >/dev/null 2>&1 || { print_error "Docker is required but not installed. Aborting."; exit 1; }

print_message "All prerequisites satisfied."
echo ""

# Menu
echo "Select deployment option:"
echo "1. Build application only"
echo "2. Run security scans (LINT + SAST)"
echo "3. Build Docker image"
echo "4. Deploy with monitoring stack (Prometheus + Grafana)"
echo "5. Full deployment (Build + Docker + Monitoring)"
echo "6. Stop all containers"
read -p "Enter option (1-6): " option

case $option in
    1)
        print_message "Building application..."
        mvn clean package -DskipTests
        print_message "Build completed successfully!"
        ;;

    2)
        print_message "Running security scans..."

        print_message "Running Checkstyle..."
        mvn checkstyle:check || print_warning "Checkstyle found issues"

        print_message "Running SpotBugs..."
        mvn spotbugs:check || print_warning "SpotBugs found issues"

        print_message "Running OWASP Dependency Check..."
        mvn dependency-check:check || print_warning "Dependency check found vulnerabilities"

        print_message "Security scans completed. Check reports in target/ directory."
        ;;

    3)
        print_message "Building Docker image..."
        mvn clean package -DskipTests
        docker build -t ecogo:latest .
        print_message "Docker image built successfully!"
        ;;

    4)
        print_message "Deploying monitoring stack..."
        cd monitoring
        docker-compose up -d
        print_message "Monitoring stack deployed!"
        echo ""
        echo -e "${GREEN}Access URLs:${NC}"
        echo "  - Prometheus: http://localhost:9090"
        echo "  - Grafana: http://localhost:3000 (admin/admin)"
        echo "  - MongoDB: mongodb://localhost:27017"
        cd ..
        ;;

    5)
        print_message "Full deployment starting..."

        # Build
        print_message "Step 1/4: Building application..."
        mvn clean package -DskipTests

        # Docker image
        print_message "Step 2/4: Building Docker image..."
        docker build -t ecogo:latest .

        # Deploy monitoring
        print_message "Step 3/4: Deploying monitoring stack..."
        cd monitoring
        docker-compose up -d
        cd ..

        # Wait for MongoDB
        print_message "Waiting for MongoDB to be ready..."
        sleep 10

        # Deploy application
        print_message "Step 4/4: Deploying application..."
        docker run -d \
            --name ecogo-app \
            --network monitoring_ecogo-monitoring \
            -p 8090:8090 \
            -e SPRING_DATA_MONGODB_URI=mongodb://ecogo-mongodb:27017/EcoGo \
            -e SPRING_PROFILES_ACTIVE=dev \
            ecogo:latest

        print_message "Waiting for application to start..."
        sleep 20

        # Health check
        print_message "Checking application health..."
        if curl -f http://localhost:8090/actuator/health > /dev/null 2>&1; then
            print_message "Application is healthy!"
        else
            print_warning "Application health check failed. Check logs with: docker logs ecogo-app"
        fi

        echo ""
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN}Deployment completed successfully!${NC}"
        echo -e "${GREEN}========================================${NC}"
        echo ""
        echo -e "${GREEN}Access URLs:${NC}"
        echo "  - Application: http://localhost:8090"
        echo "  - Health Check: http://localhost:8090/actuator/health"
        echo "  - Metrics: http://localhost:8090/actuator/prometheus"
        echo "  - Prometheus: http://localhost:9090"
        echo "  - Grafana: http://localhost:3000 (admin/admin)"
        echo ""
        echo -e "${GREEN}Useful commands:${NC}"
        echo "  - View logs: docker logs -f ecogo-app"
        echo "  - Stop application: docker stop ecogo-app"
        echo "  - Remove application: docker rm ecogo-app"
        echo "  - Stop all: ./scripts/local-deploy.sh (option 6)"
        ;;

    6)
        print_message "Stopping all containers..."
        docker stop ecogo-app 2>/dev/null || true
        docker rm ecogo-app 2>/dev/null || true
        cd monitoring
        docker-compose down
        cd ..
        print_message "All containers stopped and removed."
        ;;

    *)
        print_error "Invalid option"
        exit 1
        ;;
esac

echo ""
print_message "Script completed!"
