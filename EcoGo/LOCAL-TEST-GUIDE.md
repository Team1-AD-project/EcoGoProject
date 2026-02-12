# EcoGo 本地完整测试指南

## 📋 完整测试步骤

### 第1步：确保在Feature分支

```bash
cd EcoGo
git checkout feature/cicdfeature
git status
```

### 第2步：运行完整本地测试

```bash
# 运行测试脚本（交互式菜单会出现，选择选项5）
./scripts/test-cicd.sh
```

**或者按步骤手动测试：**

### 第3步：快速环境验证（可选，2分钟）

```bash
./scripts/verify-deployment.sh
```

### 第4步：分步手动测试

#### 4a. 测试代码质量（LINT）- 2分钟
```bash
mvn checkstyle:check
```

#### 4b. 测试SAST扫描 - 5分钟
```bash
# 编译
mvn clean compile -DskipTests

# SpotBugs
mvn spotbugs:spotbugs -DskipTests

# 依赖检查
mvn dependency-check:check -DskipTests

# 查看报告
# - SpotBugs: target/spotbugsXml.xml
# - Dependency Check: target/dependency-check-report.html
```

#### 4c. 测试应用构建 - 5分钟
```bash
mvn clean package -DskipTests
```

#### 4d. 测试Docker镜像构建 - 3分钟
```bash
docker build -t ecogo:test .
docker images ecogo:test
```

#### 4e. 启动监控栈 - 1分钟
```bash
cd monitoring
docker-compose up -d

# 等待服务启动
sleep 15

# 检查状态
docker ps | grep ecogo
cd ..
```

#### 4f. 启动应用 - 1分钟
```bash
docker run -d \
  --name ecogo-test \
  --network monitoring_ecogo-monitoring \
  -p 8091:8090 \
  -e SPRING_DATA_MONGODB_URI=mongodb://ecogo-mongodb:27017/EcoGo \
  -e SPRING_PROFILES_ACTIVE=test \
  ecogo:test

# 等待应用启动
sleep 20
```

#### 4g. 测试API端点 - 1分钟
```bash
# 健康检查
curl http://localhost:8091/actuator/health

# 信息端点
curl http://localhost:8091/actuator/info

# Prometheus指标
curl http://localhost:8091/actuator/prometheus | head -20
```

#### 4h. 查看监控系统
```bash
# Prometheus
curl http://localhost:9090/-/healthy

# Grafana
curl http://localhost:3000/api/health

# MongoDB
docker exec ecogo-mongodb mongosh --eval "db.adminCommand('ping')"
```

### 第5步：清理资源

```bash
# 停止应用
docker stop ecogo-test
docker rm ecogo-test

# 清理镜像
docker rmi ecogo:test

# 可选：停止监控栈
cd monitoring
docker-compose down
cd ..
```

---

## 🌐 访问地址

测试完成后，可以访问：

### 应用端点
| 端点 | 地址 | 说明 |
|------|------|------|
| 健康检查 | http://localhost:8091/actuator/health | 应用健康状态 |
| 应用信息 | http://localhost:8091/actuator/info | 应用版本和信息 |
| Prometheus指标 | http://localhost:8091/actuator/prometheus | 监控指标 |

### 监控系统
| 系统 | 地址 | 凭证 |
|------|------|------|
| Prometheus | http://localhost:9090 | 无需认证 |
| Grafana | http://localhost:3000 | admin/admin |
| MongoDB | mongodb://localhost:27017 | 无需认证 |

---

## ✅ 测试检查清单

完整测试应该：

- [ ] 环境验证通过
- [ ] Checkstyle通过（或有可接受的警告）
- [ ] 应用成功构建
- [ ] Docker镜像成功构建
- [ ] MongoDB容器运行
- [ ] Prometheus容器运行
- [ ] Grafana容器运行
- [ ] 应用容器启动成功
- [ ] 健康检查端点返回UP状态
- [ ] Prometheus端点返回指标数据
- [ ] Grafana可访问

---

## 🐛 故障排查

### MongoDB连接失败
```bash
# 检查MongoDB容器
docker logs ecogo-mongodb

# 检查MongoDB是否响应
docker exec ecogo-mongodb mongosh --eval "db.adminCommand('ping')"
```

### 应用启动失败
```bash
# 查看应用日志
docker logs -f ecogo-test

# 检查MongoDB URI是否正确
echo $MONGODB_URI
```

### Docker构建失败
```bash
# 清理旧镜像
docker rmi ecogo:test

# 重新构建并查看详细输出
docker build -t ecogo:test . --progress=plain
```

### 端口被占用
```bash
# 查找占用端口的进程
lsof -i :8091  # 应用
lsof -i :9090  # Prometheus
lsof -i :3000  # Grafana
lsof -i :27017 # MongoDB

# 杀死进程（如果需要）
kill -9 <PID>
```

---

## 📊 预期输出示例

### 健康检查成功
```json
{
  "status": "UP",
  "components": {
    "mongodb": {
      "status": "UP"
    }
  }
}
```

### Prometheus指标示例
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="PS Survivor Space"} 1.0E7
http_requests_total{method="GET",status="200"} 42
```

---

## ⏱️ 时间估计

| 步骤 | 时间 |
|------|------|
| 代码质量检查 | 2分钟 |
| SAST扫描 | 5分钟 |
| 应用构建 | 5分钟 |
| Docker构建 | 3分钟 |
| 监控栈启动 | 1分钟 |
| 应用启动 | 2分钟 |
| API测试 | 1分钟 |
| **总计** | **~20分钟** |

---

## 🎉 成功标志

当所有测试完成后，您应该看到：

```
✓ PASS: Java installed
✓ PASS: Maven installed
✓ PASS: Docker installed
✓ PASS: Docker daemon running
✓ PASS: .github/workflows/cicd-pipeline.yml 存在
✓ PASS: Dockerfile 存在
✓ PASS: Checkstyle检查通过
✓ PASS: 应用构建成功
✓ PASS: Docker镜像构建成功
✓ PASS: MongoDB运行中
✓ PASS: Prometheus运行中
✓ PASS: Grafana运行中
✓ PASS: 应用健康状态: UP
✓ PASS: Prometheus端点可访问
✓ PASS: Info端点可访问

========================================
通过: 15
警告: 0
失败: 0
========================================
✓ 所有关键测试通过！
✓ 可以安全地推送到GitHub
```

---

## 🚀 下一步

完整测试通过后：

1. **确认workflow配置正确**
   - GitHub Actions应该通过LINT、SAST、Build步骤
   - 完全可以推送到main分支

2. **合并到main分支**
   ```bash
   git checkout main
   git merge feature/cicdfeature
   git push origin main
   ```

3. **在main分支上运行完整CI/CD**
   - 部署到AWS（需要配置secrets）
   - 运行Integration Tests
   - 运行DAST
   - 部署Monitoring Stack

4. **监控部署**
   - 查看GitHub Actions运行状态
   - 检查AWS资源创建
   - 访问Grafana查看监控数据

---

**关键点：** 本地测试只需要Docker和Java，不需要AWS。这样可以快速验证CI/CD流程的有效性！
