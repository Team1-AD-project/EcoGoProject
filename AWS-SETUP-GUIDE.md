# AWS Setup Guide for EcoGo CI/CD Deployment

## 🎯 概述

完整的CI/CD流程需要AWS credentials来部署应用。本指南说明如何获取和配置AWS credentials。

## 📋 需要配置的GitHub Secrets

| Secret名称 | 说明 | 示例 |
|-----------|------|------|
| `AWS_ACCESS_KEY_ID` | AWS访问密钥ID | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS秘密访问密钥 | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `MONGODB_URI` | MongoDB连接字符串 | `mongodb+srv://user:pass@cluster.mongodb.net/EcoGo` |

（可选）
| `SONAR_HOST_URL` | SonarQube主机URL | `https://sonarcloud.io` |
| `SONAR_TOKEN` | SonarQube token | `squ_xxxxx` |

---

## 🔑 步骤1：获取AWS Credentials

### 1a. 登录AWS Console

访问：https://console.aws.amazon.com

使用您的AWS账户登录。

### 1b. 创建或获取IAM用户

1. 进入 **Services** → 搜索 **IAM**
2. 点击 **Users**
3. 选择现有用户或创建新用户：
   - 点击 **Create user**
   - 用户名：`ecogo-ci-cd` 或任何您想要的名称
   - 点击 **Next**

### 1c. 附加必要权限

1. 点击 **Add permissions** → **Attach policies directly**
2. 搜索并选择以下策略：
   - `AmazonECS_FullAccess` - ECS服务权限
   - `AmazonEC2FullAccess` - EC2实例权限
   - `AmazonVPCFullAccess` - VPC和网络权限
   - `CloudWatchLogsFullAccess` - 日志权限
   - `AmazonElasticLoadBalancingFullAccess` - ALB权限
   - `IAMFullAccess` - IAM权限（或至少CreateRole）

3. 点击 **Next** → **Create user**

### 1d. 创建Access Keys

1. 在用户列表中选择刚创建的用户
2. 点击 **Security credentials** 标签
3. 向下滚动到 **Access keys** 部分
4. 点击 **Create access key**
5. 选择用途：**Application running outside AWS**
6. 点击 **Next**
7. （可选）添加描述：`EcoGo CI/CD Pipeline`
8. 点击 **Create access key**

**重要！** 复制并保存：
- **Access Key ID** (AKIAIOSFODNN7EXAMPLE)
- **Secret Access Key** (wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY)

⚠️ **Secret Access Key只会显示一次，务必保存！** 如果丢失，需要重新创建。

---

## 🔒 步骤2：配置GitHub Secrets

### 2a. 访问Repository Settings

1. 进入GitHub仓库：https://github.com/Team1-AD-project/EcoGo
2. 点击 **Settings** 标签
3. 在左侧菜单点击 **Secrets and variables** → **Actions**

### 2b. 创建Secrets

点击 **New repository secret** 并按照以下方式添加：

#### Secret 1: AWS_ACCESS_KEY_ID
```
Name: AWS_ACCESS_KEY_ID
Value: [从AWS复制的Access Key ID]
```
例如：`AKIAIOSFODNN7EXAMPLE`

点击 **Add secret**

#### Secret 2: AWS_SECRET_ACCESS_KEY
```
Name: AWS_SECRET_ACCESS_KEY
Value: [从AWS复制的Secret Access Key]
```
例如：`wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`

点击 **Add secret**

#### Secret 3: MONGODB_URI
```
Name: MONGODB_URI
Value: [您的MongoDB连接字符串]
```

**选项A：MongoDB Atlas**
```
mongodb+srv://username:password@cluster0.mongodb.net/EcoGo
```

获取连接字符串：
1. 访问 https://www.mongodb.com/cloud/atlas
2. 登录您的Atlas账户
3. 进入您的集群
4. 点击 **Connect**
5. 选择 **Drivers**
6. 复制连接字符串

**选项B：自建MongoDB**
```
mongodb://host:27017/EcoGo
```

点击 **Add secret**

#### Secret 4: SONAR_HOST_URL（可选）
```
Name: SONAR_HOST_URL
Value: https://sonarcloud.io
```

#### Secret 5: SONAR_TOKEN（可选）
```
Name: SONAR_TOKEN
Value: [您的SonarQube token]
```

---

## ✅ 验证Secrets配置

配置完成后，您应该在Secrets页面看到：
```
AWS_ACCESS_KEY_ID       [已配置]
AWS_SECRET_ACCESS_KEY   [已配置]
MONGODB_URI             [已配置]
SONAR_HOST_URL          [已配置]（如果添加）
SONAR_TOKEN             [已配置]（如果添加）
```

---

## 🚀 步骤3：重新运行GitHub Actions

1. 访问 GitHub Actions：https://github.com/Team1-AD-project/EcoGo/actions
2. 找到 "EcoGo CI/CD Pipeline" 工作流
3. 选择失败的运行（应该是最新的）
4. 点击 **Re-run all jobs**

或者，推送一个新的commit来触发新的运行：

```bash
cd EcoGo
git commit --allow-empty -m "trigger: Re-run CI/CD with AWS credentials configured"
git push origin feature/cicdfeature
```

---

## 📊 预期的部署流程

配置好AWS credentials后，GitHub Actions应该会：

### ✅ 前阶段（不需要AWS）
1. **Lint** (~2分钟) - Checkstyle代码质量检查
2. **SAST** (~4分钟) - SpotBugs + OWASP Dependency Check
3. **Build** (~4分钟) - Maven构建 + Docker镜像

### 🚀 部署阶段（需要AWS）
4. **SonarQube** (~3分钟) - 代码质量分析（如果配置了token）
5. **Deploy** (~5分钟) - Terraform创建AWS基础设施 + Ansible部署应用
6. **Integration Tests** (~5分钟) - 测试已部署的应用
7. **DAST** (~5分钟) - OWASP ZAP安全扫描
8. **Monitoring** (~3分钟) - 部署Prometheus和Grafana

**总计**：约25-30分钟

---

## 💰 AWS费用估算

部署会创建以下AWS资源：

| 资源 | 估计费用 | 说明 |
|------|---------|------|
| ECS Fargate | $15-20/月 | 如果一直运行 |
| ALB | $16/月 | 应用负载均衡器 |
| VPC/NAT | $5-10/月 | 网络基础设施 |
| 其他（ECR等） | $5/月 | 容器仓库等 |
| **总计** | **$40-50/月** | |

### ⚠️ 测试后删除资源

为避免持续产生费用，测试完成后删除AWS资源：

```bash
cd EcoGo/terraform

# 查看将被删除的资源
terraform plan -destroy

# 删除所有资源
terraform destroy -auto-approve
```

---

## 🐛 常见问题

### Q1：无法创建IAM用户
**A：** 确保您有足够的AWS账户权限。如果您使用的是企业账户，请联系管理员。

### Q2：Terraform创建失败
**A：** 检查：
1. AWS credentials是否正确
2. IAM用户是否有足够权限
3. AWS账户是否有使用额度
4. 检查GitHub Actions日志获取详细错误

### Q3：应用部署成功但无法访问
**A：**
1. 检查ALB是否健康：`aws elbv2 describe-target-health`
2. 检查安全组规则是否允许入站流量
3. 检查CloudWatch日志：`aws logs tail /ecs/ecogo`

### Q4：想保留AWS资源怎么办
**A：** 不运行 `terraform destroy`，但记得关闭应用以减少费用：
```bash
aws ecs update-service --cluster ecogo-cluster --service ecogo-service --desired-count 0
```

---

## 📝 总结

1. ✅ 在AWS IAM中创建用户和access keys
2. ✅ 在GitHub Secrets中配置这些credentials
3. ✅ 重新运行或推送新commit触发workflow
4. ✅ 监控GitHub Actions直到部署完成
5. ✅ 访问部署的应用（URL在Terraform outputs中）
6. ⚠️ 测试完成后运行 `terraform destroy` 删除资源

---

**需要帮助？** 在workflow失败时，检查GitHub Actions日志获取详细错误信息。

