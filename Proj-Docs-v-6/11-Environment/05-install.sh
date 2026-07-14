#!/bin/bash
# MITEDTSM CRM 模块 - 新开发环境一键安装脚本
# 用法: chmod +x install.sh && ./install.sh

set -e

echo "========================================="
echo " MITEDTSM CRM 开发环境安装脚本"
echo "========================================="

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

check_command() {
    if command -v "$1" &> /dev/null; then
        echo -e "${GREEN}✓${NC} $1: $(command -v "$1")"
        return 0
    else
        echo -e "${RED}✗${NC} $1: 未安装"
        return 1
    fi
}

# 1. 检查基础环境
echo -e "\n${YELLOW}[1/5] 检查基础环境...${NC}"
MISSING=0

check_command "java" || MISSING=1
check_command "node" || MISSING=1
check_command "pnpm" || MISSING=1
check_command "mvn" || MISSING=1
check_command "docker" || MISSING=1
check_command "git" || MISSING=1

if [ $MISSING -eq 1 ]; then
    echo -e "\n${RED}缺少必要工具，请先安装后再运行此脚本${NC}"
    exit 1
fi

# 版本检查
JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "Java 主版本: $JAVA_VER"
NODE_VER=$(node --version | cut -d'v' -f2)
echo "Node 版本: $NODE_VER"

# 2. 启动 Docker 中间件
echo -e "\n${YELLOW}[2/5] 启动 Docker 中间件...${NC}"
cd InstallPackage/dev
docker compose up -d
echo "等待中间件就绪..."
sleep 5
docker compose ps

# 3. 初始化数据库
echo -e "\n${YELLOW}[3/5] 初始化数据库...${NC}"
cd ../..
if [ -d "InstallPackage/database/base" ]; then
    for f in InstallPackage/database/base/*.sql; do
        echo "执行: $f"
        docker exec -i mitedtsm-mysql mysql -u root -p1234 mitedtsm_database < "$f" 2>/dev/null || true
    done
    echo -e "${GREEN}✓ 数据库初始化完成${NC}"
fi

# 4. 安装前端依赖
echo -e "\n${YELLOW}[4/5] 安装前端依赖...${NC}"

if [ -d "Code/Web" ]; then
    echo "安装 Web Admin 依赖..."
    cd Code/Web && pnpm install && cd ../..
    echo -e "${GREEN}✓ Web Admin 依赖安装完成${NC}"
fi

if [ -d "Code/portal-web" ]; then
    echo "安装 Portal Web 依赖..."
    cd Code/portal-web && pnpm install && cd ../..
    echo -e "${GREEN}✓ Portal Web 依赖安装完成${NC}"
fi

if [ -d "Code/AdminMobileFrontend" ]; then
    echo "安装 Admin Mobile 依赖..."
    cd Code/AdminMobileFrontend && pnpm install && cd ../..
    echo -e "${GREEN}✓ Admin Mobile 依赖安装完成${NC}"
fi

# 5. 验证
echo -e "\n${YELLOW}[5/5] 验证环境...${NC}"
echo "MySQL: $(docker exec mitedtsm-mysql mysqladmin ping -h localhost 2>&1)"
echo "Redis: $(docker exec mitedtsm-redis redis-cli ping 2>&1)"
echo "RabbitMQ: $(docker exec mitedtsm-rabbitmq rabbitmqctl status 2>&1 | head -1)"

echo -e "\n${GREEN}=========================================${NC}"
echo -e "${GREEN} 环境安装完成!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo "启动开发服务器:"
echo "  后端: IDEA 打开 Code/Server/, 运行 MitedtsmServerApplication (profile=local)"
echo "  Web Admin: cd Code/Web && npm run dev         → http://localhost:3000"
echo "  Portal Web: cd Code/portal-web && npm run dev → http://localhost:3001"
echo "  Admin Mobile: cd Code/AdminMobileFrontend && pnpm dev:h5"
echo ""
echo "登录信息:"
echo "  地址: http://localhost:3000"
echo "  租户: 密讯科技"
echo "  账号: admin"
echo "  密码: admin123"
