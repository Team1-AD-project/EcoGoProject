# SonarCloud 配置确认

## ✅ 你的 SonarCloud 项目信息

- **Project Name**: Team1-AD project
- **Project Key**: `team1-ad-project`
- **Organization**: `team1-ad-project`
- **Repository**: `Team1-AD-project/EcoGo`

## 📋 配置状态

所有配置文件已更新为正确的项目密钥：`team1-ad-project`

### 已更新的文件：
1. ✅ `sonar-project.properties`
2. ✅ `.github/workflows/cicd-pipeline.yml`
3. ✅ `test-sonar-local.bat`
4. ✅ `test-sonar-local.ps1`

## 🚀 下一步操作

### 选项 1：本地测试（推荐先测试）

#### 使用 PowerShell:
```powershell
# 设置环境变量
$env:SONAR_HOST_URL = "https://sonarcloud.io"
$env:SONAR_TOKEN = "你的SonarCloud Token"

# 运行扫描
.\test-sonar-local.ps1
```

#### 使用 CMD:
```cmd
set SONAR_HOST_URL=https://sonarcloud.io
set SONAR_TOKEN=你的Token

test-sonar-local.bat
```

#### 手动命令:
```bash
mvn clean verify sonar:sonar -X -e \
  -Dsonar.projectKey=team1-ad-project \
  -Dsonar.organization=team1-ad-project \
  -Dsonar.sources=src/main \
  -Dsonar.tests=src/test \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.login=你的Token \
  -Dsonar.verbose=true
```

### 选项 2：通过 GitHub Actions 测试

1. **确保 GitHub Secrets 已配置**:
   - 进入: https://github.com/Team1-AD-project/EcoGo/settings/secrets/actions
   - 检查是否存在:
     - `SONAR_TOKEN`
     - `SONAR_HOST_URL` = `https://sonarcloud.io`

2. **提交并推送**:
```bash
git add .
git commit -m "fix: 更新 SonarCloud 项目密钥为 team1-ad-project"
git push
```

3. **查看运行结果**:
   - GitHub Actions: https://github.com/Team1-AD-project/EcoGo/actions
   - 展开 "SonarQube Scan" 步骤查看详细日志

## 📊 查看扫描结果

成功后访问你的 SonarCloud 项目:
- **项目仪表板**: https://sonarcloud.io/dashboard?id=team1-ad-project
- **项目概览**: https://sonarcloud.io/summary/overall?id=team1-ad-project

## 🔑 如何获取 SonarCloud Token

如果还没有 Token:

1. 登录 https://sonarcloud.io
2. 右上角头像 → **"My Account"**
3. 选择 **"Security"** 标签
4. 在 "Generate Tokens" 部分:
   - Name: `GitHub-Actions-EcoGo`
   - Type: `Global Analysis Token` 或 `Project Analysis Token`
   - 点击 **"Generate"**
5. **立即复制** Token（只显示一次！）

## ⚠️ 常见问题

### Q: 如果本地测试时提示 "Could not find a default branch"？

**可能原因**:
- Token 权限不足
- 项目密钥拼写错误
- 组织名称错误

**解决方法**:
```bash
# 检查你的项目是否存在
# 访问: https://sonarcloud.io/projects
# 确认项目密钥确实是 "team1-ad-project"
```

### Q: GitHub Actions 中如何查看详细日志？

1. 进入 GitHub Actions 运行页面
2. 点击失败的工作流
3. 展开 "SonarQube Scan" 步骤
4. 现在会显示完整的 Maven 调试输出（包含 -X -e 参数）

### Q: 如何验证 Token 是否有效？

```bash
# 使用 curl 测试
curl -u 你的Token: https://sonarcloud.io/api/authentication/validate
```

如果返回 `{"valid":true}`，表示 Token 有效。

## 📝 配置摘要

```properties
# sonar-project.properties
sonar.projectKey=team1-ad-project
sonar.projectName=EcoGo
sonar.organization=team1-ad-project (CI/CD 中自动添加)
```

现在配置应该完全匹配你的 SonarCloud 项目了！🎉
