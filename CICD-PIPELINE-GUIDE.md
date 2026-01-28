# EcoGo CI/CD Pipeline 流程指南

## 📋 目录

- [概述](#概述)
- [触发条件](#触发条件)
- [流程架构](#流程架构)
- [详细阶段说明](#详细阶段说明)
- [分支策略](#分支策略)
- [工具清单](#工具清单)
- [配置要求](#配置要求)
- [常见问题](#常见问题)

---

## 概述

EcoGo CI/CD Pipeline 是一个全自动化的持续集成和持续部署流程，涵盖了从代码质量检查、安全扫描、构建、测试到部署的完整生命周期。该流程采用 GitHub Actions 实现，支持多环境部署（Staging 和 Production）。

### 核心特性

- ✅ **全面的安全扫描**：SAST、容器扫描、DAST
- ✅ **多层次测试**：单元测试、集成测试、冒烟测试、性能测试
- ✅ **代码质量保障**：Checkstyle、SonarQube、代码覆盖率检查
- ✅ **多环境支持**：Staging（自动）和 Production（需批准）
- ✅ **智能容错**：大部分阶段支持 continue-on-error
- ✅ **完整监控**：Prometheus + Grafana 监控栈

---

## 触发条件

Pipeline 会在以下情况自动触发：

```yaml
触发事件：
  - Push 到分支: main, develop, feature/**
  - Pull Request 到分支: main, develop, feature/**
```

---

## 流程架构

### 整体流程图

```
┌─────────────────────────────────────────────────────────────┐
│                         所有分支                             │
│  Stage 1-6: 共同流程 (代码质量、安全、构建、测试)              │
└──────────────────────┬──────────────────────────────────────┘
                       │
            ┌──────────┴──────────┐
            │                     │
    ┌───────▼──────┐      ┌──────▼────────┐
    │ Feature/     │      │    Main       │
    │ Develop      │      │   分支        │
    │ 分支         │      │               │
    └───────┬──────┘      └──────┬────────┘
            │                    │
    ┌───────▼──────────┐  ┌─────▼──────────┐
    │ Staging 环境     │  │ Production     │
    │ Stage 7-11       │  │ 环境           │
    │                  │  │ Stage 7-9      │
    │ - Deploy Staging │  │                │
    │ - Integration    │  │ - Deploy Prod  │
    │ - Smoke Tests    │  │   (需批准)     │
    │ - Performance    │  │ - Smoke Tests  │
    │ - DAST           │  │ - Monitoring   │
    │ - Monitoring     │  │                │
    └──────────────────┘  └────────────────┘
```

---

## 详细阶段说明

### Stage 1: Lint & Code Quality

**执行条件**: 所有分支

**目的**: 确保代码符合编码规范和风格标准

**工具**: Maven Checkstyle

**执行内容**:
- 运行 Checkstyle 代码风格检查
- 生成 Checkstyle 报告
- 检查代码格式、命名规范、注释等

**失败影响**: 不阻断流程 (continue-on-error: true)

---

### Stage 2: SAST Analysis (静态应用安全测试)

**执行条件**: 所有分支

**目的**: 在代码编译阶段发现安全漏洞和代码缺陷

**工具**: 
- SpotBugs - Java 代码静态分析
- OWASP Dependency Check - 依赖项漏洞扫描

**执行内容**:
1. 编译应用程序
2. 运行 SpotBugs 检查常见代码漏洞
3. 运行 OWASP Dependency Check 扫描第三方依赖
4. 上传 SAST 报告到 Artifacts

**产出物**:
- `target/spotbugsXml.xml` - SpotBugs 报告
- `target/dependency-check-report.html` - OWASP 报告

**失败影响**: 不阻断流程

---

### Stage 3: Build Application (构建应用)

**执行条件**: 所有分支

**目的**: 编译和打包应用程序，构建 Docker 镜像

**工具**: 
- Maven
- Docker

**执行内容**:
1. 使用 Maven 打包应用 (`mvn clean package`)
2. 生成 JAR 文件
3. 构建 Docker 镜像（两个版本）
   - `${IMAGE_NAME}:${COMMIT_SHA}` - 特定版本
   - `${IMAGE_NAME}:latest` - 最新版本
4. 上传构建产物

**产出物**:
- `build-artifacts/EcoGo-*.jar` - 应用 JAR 包
- Docker 镜像

**失败影响**: 阻断流程（后续阶段依赖构建产物）

---

### Stage 4: Container Security Scan (容器安全扫描)

**执行条件**: 所有分支

**目的**: 扫描 Docker 镜像中的安全漏洞

**工具**: Trivy

**执行内容**:
1. 构建 Docker 镜像用于扫描
2. 运行 Trivy 扫描（CRITICAL 和 HIGH 级别漏洞）
3. 生成 SARIF 格式报告
4. 上传到 GitHub Security（需要 security-events 权限）
5. 生成表格格式报告

**产出物**:
- `trivy-results.sarif` - SARIF 格式报告
- GitHub Security 告警

**失败影响**: 不阻断流程

---

### Stage 5: Code Coverage Gate (代码覆盖率检查)

**执行条件**: 所有分支

**目的**: 确保代码有足够的测试覆盖率

**工具**: JaCoCo

**执行内容**:
1. 启动 MongoDB 服务
2. 运行单元测试和集成测试 (`mvn clean verify`)
3. 生成 JaCoCo 覆盖率报告
4. 检查覆盖率阈值：
   - 行覆盖率: 70%
   - 分支覆盖率: 60%
5. 上传覆盖率报告

**产出物**:
- `target/site/jacoco/` - HTML 覆盖率报告

**失败影响**: 不阻断流程（提供信息性反馈）

---

### Stage 6: SonarQube Analysis (代码质量分析)

**执行条件**: 所有分支（需要配置 SONAR_TOKEN）

**目的**: 深度代码质量分析和技术债务评估

**工具**: SonarQube

**执行内容**:
1. 运行 SonarQube 扫描
2. 分析：
   - 代码异味（Code Smells）
   - 代码重复
   - 复杂度
   - 安全热点
   - 技术债务
3. 上传结果到 SonarQube 服务器

**失败影响**: 不阻断流程

**可选**: 如果未配置 SONAR_TOKEN，跳过此步骤

---

## Feature/Develop 分支专属流程

### Stage 7: Deploy to Staging (部署到测试环境)

**执行条件**: `develop` 或 `feature/**` 分支

**目的**: 将应用部署到 Staging 测试环境

**工具**: 
- AWS CLI
- Terraform (可选)
- Ansible (可选)

**执行内容**:
1. 下载构建产物
2. 检查 AWS 凭证是否配置
3. 如果配置了 AWS:
   - 配置 AWS 凭证
   - 使用 Terraform/Ansible 部署到 AWS ECS
4. 如果未配置:
   - 显示警告信息
   - 继续后续测试流程

**环境**: `staging`

**失败影响**: 不阻断流程

---

### Stage 8a: Integration Tests (集成测试)

**执行条件**: `develop` 或 `feature/**` 分支

**目的**: 测试各组件之间的集成

**工具**: Maven + MongoDB

**执行内容**:
1. 启动 MongoDB 服务
2. 运行集成测试 (`mvn clean verify`)
3. 测试数据库集成、API 交互等

**失败影响**: 不阻断流程

---

### Stage 8b: Smoke Tests - Staging (冒烟测试)

**执行条件**: `develop` 或 `feature/**` 分支

**目的**: 快速验证应用的关键功能是否正常

**工具**: Bash 脚本 + curl

**执行内容**:

**智能测试逻辑**:

1. **检查 STAGING_URL**:
   - 如果配置了 `STAGING_URL`:
     - 等待部署完成（30秒）
     - 测试真实部署环境
   
   - 如果未配置:
     - 下载构建产物
     - 启动本地 MongoDB
     - 启动本地应用
     - 测试本地环境

2. **运行冒烟测试**:
   - 健康检查: `/actuator/health`
   - 信息端点: `/actuator/info`
   - 监控指标: `/actuator/metrics`

**测试脚本**: `.github/scripts/smoke-tests.sh`

**失败影响**: 不阻断流程

---

### Stage 9: Performance Tests (性能测试)

**执行条件**: `develop` 或 `feature/**` 分支

**目的**: 评估应用性能和负载能力

**工具**: JMeter

**执行内容**:
1. 启动应用 + MongoDB
2. 运行 JMeter 负载测试
3. 测试场景：
   - 并发用户访问
   - API 响应时间
   - 吞吐量
4. 生成性能报告
5. 检查性能基准

**测试计划**: `performance-tests/load-test.jmx`

**产出物**:
- `performance-tests/results.jtl` - 测试结果
- `performance-tests/report/` - HTML 报告

**失败影响**: 不阻断流程

---

### Stage 10: DAST (动态应用安全测试)

**执行条件**: `develop` 或 `feature/**` 分支

**目的**: 在运行时发现安全漏洞

**工具**: OWASP ZAP

**执行内容**:
1. 启动应用 + MongoDB
2. 等待应用就绪
3. 运行 ZAP 基线扫描:
   - 快速安全扫描
   - 常见漏洞检测
4. 运行 ZAP 完整扫描:
   - 深度安全分析
   - 使用自定义配置 (`.zap/zap-config.yaml`)
5. 生成多种格式报告:
   - HTML 报告
   - Markdown 报告
   - JSON 报告
   - XML 报告

**配置文件**: `.zap/zap-config.yaml`, `.zap/rules.tsv`

**产出物**:
- `zap-report.html` - 主报告
- `zap-report.md` - Markdown 版本
- `zap-report.json` - JSON 数据
- `zap-report.xml` - XML 数据

**失败影响**: 不阻断流程

---

### Stage 11: Monitoring Setup (监控部署)

**执行条件**: `develop` 或 `feature/**` 分支

**目的**: 部署监控栈，收集应用指标

**工具**: 
- Prometheus - 指标收集
- Grafana - 可视化

**执行内容**:
1. 部署 Prometheus:
   - 端口: 9090
   - 配置: `monitoring/prometheus.yml`
2. 部署 Grafana:
   - 端口: 3000
   - 默认密码: admin
3. 配置 Grafana 数据源（连接 Prometheus）

**访问地址**:
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

**失败影响**: 不阻断流程

---

## Main 分支专属流程 (生产环境)

### Stage 7: Deploy to Production (部署到生产)

**执行条件**: `main` 分支

**目的**: 将应用部署到生产环境

**工具**: 
- AWS CLI
- Terraform
- Ansible

**执行内容**:

⚠️ **重要特性: 需要手动批准**

1. **等待手动批准**:
   - GitHub Environment 保护规则
   - 需要授权人员在 GitHub 界面批准
   
2. **批准后执行**:
   - 下载构建产物
   - 检查 AWS 凭证
   - 配置 AWS 凭证（us-east-1）
   - 部署到 AWS ECS Production
   
3. **如果未配置 AWS**:
   - 显示警告
   - 跳过部署

**环境**: `production`

**失败影响**: 不阻断流程

**批准流程**:
```
1. Push 到 main 分支
2. Pipeline 运行到 Deploy to Production
3. 暂停等待批准
4. 管理员在 GitHub → Actions → 当前 Workflow 中点击 "Review deployments"
5. 选择 "production" 环境并批准
6. 继续部署
```

---

### Stage 8: Smoke Tests - Production (生产冒烟测试)

**执行条件**: `main` 分支

**目的**: 验证生产环境部署成功

**工具**: Bash 脚本 + curl

**执行内容**:

**智能测试逻辑**:

1. **检查 PRODUCTION_URL**:
   - 如果配置了:
     - 等待部署完成（30秒）
     - 运行冒烟测试脚本
     - 测试关键 API 端点
   
   - 如果未配置:
     - 跳过测试
     - 显示配置提示

2. **测试端点**:
   - `/actuator/health` - 健康检查
   - 其他关键业务端点

**失败影响**: 不阻断流程

---

### Stage 9: Monitoring Setup - Production (生产监控)

**执行条件**: `main` 分支

**目的**: 配置生产环境监控

**工具**: Prometheus + Grafana

**执行内容**:
- 部署生产级监控配置
- 可以在此配置告警规则
- 集成到现有监控系统

**失败影响**: 不阻断流程

---

## Pipeline Status (流程状态报告)

**执行条件**: 所有分支，总是运行

**目的**: 生成流程执行摘要

**执行内容**:
- 显示工作流名称
- 显示提交 SHA
- 显示分支信息
- 汇总执行结果

---

## 分支策略

### 推荐的 Git 工作流

```
┌─────────────────────────────────────────────────────────┐
│                     开发流程                             │
└─────────────────────────────────────────────────────────┘

1. 功能开发
   feature/xxx 分支
   ↓
   - 开发新功能
   - 本地测试
   - Push 触发 CI/CD
   - 执行完整 Staging 流程
   
2. 集成测试
   develop 分支 (可选)
   ↓
   - 合并多个 feature
   - 集成测试
   - 再次验证
   
3. 生产发布
   main 分支
   ↓
   - 创建 PR: develop → main
   - Code Review
   - 合并后触发 Production 流程
   - 等待手动批准
   - 部署到生产
```

### 分支执行对比

| 阶段 | Feature/Develop | Main |
|-----|----------------|------|
| Lint | ✅ | ✅ |
| SAST | ✅ | ✅ |
| Build | ✅ | ✅ |
| Container Security | ✅ | ✅ |
| Coverage Check | ✅ | ✅ |
| SonarQube | ✅ | ✅ |
| **Deploy Staging** | ✅ | ❌ |
| **Integration Tests** | ✅ | ❌ |
| **Smoke Tests Staging** | ✅ | ❌ |
| **Performance Tests** | ✅ | ❌ |
| **DAST** | ✅ | ❌ |
| **Monitoring Setup** | ✅ | ❌ |
| **Deploy Production** | ❌ | ✅ |
| **Smoke Tests Production** | ❌ | ✅ |
| **Monitoring Production** | ❌ | ✅ |

---

## 工具清单

### 代码质量工具
| 工具 | 用途 | 阶段 |
|-----|------|------|
| Maven Checkstyle | 代码风格检查 | Stage 1 |
| SonarQube | 代码质量分析 | Stage 6 |

### 安全扫描工具
| 工具 | 用途 | 阶段 |
|-----|------|------|
| SpotBugs | Java 静态分析 | Stage 2 |
| OWASP Dependency Check | 依赖漏洞扫描 | Stage 2 |
| Trivy | 容器漏洞扫描 | Stage 4 |
| OWASP ZAP | 动态安全测试 | Stage 10 |

### 测试工具
| 工具 | 用途 | 阶段 |
|-----|------|------|
| JaCoCo | 代码覆盖率 | Stage 5 |
| Maven | 单元/集成测试 | Stage 5, 8a |
| JMeter | 性能测试 | Stage 9 |
| curl | API 测试 | Stage 8b |

### 构建和部署工具
| 工具 | 用途 | 阶段 |
|-----|------|------|
| Maven | Java 构建 | Stage 3 |
| Docker | 容器化 | Stage 3, 4 |
| AWS CLI | AWS 部署 | Stage 7 |
| Terraform | 基础设施 | Stage 7 |
| Ansible | 配置管理 | Stage 7 |

### 监控工具
| 工具 | 用途 | 阶段 |
|-----|------|------|
| Prometheus | 指标收集 | Stage 11 |
| Grafana | 可视化 | Stage 11 |

### 基础设施
| 工具 | 用途 | 使用场景 |
|-----|------|---------|
| MongoDB | 数据库 | 测试阶段 |
| AWS ECS | 容器服务 | 部署 |
| GitHub Actions | CI/CD 平台 | 整个流程 |

---

## 配置要求

### GitHub Secrets 配置

在 `Settings → Secrets and variables → Actions` 中配置：

#### 必需配置（用于 AWS 部署）
```
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
```

#### 可选配置（增强功能）
```
SONAR_HOST_URL=https://sonarqube.example.com
SONAR_TOKEN=your-sonar-token
STAGING_URL=https://staging.example.com
PRODUCTION_URL=https://production.example.com
```

### GitHub Environments 配置

#### 1. 创建 Environments

在 `Settings → Environments` 中创建：

**Staging Environment:**
- 名称: `staging`
- 保护规则: 无（自动部署）

**Production Environment:**
- 名称: `production`
- 保护规则: 
  - ✅ Required reviewers（必需审批人）
  - 添加审批人员
  - 可选：等待计时器

#### 2. Environment Secrets（可选）

可以为每个环境配置特定的 secrets：
- 在 `staging` 环境: 配置 staging 特定的变量
- 在 `production` 环境: 配置 production 特定的变量

### 本地文件配置

#### 1. Terraform 配置

创建 `terraform/terraform.tfvars`:
```hcl
aws_region = "us-east-1"
ecr_repository_url = "your-ecr-url"
mongodb_uri = "your-mongodb-uri"
```

#### 2. Ansible 配置

编辑 `ansible/inventory.ini`:
```ini
[staging]
staging-server-1 ansible_host=YOUR_STAGING_IP

[production]
prod-server-1 ansible_host=YOUR_PRODUCTION_IP
```

#### 3. OWASP ZAP 配置

确保存在：
- `.zap/zap-config.yaml` - ZAP 扫描配置
- `.zap/rules.tsv` - 自定义规则

---

## 常见问题

### Q1: 为什么 Production 部署没有执行？

**答**: Production 部署只在 `main` 分支执行。如果您在 `feature/*` 或 `develop` 分支，只会执行 Staging 流程。

**解决方案**:
1. 合并代码到 `main` 分支
2. 或者在 `main` 分支直接 push

---

### Q2: 如何批准 Production 部署？

**答**: Production 部署需要手动批准。

**步骤**:
1. 访问 GitHub 仓库
2. 进入 `Actions` 标签
3. 选择正在运行的 Workflow
4. 点击 "Review deployments"
5. 选择 `production` 环境
6. 点击 "Approve and deploy"

---

### Q3: 没有 AWS 账号可以运行吗？

**答**: 可以！流程会自动检测 AWS 配置。

**未配置 AWS 时的行为**:
- Deploy 阶段: 显示警告，继续流程
- Smoke Tests: 自动启动本地应用进行测试
- 其他测试: 正常运行

---

### Q4: 某个阶段失败了怎么办？

**答**: 大部分阶段设置了 `continue-on-error: true`，不会阻断流程。

**需要关注的失败**:
- Build 失败: 会阻断后续流程（必须修复）
- 测试失败: 查看报告，修复问题，重新提交

**查看报告**:
1. 进入 GitHub Actions
2. 选择失败的 Workflow
3. 下载 Artifacts：
   - `sast-reports` - 安全报告
   - `coverage-reports` - 覆盖率报告
   - `jmeter-report` - 性能报告
   - `dast-report` - DAST 报告

---

### Q5: 如何查看生成的报告？

**答**: 所有报告作为 Artifacts 上传到 GitHub。

**下载步骤**:
1. 进入 GitHub Actions
2. 选择对应的 Workflow 运行
3. 滚动到底部 "Artifacts" 区域
4. 点击下载对应报告

**可用报告**:
- `sast-reports` - SpotBugs, OWASP 依赖检查
- `coverage-reports` - JaCoCo 覆盖率
- `jmeter-report` - 性能测试
- `dast-report` - OWASP ZAP 安全扫描

---

### Q6: Coverage 不达标怎么办？

**答**: 覆盖率检查不会阻断流程，但应该努力提高。

**目标阈值**:
- 行覆盖率: 70%
- 分支覆盖率: 60%

**改进建议**:
1. 为关键业务逻辑添加单元测试
2. 补充边界条件测试
3. 增加集成测试覆盖
4. 查看 JaCoCo 报告，找到未覆盖的代码

---

### Q7: SonarQube 扫描失败？

**答**: 如果未配置 `SONAR_TOKEN`，会自动跳过。

**配置 SonarQube**:
1. 注册 SonarQube 账号（SonarCloud 或自建）
2. 创建项目并获取 Token
3. 在 GitHub Secrets 中配置:
   ```
   SONAR_HOST_URL=https://sonarcloud.io
   SONAR_TOKEN=your-token
   ```

---

### Q8: 如何加速 Pipeline 执行？

**答**: Pipeline 已经进行了优化，但可以进一步改进：

**优化建议**:
1. **Maven 缓存**: 已启用 (`cache: maven`)
2. **并行执行**: Integration Tests 和 Smoke Tests 可以并行
3. **跳过非关键测试**: 
   - 在开发阶段可以临时禁用性能测试
   - 使用 `[skip ci]` 跳过文档更新的 CI

**典型执行时间**:
- Staging 流程: 15-25 分钟
- Production 流程: 5-10 分钟

---

### Q9: DAST 扫描时间太长？

**答**: OWASP ZAP 完整扫描可能需要较长时间。

**优化方案**:
1. 使用 Baseline 扫描（快速）
2. 配置扫描规则 (`.zap/rules.tsv`)
3. 限制扫描范围
4. 只在合并到 develop 时运行完整扫描

---

### Q10: 如何本地测试 Pipeline？

**答**: 可以使用 `act` 工具本地运行 GitHub Actions。

**安装 act**:
```bash
# macOS
brew install act

# Windows
choco install act-cli

# Linux
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash
```

**运行测试**:
```bash
# 测试整个 workflow
act push

# 测试特定 job
act -j build

# 使用 secrets
act -s AWS_ACCESS_KEY_ID=xxx -s AWS_SECRET_ACCESS_KEY=yyy
```

---

## 总结

EcoGo CI/CD Pipeline 提供了一个完整、自动化、安全的软件交付流程。通过分支策略和环境隔离，确保了代码质量和部署安全。

### 关键要点

✅ **自动化**: 代码提交自动触发完整流程
✅ **安全**: 多层次安全扫描（SAST、容器、DAST）
✅ **质量**: 代码质量和覆盖率检查
✅ **测试**: 单元、集成、性能、冒烟测试
✅ **保护**: Production 需要手动批准
✅ **监控**: 内置 Prometheus + Grafana
✅ **灵活**: 支持有/无 AWS 环境运行

### 下一步

1. ✅ 配置 GitHub Secrets
2. ✅ 设置 GitHub Environments
3. ✅ 推送代码触发 Pipeline
4. ✅ 查看报告和结果
5. ✅ 持续改进测试覆盖率

---

**文档版本**: 1.0  
**最后更新**: 2026-01-28  
**维护者**: EcoGo Team
