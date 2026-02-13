#!/bin/bash

# EcoGo CI/CD 完整测试脚本
# 这个脚本会按顺序执行所有测试

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 计数器
TESTS_PASSED=0
TESTS_FAILED=0

# 辅助函数
print_header() {
    echo -e "\n${BLUE}╔════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║ $1${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════╝${NC}\n"
}

print_test() {
    echo -e "${BLUE}→${NC} $1"
}

print_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((TESTS_PASSED++))
}

print_fail() {
    echo -e "${RED}✗${NC} $1"
    ((TESTS_FAILED++))
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# 测试1: 环境验证
test_environment() {
    print_header "测试1: 环境验证"

    print_test "检查Java..."
    if command -v java >/dev/null 2>&1; then
        VERSION=$(java -version 2>&1 | grep version | cut -d'"' -f2)
        print_pass "Java可用: $VERSION"
    else
        print_fail "Java未找到"
        return 1
    fi

    print_test "检查Maven..."
    if command -v mvn >/dev/null 2>&1; then
        print_pass "Maven可用"
    else
        print_fail "Maven未找到"
        return 1
    fi

    print_test "检查Docker..."
    if command -v docker >/dev/null 2>&1; then
        print_pass "Docker可用"
    else
        print_fail "Docker未找到"
        return 1
    fi

    print_test "检查Docker daemon..."
    if docker ps >/dev/null 2>&1; then
        print_pass "Docker daemon运行中"
    else
        print_fail "Docker daemon未启动"
        return 1
    fi
}

# 测试2: 配置文件验证
test_configuration_files() {
    print_header "测试2: 配置文件验证"

    REQUIRED_FILES=(
        ".github/workflows/cicd-pipeline.yml"
        "Dockerfile"
        "pom.xml"
        "sonar-project.properties"
        "terraform/main.tf"
        "ansible/deploy.yml"
        "monitoring/docker-compose.yml"
        "scripts/local-deploy.sh"
    )

    for file in "${REQUIRED_FILES[@]}"; do
        print_test "检查 $file..."
        if [[ -f "$file" ]]; then
            print_pass "$file 存在"
        else
            print_fail "$file 不存在"
        fi
    done
}

# 测试3: LINT检查
test_lint() {
    print_header "测试3: LINT检查 (Checkstyle)"
    print_info "这可能需要2-3分钟..."

    if mvn checkstyle:check -q 2>/dev/null; then
        print_pass "Checkstyle检查通过"
    else
        print_warning "Checkstyle发现问题(查看: mvn checkstyle:checkstyle)"
    fi
}

# 测试4: 构建测试
test_build() {
    print_header "测试4: 应用构建"
    print_info "这可能需要3-5分钟..."

    if mvn clean package -DskipTests -q 2>/dev/null; then
        print_pass "应用构建成功"

        if [[ -f "target/EcoGo-0.0.1-SNAPSHOT.jar" ]]; then
            print_pass "JAR文件生成成功"
        fi
    else
        print_fail "应用构建失败"
        return 1
    fi
}

# 测试5: Docker镜像构建
test_docker_build() {
    print_header "测试5: Docker镜像构建"
    print_info "这可能需要2-3分钟..."

    if docker build -t ecogo:cicd-test . -q 2>/dev/null; then
        print_pass "Docker镜像构建成功"

        IMAGE_ID=$(docker images ecogo:cicd-test -q)
        SIZE=$(docker images ecogo:cicd-test --format "{{.Size}}")
        print_info "镜像ID: $IMAGE_ID, 大小: $SIZE"
    else
        print_fail "Docker镜像构建失败"
        return 1
    fi
}

# 测试6: 监控栈启动
test_monitoring_stack() {
    print_header "测试6: 监控栈启动"
    print_info "这可能需要1-2分钟..."

    cd monitoring

    if docker-compose up -d 2>&1 | grep -q "done"; then
        print_pass "监控栈启动成功"

        print_test "等待服务启动..."
        sleep 15

        print_test "检查MongoDB..."
        if docker exec ecogo-mongodb mongosh --eval "db.adminCommand('ping')" >/dev/null 2>&1; then
            print_pass "MongoDB运行中"
        else
            print_warning "MongoDB未就绪"
        fi

        print_test "检查Prometheus..."
        if curl -s http://localhost:9090/-/healthy >/dev/null 2>&1; then
            print_pass "Prometheus运行中"
        else
            print_warning "Prometheus未就绪"
        fi

        print_test "检查Grafana..."
        if curl -s http://localhost:3000/api/health >/dev/null 2>&1; then
            print_pass "Grafana运行中"
        else
            print_warning "Grafana未就绪"
        fi
    else
        print_fail "监控栈启动失败"
    fi

    cd ..
}

# 测试7: 应用启动
test_app_startup() {
    print_header "测试7: 应用启动测试"
    print_info "这可能需要1-2分钟..."

    print_test "启动应用容器..."

    # 停止旧容器
    docker stop ecogo-cicd-test 2>/dev/null || true
    docker rm ecogo-cicd-test 2>/dev/null || true

    if docker run -d \
        --name ecogo-cicd-test \
        --network monitoring_ecogo-monitoring \
        -p 8091:8090 \
        -e SPRING_DATA_MONGODB_URI=mongodb://ecogo-mongodb:27017/EcoGo \
        -e SPRING_PROFILES_ACTIVE=test \
        ecogo:cicd-test >/dev/null 2>&1; then

        print_pass "应用容器启动成功"

        print_test "等待应用启动..."
        sleep 20

        print_test "检查健康状态..."
        for i in {1..10}; do
            if curl -s http://localhost:8091/actuator/health >/dev/null 2>&1; then
                HEALTH=$(curl -s http://localhost:8091/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
                if [[ "$HEALTH" = "UP" ]]; then
                    print_pass "应用健康状态: $HEALTH"
                    return 0
                fi
            fi
            echo "  等待中... ($i/10)"
            sleep 2
        done

        print_warning "应用未完全启动，但容器在运行"
    else
        print_fail "应用启动失败"
        return 1
    fi
}

# 测试8: API测试
test_api_endpoints() {
    print_header "测试8: API端点测试"

    print_test "测试 /actuator/health..."
    if curl -s http://localhost:8091/actuator/health | grep -q "UP"; then
        print_pass "Health端点正常"
    else
        print_warning "Health端点响应异常"
    fi

    print_test "测试 /actuator/info..."
    if curl -s http://localhost:8091/actuator/info >/dev/null 2>&1; then
        print_pass "Info端点可访问"
    else
        print_warning "Info端点不可访问"
    fi

    print_test "测试 /actuator/prometheus..."
    if curl -s http://localhost:8091/actuator/prometheus >/dev/null 2>&1; then
        METRICS=$(curl -s http://localhost:8091/actuator/prometheus | wc -l)
        print_pass "Prometheus端点可访问 ($METRICS行指标)"
    else
        print_warning "Prometheus端点不可访问"
    fi
}

# 清理
cleanup() {
    print_header "清理资源"

    print_test "停止测试应用..."
    docker stop ecogo-cicd-test 2>/dev/null || true
    docker rm ecogo-cicd-test 2>/dev/null || true
    print_pass "应用已停止"

    print_test "清理测试镜像..."
    docker rmi ecogo:cicd-test 2>/dev/null || true
    print_pass "镜像已清理"

    print_info "监控栈已保留（cd monitoring && docker-compose down 可停止）"
}

# 主函数
main() {
    echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   EcoGo CI/CD 完整测试套件              ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"

    test_environment || exit 1
    test_configuration_files
    test_lint || print_warning "LINT检查未通过，继续其他测试..."
    test_build || exit 1
    test_docker_build || exit 1
    test_monitoring_stack
    test_app_startup || print_warning "应用启动有问题"
    test_api_endpoints

    cleanup

    # 显示总结
    print_header "测试总结"
    echo -e "${GREEN}通过: $TESTS_PASSED${NC}"
    echo -e "${RED}失败: $TESTS_FAILED${NC}"

    if [[ $TESTS_FAILED -eq 0 ]]; then
        echo -e "\n${GREEN}✓ 所有关键测试通过！${NC}"
        echo -e "${GREEN}✓ 可以安全地推送到GitHub${NC}"
        return 0
    else
        echo -e "\n${RED}✗ 某些测试失败，请检查上面的输出${NC}"
        return 1
    fi
}

# 运行
main "$@"
