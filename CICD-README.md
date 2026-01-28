# EcoGo CI/CD Pipeline - Quick Start

## 🎯 What Has Been Configured

A comprehensive CI/CD pipeline has been set up for the EcoGo project with the following components:

### 1. **GitHub Actions Pipeline** (`.github/workflows/cicd-pipeline.yml`)
Complete DevSecOps workflow with:
- ✅ LINT (Checkstyle)
- ✅ SAST (SpotBugs, OWASP Dependency Check)
- ✅ SonarQube Analysis
- ✅ Build & Docker Image Creation
- ✅ Terraform Infrastructure Deployment
- ✅ Ansible Configuration Management
- ✅ Integration Tests
- ✅ DAST (OWASP ZAP)
- ✅ Monitoring Setup (Prometheus & Grafana)

### 2. **Security Scanning**
- Checkstyle for code quality
- SpotBugs for static bug detection
- OWASP Dependency Check for vulnerable dependencies
- OWASP ZAP for dynamic security testing

### 3. **Infrastructure as Code**
- **Terraform**: AWS ECS deployment configuration in `terraform/`
- **Ansible**: Configuration management playbooks in `ansible/`

### 4. **Monitoring Stack**
- **Prometheus**: Metrics collection (`monitoring/prometheus.yml`)
- **Grafana**: Visualization dashboards (`monitoring/dashboards/`)
- **Docker Compose**: Local monitoring stack (`monitoring/docker-compose.yml`)

### 5. **Helper Scripts**
- `scripts/local-deploy.sh`: Local deployment and testing
- `scripts/verify-deployment.sh`: Deployment verification

## 🚀 Quick Start Guide

### Step 1: Configure GitHub Secrets

Follow the instructions in `.github/SECRETS-TEMPLATE.md` to add these secrets to your GitHub repository:

1. `AWS_ACCESS_KEY_ID`
2. `AWS_SECRET_ACCESS_KEY`
3. `SONAR_HOST_URL`
4. `SONAR_TOKEN`
5. `MONGODB_URI`

### Step 2: Test Locally

```bash
# Option 1: Run monitoring stack
cd monitoring
docker-compose up -d

# Option 2: Use deployment script
./scripts/local-deploy.sh
# Select option 5 for full deployment

# Verify deployment
./scripts/verify-deployment.sh
```

### Step 3: Commit and Push

```bash
# Add all CI/CD configuration files
git add .github/ terraform/ ansible/ monitoring/ scripts/ Dockerfile pom.xml

# Commit changes
git commit -m "Add comprehensive CI/CD pipeline with monitoring"

# Push to trigger pipeline
git push origin main
```

### Step 4: Monitor Pipeline

1. Go to GitHub Actions tab
2. Watch the "EcoGo CI/CD Pipeline" workflow
3. Review security scan results
4. Check deployment status

## 📊 Access Points

After deployment, access:

### Local Development
- **Application**: http://localhost:8090
- **Health Check**: http://localhost:8090/actuator/health
- **Metrics**: http://localhost:8090/actuator/prometheus
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

### Deployed Environment
- **Application**: Check Terraform outputs or ALB DNS
- **CloudWatch Logs**: AWS Console → CloudWatch
- **ECS Service**: AWS Console → ECS

## 📁 File Structure

```
EcoGo/
├── .github/
│   ├── workflows/
│   │   └── cicd-pipeline.yml         # Main CI/CD pipeline
│   ├── SECRETS-TEMPLATE.md           # Secrets configuration guide
│   └── pull_request_template.md      # PR template
│
├── terraform/
│   ├── main.tf                       # Main infrastructure config
│   ├── variables.tf                  # Terraform variables
│   ├── outputs.tf                    # Terraform outputs
│   └── terraform.tfvars.example      # Example variables
│
├── ansible/
│   ├── ansible.cfg                   # Ansible configuration
│   ├── inventory.ini                 # Server inventory
│   ├── deploy.yml                    # Main deployment playbook
│   ├── templates/                    # Configuration templates
│   └── tasks/                        # Ansible tasks
│
├── monitoring/
│   ├── docker-compose.yml            # Monitoring stack
│   ├── prometheus.yml                # Prometheus config
│   ├── alerts.yml                    # Alert rules
│   ├── grafana-provisioning.yml      # Grafana setup
│   └── dashboards/                   # Grafana dashboards
│
├── scripts/
│   ├── local-deploy.sh               # Local deployment script
│   └── verify-deployment.sh          # Verification script
│
├── .zap/
│   ├── rules.tsv                     # ZAP security rules
│   └── zap-config.yaml               # ZAP configuration
│
├── Dockerfile                        # Docker image definition
├── sonar-project.properties          # SonarQube config
├── CI-CD-SETUP.md                    # Detailed setup guide
└── CICD-README.md                    # This file
```

## 🔍 Pipeline Stages Explained

### 1. Lint (on push)
- Runs Checkstyle
- Validates code quality
- Generates reports

### 2. SAST (on push)
- SpotBugs: Static bug detection
- OWASP Dependency Check: Vulnerability scanning

### 3. Build (after SAST)
- Maven build
- Docker image creation
- Artifact generation

### 4. SonarQube (after build)
- Code quality analysis
- Security hotspot detection
- Quality gate validation

### 5. Deploy (main branch only)
- Terraform infrastructure provisioning
- Ansible configuration
- ECS deployment

### 6. Integration Tests (after deploy)
- MongoDB integration tests
- API endpoint tests

### 7. DAST (after deploy)
- OWASP ZAP scanning
- Dynamic security testing
- Vulnerability reporting

### 8. Monitoring Setup (after DAST)
- Prometheus deployment
- Grafana configuration
- Metrics collection

## 🛠️ Common Commands

### Local Development

```bash
# Build application
mvn clean package

# Run security scans
mvn checkstyle:check spotbugs:check dependency-check:check

# Build Docker image
docker build -t ecogo:latest .

# Start monitoring stack
cd monitoring && docker-compose up -d

# Full local deployment
./scripts/local-deploy.sh
```

### Terraform

```bash
cd terraform
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
```

### Ansible

```bash
cd ansible
ansible-playbook deploy.yml -i inventory.ini
```

## 📝 Configuration Files

### pom.xml Updates
- Added Checkstyle plugin
- Added SpotBugs plugin
- Added OWASP Dependency Check
- Added JaCoCo for code coverage
- Added SonarQube scanner
- Added Spring Boot Actuator
- Added Micrometer Prometheus

### application.yaml Updates
- Added Actuator endpoints
- Added Prometheus metrics export
- Added health check configuration

## 🔐 Security Best Practices

1. **Never commit secrets** to git
2. Use GitHub Secrets for sensitive data
3. Use Ansible Vault for deployment secrets
4. Review SAST/DAST reports before merging
5. Keep dependencies updated
6. Monitor security alerts in GitHub

## 📈 Monitoring & Alerts

### Prometheus Metrics
- HTTP request rate
- Response latency (P50, P95, P99)
- Error rates by status code
- JVM memory usage
- Garbage collection metrics

### Grafana Dashboards
- Application overview
- System metrics
- Performance metrics
- Error tracking

### Alert Rules
- Application down
- High error rate
- High latency
- Memory usage warnings
- Frequent GC

## 🚦 Testing the Pipeline

### Before Pushing
```bash
# Run local verification
./scripts/verify-deployment.sh

# Test specific components
mvn clean verify                    # All checks
mvn checkstyle:check                # Code style
mvn spotbugs:check                  # Bug detection
mvn dependency-check:check          # Vulnerabilities
```

### After Pushing
1. Check GitHub Actions for pipeline status
2. Review security scan reports in artifacts
3. Check SonarQube dashboard for quality metrics
4. Verify deployment in AWS Console
5. Test application endpoints

## 📚 Additional Documentation

- **Detailed Setup**: See `CI-CD-SETUP.md`
- **GitHub Secrets**: See `.github/SECRETS-TEMPLATE.md`
- **Terraform**: See `terraform/README.md` (create if needed)
- **Ansible**: See `ansible/README.md` (create if needed)

## 🎯 Feature Deployment

The pipeline is configured to deploy the **Authentication feature** (AuthController) as a proof of concept.

To deploy:
1. Make changes to authentication code
2. Create a feature branch
3. Open a Pull Request
4. Pipeline runs automatically
5. Merge to `main` to deploy

## ⚠️ Known Limitations

1. Terraform requires AWS credentials
2. SonarQube requires an instance (cloud or self-hosted)
3. DAST requires application to be running
4. Monitoring stack requires Docker

## 🆘 Troubleshooting

### Pipeline fails at LINT stage
- Review Checkstyle reports in GitHub Actions artifacts
- Fix code style issues locally first

### SAST reports vulnerabilities
- Review dependency-check report
- Update vulnerable dependencies in pom.xml

### Terraform fails
- Verify AWS credentials are correct
- Check IAM permissions
- Review Terraform state

### Application won't start
- Check MongoDB connection string
- Review application logs
- Verify environment variables

## 🎉 Next Steps

1. ✅ Configure GitHub Secrets
2. ✅ Test locally with monitoring stack
3. ✅ Push to GitHub to trigger pipeline
4. ✅ Review security scan results
5. ✅ Configure SonarQube project
6. ✅ Set up AWS infrastructure
7. ✅ Deploy to staging environment
8. ✅ Configure Grafana dashboards
9. ✅ Set up alert notifications
10. ✅ Deploy to production

## 📞 Support

For issues or questions:
- Review GitHub Actions logs
- Check monitoring dashboards
- Consult `CI-CD-SETUP.md` for detailed documentation
- Review security scan reports

---

**Note**: This CI/CD pipeline is production-ready but should be customized based on your specific requirements and infrastructure setup.

**Configured by**: Claude Code Assistant
**Date**: 2026-01-28
**Version**: 1.0
