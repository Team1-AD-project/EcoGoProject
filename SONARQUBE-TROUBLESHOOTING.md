# SonarQube 扫描问题排查指南

## ❌ 错误信息

```
Failed to execute goal org.sonarsource.scanner.maven:sonar-maven-plugin:3.10.0.2594:sonar
Could not find a default branch for project with key 'EcoGo'
```

## 🔍 问题原因

这个错误表示 SonarQube/SonarCloud 服务器上没有找到项目 `EcoGo`。可能的原因:

1. **项目未创建**: 在 SonarQube/SonarCloud 平台上还没有创建该项目
2. **项目密钥不匹配**: 配置的项目密钥与服务器上的不一致
3. **认证问题**: Token 没有足够的权限或已过期
4. **组织配置错误** (仅 SonarCloud): 组织名称配置不正确

## 📋 查看详细日志的方法

### 方法 1: 本地运行 (推荐用于调试)

#### 使用 PowerShell:

```powershell
# 1. 设置环境变量
$env:SONAR_HOST_URL = "https://sonarcloud.io"  # 或你的 SonarQube 服务器地址
$env:SONAR_TOKEN = "你的Token"

# 2. 运行测试脚本
.\test-sonar-local.ps1
```

#### 使用命令提示符 (CMD):

```cmd
# 1. 设置环境变量
set SONAR_HOST_URL=https://sonarcloud.io
set SONAR_TOKEN=你的Token

# 2. 运行测试脚本
test-sonar-local.bat
```

#### 手动运行完整命令:

```bash
mvn clean verify sonar:sonar -X -e \
  -Dsonar.projectKey=EcoGo \
  -Dsonar.sources=src/main \
  -Dsonar.tests=src/test \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.login=你的Token \
  -Dsonar.verbose=true
```

**参数说明:**
- `-X`: Maven 调试模式,显示详细的执行信息
- `-e`: 显示完整的错误堆栈跟踪
- `-Dsonar.verbose=true`: SonarQube 详细日志模式

### 方法 2: GitHub Actions 中查看

我已经更新了 `.github/workflows/cicd-pipeline.yml` 文件,添加了 `-X -e` 参数。
下次 GitHub Actions 运行时,会自动显示详细日志。

在 GitHub Actions 中查看:
1. 进入 GitHub 仓库
2. 点击 "Actions" 标签
3. 选择失败的工作流运行
4. 展开 "SonarQube Scan" 步骤查看详细日志

## ✅ 解决方案

### 步骤 1: 检查 SonarQube 配置

运行配置检查脚本:

```bash
# Linux/Mac
chmod +x .github/scripts/check-sonar-config.sh
./.github/scripts/check-sonar-config.sh

# Windows (Git Bash)
bash .github/scripts/check-sonar-config.sh
```

### 步骤 2: 在 SonarCloud 上创建项目

如果使用 **SonarCloud**:

1. 访问 https://sonarcloud.io
2. 使用 GitHub 账号登录
3. 点击 "+" → "Analyze new project"
4. 选择你的 GitHub 仓库
5. 设置项目密钥为 `EcoGo` (与配置文件一致)
6. 记下组织名称 (organization)

如果使用 **自托管 SonarQube**:

1. 访问你的 SonarQube 服务器
2. 登录管理员账号
3. 点击 "Create new project"
4. 输入项目密钥: `EcoGo`
5. 输入项目名称: `EcoGo`

### 步骤 3: 生成认证 Token

**SonarCloud:**
1. 点击右上角头像 → "My Account"
2. 选择 "Security" 标签
3. 生成新 Token,名称如 "GitHub Actions"
4. **立即复制 Token** (只显示一次)

**自托管 SonarQube:**
1. 进入 "My Account" → "Security"
2. 创建新 Token
3. 复制 Token

### 步骤 4: 配置 GitHub Secrets

1. 进入 GitHub 仓库 → Settings → Secrets and variables → Actions
2. 添加以下 secrets:

   **对于 SonarCloud:**
   - `SONAR_TOKEN`: 你的 SonarCloud Token
   - `SONAR_HOST_URL`: `https://sonarcloud.io`

   **对于自托管 SonarQube:**
   - `SONAR_TOKEN`: 你的 SonarQube Token
   - `SONAR_HOST_URL`: 你的 SonarQube 服务器地址 (如 `https://sonar.example.com`)

### 步骤 5: 验证配置

#### 本地验证:

```powershell
# PowerShell
$env:SONAR_HOST_URL = "https://sonarcloud.io"
$env:SONAR_TOKEN = "你的Token"
.\test-sonar-local.ps1
```

#### GitHub Actions 验证:

```bash
# 提交并推送更改
git add .
git commit -m "test: 验证 SonarQube 配置"
git push
```

然后在 GitHub Actions 中查看运行结果。

## 🔧 常见问题

### Q1: 如何检查项目是否已创建?

**SonarCloud:**
- 访问 https://sonarcloud.io/projects
- 查找项目密钥 `EcoGo`

**自托管 SonarQube:**
- 访问你的 SonarQube 服务器
- 进入 "Projects" 页面
- 搜索 `ecogo`

### Q2: 组织名称在哪里找?

**SonarCloud:**
- 登录 SonarCloud
- 右上角头像 → "My Organizations"
- 查看组织的 Key (通常显示在 URL 中)

当前配置的组织名称是: `team1-ad-project`

### Q3: Token 权限不足怎么办?

确保 Token 具有以下权限:
- **SonarCloud**: "Execute Analysis" 权限
- **SonarQube**: "Execute Analysis" 和 "Browse" 权限

如果权限不足,请重新生成一个新的 Token。

### Q4: 日志中显示连接超时?

检查:
- 网络连接是否正常
- `SONAR_HOST_URL` 是否正确
- 防火墙/代理设置

### Q5: 如何更改项目密钥?

如果需要更改项目密钥,需要同时修改:

1. `sonar-project.properties`:
   ```properties
   sonar.projectKey=新的密钥
   ```

2. `.github/workflows/cicd-pipeline.yml`:
   ```yaml
   -Dsonar.projectKey=新的密钥
   ```

3. SonarQube/SonarCloud 上创建对应的项目

## 📊 查看扫描结果

扫描成功后,可以在以下位置查看结果:

**SonarCloud:**
- https://sonarcloud.io/dashboard?id=EcoGo

**自托管 SonarQube:**
- https://你的服务器地址/dashboard?id=EcoGo

## 🆘 需要更多帮助?

如果问题仍未解决:

1. 运行本地测试脚本并保存完整日志
2. 检查 GitHub Actions 中的详细日志
3. 查看 SonarQube 文档: https://docs.sonarqube.org/latest/
4. 查看 SonarCloud 文档: https://docs.sonarcloud.io/

## 📝 相关文件

- `sonar-project.properties` - SonarQube 项目配置
- `.github/workflows/cicd-pipeline.yml` - CI/CD 流水线配置
- `.github/scripts/check-sonar-config.sh` - 配置检查脚本
- `test-sonar-local.ps1` - PowerShell 本地测试脚本
- `test-sonar-local.bat` - CMD 本地测试脚本
