# EcoGo CI/CD Pipeline Setup Guide

## Overview

This CI/CD pipeline implements a comprehensive DevSecOps workflow using GitHub Actions, incorporating:

- **LINT**: Code quality checks with Checkstyle
- **SAST**: Static Application Security Testing with SpotBugs and OWASP Dependency Check
- **SonarQube**: Code quality and security analysis
- **DAST**: Dynamic Application Security Testing with OWASP ZAP
- **Infrastructure**: Terraform for IaC
- **Monitoring**: Prometheus and Grafana
- **Deployment**: Ansible for configuration management

## Prerequisites

1. GitHub repository with admin access
2. AWS account (for deployment)
3. SonarQube instance (cloud or self-hosted)
4. MongoDB instance

## GitHub Secrets Configuration

Configure the following secrets in your GitHub repository (Settings → Secrets → Actions):

### Required Secrets

```bash
# AWS Credentials
AWS_ACCESS_KEY_ID=<your-aws-access-key>
AWS_SECRET_ACCESS_KEY=<your-aws-secret-key>

# SonarQube
SONAR_HOST_URL=https://sonarcloud.io  # or your SonarQube instance
SONAR_TOKEN=<your-sonarqube-token>

# MongoDB
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/EcoGo

# Docker Registry (if using private registry)
DOCKER_USERNAME=<github-username>
DOCKER_PASSWORD=<github-token>
```

## Pipeline Stages

### 1. Lint Stage
- Runs Checkstyle for code quality
- Validates Java coding standards
- Generates checkstyle reports

### 2. SAST Stage
- SpotBugs for static bug detection
- OWASP Dependency Check for vulnerable dependencies
- Security vulnerability scanning

### 3. Build Stage
- Maven build with tests
- Docker image creation
- Artifact generation

### 4. SonarQube Stage
- Code quality analysis
- Security hotspot detection
- Code coverage metrics
- Quality gate validation

### 5. Deploy Stage
- Terraform infrastructure provisioning
- Ansible configuration management
- Application deployment to AWS ECS

### 6. Integration Tests
- Runs integration tests against deployed application
- MongoDB integration testing

### 7. DAST Stage
- OWASP ZAP dynamic security scanning
- API endpoint vulnerability testing
- Security report generation

### 8. Monitoring Setup
- Prometheus deployment
- Grafana dashboard configuration
- Metrics collection setup

## Local Development & Testing

### 1. Run Monitoring Stack Locally

```bash
cd monitoring
docker-compose up -d
```

Access:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Application metrics: http://localhost:8090/actuator/prometheus

### 2. Run Local Security Scans

```bash
# Checkstyle
mvn checkstyle:check

# SpotBugs
mvn spotbugs:check

# OWASP Dependency Check
mvn dependency-check:check

# All checks
mvn clean verify
```

### 3. Build Docker Image Locally

```bash
docker build -t ecogo:local .
docker run -p 8090:8090 \
  -e SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/EcoGo \
  ecogo:local
```

## Terraform Deployment

### Initialize Terraform

```bash
cd terraform
terraform init
```

### Plan Infrastructure

```bash
terraform plan -var-file=terraform.tfvars
```

### Apply Infrastructure

```bash
terraform apply -var-file=terraform.tfvars
```

### Destroy Infrastructure

```bash
terraform destroy -var-file=terraform.tfvars
```

## Ansible Deployment

### Configure Inventory

Edit `ansible/inventory.ini` with your server details.

### Deploy Application

```bash
cd ansible
ansible-playbook deploy.yml -i inventory.ini
```

### Deploy to Specific Environment

```bash
# Staging
ansible-playbook deploy.yml -i inventory.ini --limit staging

# Production
ansible-playbook deploy.yml -i inventory.ini --limit production
```

## Monitoring & Dashboards

### Grafana Dashboards

1. Login to Grafana at http://localhost:3000
2. Default credentials: admin/admin
3. Import dashboard: `monitoring/dashboards/ecogo-app-dashboard.json`

### Prometheus Metrics

- Application metrics: http://localhost:8090/actuator/prometheus
- Prometheus UI: http://localhost:9090
- Health check: http://localhost:8090/actuator/health

### Key Metrics to Monitor

- **Request Rate**: `rate(http_requests_total[5m])`
- **Error Rate**: `rate(http_requests_total{status=~"5.."}[5m])`
- **Latency (P95)**: `histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))`
- **JVM Memory**: `jvm_memory_used_bytes / jvm_memory_max_bytes`
- **GC Pause Time**: `rate(jvm_gc_pause_seconds_sum[5m])`

## Triggering the Pipeline

The pipeline automatically runs on:

- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`

### Manual Trigger

1. Go to GitHub Actions tab
2. Select "EcoGo CI/CD Pipeline"
3. Click "Run workflow"

## Security Scanning Reports

### SAST Reports
- Location: `target/spotbugsXml.xml`
- Location: `target/dependency-check-report.html`

### DAST Reports
- Artifact: `dast-report` (in GitHub Actions)
- Local ZAP scan: See `.zap/` directory

### SonarQube
- Dashboard: Your SonarQube instance URL
- Project key: `ecogo`

## Troubleshooting

### Pipeline Failures

1. **Checkstyle Failures**: Review `target/checkstyle-result.xml`
2. **SAST Failures**: Check SpotBugs and dependency-check reports
3. **Build Failures**: Review Maven build logs
4. **Deployment Failures**: Check Terraform/Ansible logs in GitHub Actions

### Common Issues

**Issue**: MongoDB connection timeout
**Solution**: Verify `MONGODB_URI` secret is correctly configured

**Issue**: Docker build fails
**Solution**: Check Dockerfile and ensure all dependencies are available

**Issue**: Terraform apply fails
**Solution**: Verify AWS credentials and permissions

**Issue**: DAST scan fails
**Solution**: Ensure application is running and accessible

## Best Practices

1. **Never commit secrets** - Use GitHub Secrets and Ansible Vault
2. **Review security reports** - Address SAST/DAST findings before deploying
3. **Monitor applications** - Set up alerts in Prometheus/Grafana
4. **Test locally first** - Run security scans locally before pushing
5. **Use staging environment** - Test deployments in staging before production
6. **Keep dependencies updated** - Regularly update Maven dependencies
7. **Review quality gates** - Ensure SonarQube quality gates pass

## Feature Deployment Strategy

This pipeline is configured to deploy individual features. To deploy a specific feature:

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Develop and test your feature
3. Push to GitHub: `git push origin feature/your-feature`
4. Create a Pull Request
5. Pipeline runs automatically on PR
6. Merge to `main` branch to deploy

## Support

For issues or questions:
- Review GitHub Actions logs
- Check application logs in CloudWatch (AWS) or local logs
- Review monitoring dashboards in Grafana
- Consult team documentation

---

**Note**: This setup focuses on deploying the authentication feature (AuthController) as a proof of concept. Expand to other features as needed.
