# SonarQube Setup Guide for EcoGo

## Overview
This guide will help you set up SonarQube integration for the EcoGo project in GitHub Actions.

## Option 1: Using SonarCloud (Recommended - Free for Open Source)

### Step 1: Create SonarCloud Account
1. Go to https://sonarcloud.io
2. Click **"Log in"** and select **"With GitHub"**
3. Authorize SonarCloud to access your GitHub account

### Step 2: Import Your Repository
1. In SonarCloud dashboard, click **"+"** → **"Analyze new project"**
2. Select your organization (Team1-AD-project)
3. Choose the **"EcoGo"** repository
4. Click **"Set Up"**

### Step 3: Configure the Project
1. Choose **"With GitHub Actions"** as the analysis method
2. SonarCloud will show you the configuration steps
3. **Project Key**: Use `team1-ad-project` (this must match the key in your CI/CD workflow)
4. **Organization Key**: Note your organization key (e.g., `team1-ad-project`)

### Step 4: Generate SonarCloud Token
1. Go to **Account → Security** (https://sonarcloud.io/account/security)
2. Under **"Generate Tokens"**:
   - **Name**: `EcoGo-GitHub-Actions`
   - **Type**: Select **"Global Analysis Token"** or **"Project Analysis Token"**
   - **Expiration**: Choose an expiration date (recommend 90 days)
3. Click **"Generate"**
4. **Copy the token** - you won't be able to see it again!

### Step 5: Add Secrets to GitHub
1. Go to your GitHub repository: https://github.com/Team1-AD-project/EcoGo
2. Navigate to **Settings → Secrets and variables → Actions**
3. Click **"New repository secret"**

#### Add SONAR_TOKEN:
- **Name**: `SONAR_TOKEN`
- **Value**: Paste the token you copied from SonarCloud
- Click **"Add secret"**

#### Add SONAR_HOST_URL:
- Click **"New repository secret"** again
- **Name**: `SONAR_HOST_URL`
- **Value**: `https://sonarcloud.io`
- Click **"Add secret"**

### Step 6: Update CI/CD Configuration (if needed)
The current configuration already supports SonarCloud. Verify the sonar configuration in `.github/workflows/cicd-pipeline.yml`:

```yaml
sonarqube:
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
  steps:
    - name: SonarQube Scan
      run: |
        mvn clean verify sonar:sonar \
          -Dsonar.projectKey=team1-ad-project \
          -Dsonar.organization=team1-ad-project \
          -Dsonar.host.url=${{ env.SONAR_HOST_URL }} \
          -Dsonar.login=${{ env.SONAR_TOKEN }}
```

**Important**: Add `-Dsonar.organization=YOUR_ORG_KEY` if using SonarCloud.

---

## Option 2: Self-Hosted SonarQube Server

### Step 1: Deploy SonarQube Server
You can deploy SonarQube using Docker:

```bash
docker run -d --name sonarqube \
  -p 9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  sonarqube:lts-community
```

Or use docker-compose:

```yaml
version: "3"
services:
  sonarqube:
    image: sonarqube:lts-community
    ports:
      - "9000:9000"
    environment:
      - SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_logs:/opt/sonarqube/logs
      - sonarqube_extensions:/opt/sonarqube/extensions

volumes:
  sonarqube_data:
  sonarqube_logs:
  sonarqube_extensions:
```

### Step 2: Access SonarQube
1. Open browser and go to `http://your-server:9000`
2. Default credentials:
   - **Username**: `admin`
   - **Password**: `admin`
3. Change password when prompted

### Step 3: Create Project in SonarQube
1. Click **"Create Project"** → **"Manually"**
2. **Project key**: `team1-ad-project`
3. **Display name**: `EcoGo`
4. Click **"Set Up"**

### Step 4: Generate Token
1. Choose **"With GitHub Actions"**
2. Click **"Generate a token"**
3. **Token name**: `EcoGo-GitHub-Actions`
4. **Copy the token**

### Step 5: Add Secrets to GitHub
Same as Option 1, but use your server URL:
- **SONAR_HOST_URL**: `http://your-server:9000` or `https://your-domain.com`
- **SONAR_TOKEN**: Your generated token

---

## Verification

### Check if Secrets are Configured
Run this workflow and check the logs:

```yaml
- name: Check SonarQube Configuration
  run: |
    if [ -z "${{ secrets.SONAR_TOKEN }}" ]; then
      echo "❌ SONAR_TOKEN is not configured"
    else
      echo "✅ SONAR_TOKEN is configured"
    fi
    
    if [ -z "${{ secrets.SONAR_HOST_URL }}" ]; then
      echo "❌ SONAR_HOST_URL is not configured"
    else
      echo "✅ SONAR_HOST_URL is configured: ${{ secrets.SONAR_HOST_URL }}"
    fi
```

### Trigger a Build
1. Push a commit to `feature/cicdfeature` or `main` branch
2. Go to **Actions** tab in GitHub
3. Check the **"SonarQube Analysis"** job
4. If successful, you'll see: "SonarQube scan completed successfully"

### View Results
1. Go to your SonarCloud dashboard (or self-hosted server)
2. Find the **EcoGo** project
3. Review code quality metrics:
   - Bugs
   - Vulnerabilities
   - Code Smells
   - Security Hotspots
   - Code Coverage
   - Duplications

---

## Troubleshooting

### Error: "Not authorized. Please check the properties sonar.login and sonar.password"
**Solution**: Your SONAR_TOKEN is incorrect or expired. Generate a new token.

### Error: "sonar.organization is mandatory"
**Solution**: Add organization parameter when using SonarCloud:
```bash
-Dsonar.organization=your-org-key
```

### Error: "Shallow clone detected"
**Solution**: Already configured in workflow with `fetch-depth: 0`

### SonarQube job is skipped
**Solution**: Check that SONAR_TOKEN secret is set in GitHub. The job has condition:
```yaml
if: env.SONAR_TOKEN != ''
```

### Quality Gate fails
**Solution**: This is expected if code doesn't meet quality standards. Review the report and fix issues.

---

## CI/CD Pipeline Integration

The SonarQube step runs after:
1. ✅ Lint & Code Quality
2. ✅ SAST Analysis
3. ✅ Build Application
4. ✅ Container Security
5. ✅ Code Coverage Gate
6. **→ SonarQube Analysis** (Stage 6)

And before:
7. Deploy to Staging/Production

---

## Best Practices

1. **Set Quality Gates**: Configure quality gates in SonarQube/SonarCloud
2. **Review Regularly**: Check SonarQube dashboard weekly
3. **Fix Critical Issues**: Address bugs and vulnerabilities immediately
4. **Maintain Coverage**: Keep code coverage above 80%
5. **Rotate Tokens**: Regenerate tokens every 90 days

---

## Additional Resources

- SonarCloud Documentation: https://docs.sonarcloud.io
- SonarQube Documentation: https://docs.sonarqube.org
- Maven SonarQube Plugin: https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-maven/
- EcoGo Secrets Template: `.github/SECRETS-TEMPLATE.md`

---

## Quick Setup Commands

### Update the CI/CD pipeline (if using SonarCloud)
Add organization parameter to the sonar:sonar command in `.github/workflows/cicd-pipeline.yml`:

```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=team1-ad-project \
  -Dsonar.organization=team1-ad-project \
  -Dsonar.host.url=${{ env.SONAR_HOST_URL }} \
  -Dsonar.login=${{ env.SONAR_TOKEN }}
```

### Test locally (optional)
```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=team1-ad-project \
  -Dsonar.organization=team1-ad-project \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.login=YOUR_TOKEN
```

---

## Summary Checklist

- [ ] Create SonarCloud account (or deploy SonarQube server)
- [ ] Import/Create EcoGo project in SonarQube
- [ ] Generate authentication token
- [ ] Add SONAR_TOKEN to GitHub Secrets
- [ ] Add SONAR_HOST_URL to GitHub Secrets
- [ ] Update CI/CD pipeline with organization key (if using SonarCloud)
- [ ] Trigger a build and verify SonarQube analysis runs
- [ ] Check SonarQube dashboard for results
- [ ] Configure quality gates (optional)
- [ ] Set up notifications (optional)

Once completed, every push to your repository will automatically trigger SonarQube analysis! 🎉
