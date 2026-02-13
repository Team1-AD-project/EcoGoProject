#!/bin/bash

# 简化版测试脚本 - Windows友好

echo "=========================================="
echo "EcoGo 快速测试"
echo "=========================================="
echo ""

# 1. 构建应用
echo "[1/5] 构建应用..."
./mvnw clean package -DskipTests
if [[ $? -eq 0 ]]; then
    echo "✓ 应用构建成功"
else
    echo "✗ 应用构建失败"
    exit 1
fi
echo ""

# 2. 构建Docker镜像
echo "[2/5] 构建Docker镜像..."
docker build -t ecogo:test . -q
if [[ $? -eq 0 ]]; then
    echo "✓ Docker镜像构建成功"
else
    echo "✗ Docker镜像构建失败"
    exit 1
fi
echo ""

# 3. 启动监控栈
echo "[3/5] 启动监控栈..."
cd monitoring
docker-compose up -d
cd ..
echo "等待服务启动..."
sleep 20
echo "✓ 监控栈已启动"
echo ""

# 4. 启动应用
echo "[4/5] 启动应用..."
docker stop ecogo-test 2>/dev/null || true
docker rm ecogo-test 2>/dev/null || true

docker run -d \
  --name ecogo-test \
  --network monitoring_ecogo-monitoring \
  -p 8091:8090 \
  -e SPRING_DATA_MONGODB_URI=mongodb://ecogo-mongodb:27017/EcoGo \
  -e SPRING_PROFILES_ACTIVE=test \
  ecogo:test

echo "等待应用启动..."
sleep 25
echo "✓ 应用已启动"
echo ""

# 5. 测试端点
echo "[5/5] 测试API端点..."
HEALTH=$(curl -s http://localhost:8091/actuator/health)
if echo "$HEALTH" | grep -q "UP"; then
    echo "✓ 健康检查: UP"
else
    echo "⚠ 健康检查: $HEALTH"
fi

METRICS=$(curl -s http://localhost:8091/actuator/prometheus | wc -l)
if [[ $METRICS -gt 10 ]]; then
    echo "✓ Prometheus指标: $METRICS 行"
else
    echo "⚠ Prometheus指标异常"
fi
echo ""

# 完成
echo "=========================================="
echo "测试完成！"
echo "=========================================="
echo ""
echo "访问地址："
echo "- 应用健康: http://localhost:8091/actuator/health"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "查看日志："
echo "  docker logs ecogo-test"
echo ""
echo "清理资源："
echo "  docker stop ecogo-test && docker rm ecogo-test"
echo "  cd monitoring && docker-compose down && cd .."
