# 一键部署脚本集合

> 项目: MITEDTSM (密讯ETM系统)
> 版本: v1.0
> 更新日期: 2026-03-19

本文档汇总开发环境初始化、启动、停止等运维脚本。

---

## 一、环境初始化脚本 (init_env.sh)

> 适用于 Ubuntu 26.04 全新系统，一键安装所有开发依赖。
> 用法: `sudo bash init_env.sh`

```bash
#!/bin/bash
# ============================================================
# MITEDTSM - Ubuntu 一键初始化脚本
# 版本: v1.0
# 日期: 2026-03-19
# 用法: sudo bash init_env.sh
# ============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ----------------------------------------------------------
# 0. 检查权限
# ----------------------------------------------------------
if [ "$EUID" -ne 0 ]; then
    log_error "请使用 sudo 运行此脚本"
    exit 1
fi

ACTUAL_USER=$(logname 2>/dev/null || echo $SUDO_USER)
USER_HOME=$(eval echo ~$ACTUAL_USER)

log_info "开始初始化 MITEDTSM 开发环境..."
log_info "当前用户: $ACTUAL_USER, 用户目录: $USER_HOME"
echo ""

# ----------------------------------------------------------
# 1. 配置 APT 镜像源（阿里云）
# ----------------------------------------------------------
log_info ">>> 步骤 1/10: 配置 APT 镜像源（阿里云）"

if [ -f /etc/apt/sources.list ]; then
    cp /etc/apt/sources.list /etc/apt/sources.list.bak.$(date +%Y%m%d)
fi

cat > /etc/apt/sources.list << 'EOF'
deb https://mirrors.aliyun.com/ubuntu/ noble main restricted universe multiverse
deb https://mirrors.aliyun.com/ubuntu/ noble-updates main restricted universe multiverse
deb https://mirrors.aliyun.com/ubuntu/ noble-backports main restricted universe multiverse
deb https://mirrors.aliyun.com/ubuntu/ noble-security main restricted universe multiverse
EOF

log_info "APT 镜像源配置完成"

# ----------------------------------------------------------
# 2. 系统更新
# ----------------------------------------------------------
log_info ">>> 步骤 2/10: 系统更新"

apt update -y && apt upgrade -y
log_info "系统更新完成"

# ----------------------------------------------------------
# 3. 基础工具
# ----------------------------------------------------------
log_info ">>> 步骤 3/10: 安装基础工具"

apt install -y curl wget vim git unzip net-tools ca-certificates gnupg lsb-release
log_info "基础工具安装完成"

# ----------------------------------------------------------
# 4. OpenJDK 17
# ----------------------------------------------------------
log_info ">>> 步骤 4/10: 安装 OpenJDK 17"

apt install -y openjdk-17-jdk

JAVA_HOME_LINE='export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64'
PATH_LINE='export PATH=$JAVA_HOME/bin:$PATH'

if ! grep -q "JAVA_HOME" $USER_HOME/.bashrc 2>/dev/null; then
    echo "$JAVA_HOME_LINE" >> $USER_HOME/.bashrc
    echo "$PATH_LINE" >> $USER_HOME/.bashrc
fi

log_info "OpenJDK 17 安装完成"

# ----------------------------------------------------------
# 5. Maven 3.9
# ----------------------------------------------------------
log_info ">>> 步骤 5/10: 安装 Maven 3.9"

MAVEN_VERSION="3.9.9"
cd /opt

if [ ! -d "/opt/maven" ]; then
    wget -q "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    tar -xzf "apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    mv "apache-maven-${MAVEN_VERSION}" maven
    rm -f "apache-maven-${MAVEN_VERSION}-bin.tar.gz"
fi

if ! grep -q "MAVEN_HOME" $USER_HOME/.bashrc 2>/dev/null; then
    echo 'export MAVEN_HOME=/opt/maven' >> $USER_HOME/.bashrc
    echo 'export PATH=$MAVEN_HOME/bin:$PATH' >> $USER_HOME/.bashrc
fi

# 配置 settings.xml (阿里云镜像)
mkdir -p $USER_HOME/.m2
cat > $USER_HOME/.m2/settings.xml << 'SETEOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
          https://maven.apache.org/xsd/settings-1.2.0.xsd">
    <mirrors>
        <mirror>
            <id>aliyun-maven</id>
            <name>阿里云Maven仓库</name>
            <url>https://maven.aliyun.com/repository/public</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
        <mirror>
            <id>aliyun-spring</id>
            <name>阿里云Spring仓库</name>
            <url>https://maven.aliyun.com/repository/spring</url>
            <mirrorOf>spring</mirrorOf>
        </mirror>
    </mirrors>
    <profiles>
        <profile>
            <id>aliyun</id>
            <repositories>
                <repository>
                    <id>aliyun</id>
                    <url>https://maven.aliyun.com/repository/public</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>false</enabled></snapshots>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <id>aliyun-plugin</id>
                    <url>https://maven.aliyun.com/repository/public</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>false</enabled></snapshots>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>aliyun</activeProfile>
    </activeProfiles>
</settings>
SETEOF

chown -R $ACTUAL_USER:$ACTUAL_USER $USER_HOME/.m2
log_info "Maven 3.9 安装完成"

# ----------------------------------------------------------
# 6. Node.js + pnpm
# ----------------------------------------------------------
log_info ">>> 步骤 6/10: 安装 Node.js LTS + pnpm"

curl -fsSL https://deb.nodesource.com/setup_lts.x | bash -
apt install -y nodejs

npm config set registry https://registry.npmmirror.com
npm install -g pnpm
pnpm config set registry https://registry.npmmirror.com

log_info "Node.js $(node -v) + pnpm $(pnpm -v) 安装完成"

# ----------------------------------------------------------
# 7. Docker + Docker Compose
# ----------------------------------------------------------
log_info ">>> 步骤 7/10: 安装 Docker + Docker Compose"

curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun

mkdir -p /etc/docker
cat > /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://dockerhub.icu",
    "https://docker.chenby.cn"
  ]
}
EOF

systemctl daemon-reload
systemctl enable docker
systemctl restart docker
usermod -aG docker $ACTUAL_USER

COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep tag_name | cut -d '"' -f 4)
curl -L "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

log_info "Docker + Docker Compose 安装完成"

# ----------------------------------------------------------
# 8. Git 配置
# ----------------------------------------------------------
log_info ">>> 步骤 8/10: 配置 Git"

echo ""
log_warn "请输入 Git 用户信息:"
read -p "  Git 用户名: " GIT_USERNAME
read -p "  Git 邮箱:   " GIT_EMAIL

git config --global user.name "$GIT_USERNAME"
git config --global user.email "$GIT_EMAIL"

if [ ! -f "$USER_HOME/.ssh/id_ed25519" ]; then
    su - $ACTUAL_USER -c "ssh-keygen -t ed25519 -C \"$GIT_EMAIL\" -N \"\" -f ~/.ssh/id_ed25519"
fi

log_info "Git 配置完成"
log_info "SSH 公钥如下，请添加到 Gitee:"
echo ""
cat $USER_HOME/.ssh/id_ed25519.pub
echo ""

# ----------------------------------------------------------
# 9. 项目初始化
# ----------------------------------------------------------
log_info ">>> 步骤 9/10: 项目初始化"

# 启动开发中间件 (如果项目已克隆)
if [ -f "$SCRIPT_DIR/../InstallPackage/dev/docker-compose.yml" ]; then
    cd "$SCRIPT_DIR/../InstallPackage/dev"
    docker compose up -d
    log_info "开发中间件启动完成 (MySQL/Redis/RabbitMQ)"
else
    log_warn "未找到项目 docker-compose.yml，跳过中间件启动"
    log_warn "克隆项目后执行: cd InstallPackage/dev && docker compose up -d"
fi

# ----------------------------------------------------------
# 10. 验证
# ----------------------------------------------------------
log_info ">>> 步骤 10/10: 环境验证"

echo ""
echo "=============================="
echo "   MITEDTSM 环境安装完成！"
echo "=============================="
echo ""
echo "Java:    $(java -version 2>&1 | head -1)"
echo "Maven:   $(/opt/maven/bin/mvn -version 2>&1 | head -1)"
echo "Node:    $(node -v)"
echo "npm:     $(npm -v)"
echo "pnpm:    $(pnpm -v)"
echo "Docker:  $(docker --version)"
echo "Compose: $(docker compose version)"
echo "Git:     $(git --version)"
echo ""
echo "=================================="
echo "  重要提示"
echo "=================================="
echo ""
echo "1. 请重新登录以使 docker 组生效"
echo "2. 请将上方 SSH 公钥添加到 Gitee"
echo "3. 数据库初始化: InstallPackage/database/base/"
echo "4. MinIO 控制台: http://localhost:9001 (minioadmin/Minio@2026)"
echo "5. 默认登录: admin / admin123 @ 密讯科技"
echo ""
echo "初始化脚本执行完毕！"
```

---

## 二、一键启动脚本 (start.sh)

> 用法: `bash start.sh [dev|prod]`
> 默认环境: dev

```bash
#!/bin/bash
# ============================================================
# MITEDTSM - 一键启动脚本
# 版本: v1.0
# 日期: 2026-03-19
# 用法: bash start.sh [dev|prod]
# ============================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

ENV=${1:-dev}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

log_info "=========================================="
log_info "  MITEDTSM 系统启动 (环境: $ENV)"
log_info "=========================================="
echo ""

# ----------------------------------------------------------
# 1. 检查依赖
# ----------------------------------------------------------
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先执行 init_env.sh"
        exit 1
    fi
}

log_info ">>> 检查依赖..."

check_command java
check_command mvn
check_command node
check_command docker
check_command pnpm

log_info "依赖检查通过"
echo ""

# ----------------------------------------------------------
# 2. 启动基础设施 (Docker Compose)
# ----------------------------------------------------------
log_info ">>> 启动基础设施 (MySQL/Redis/RabbitMQ)..."

if [ -f "$PROJECT_ROOT/InstallPackage/dev/docker-compose.yml" ]; then
    cd "$PROJECT_ROOT/InstallPackage/dev"
    docker compose up -d
else
    log_warn "未找到 docker-compose.yml，跳过 Docker 服务启动"
    log_warn "请确保 MySQL/Redis/RabbitMQ 已手动启动"
fi

# 等待 MySQL 就绪
log_info "等待 MySQL 就绪..."
for i in $(seq 1 30); do
    if docker exec mitedtsm-mysql mysqladmin ping -h localhost -u root -p1234 --silent 2>/dev/null; then
        log_info "MySQL 已就绪"
        break
    fi
    if [ $i -eq 30 ]; then
        log_error "MySQL 启动超时"
        exit 1
    fi
    sleep 2
done

echo ""

# ----------------------------------------------------------
# 3. 构建后端
# ----------------------------------------------------------
log_info ">>> 构建后端服务..."

BACKEND_DIR="$PROJECT_ROOT/Code/Server"
if [ -d "$BACKEND_DIR" ]; then
    cd "$BACKEND_DIR"
    mvn clean package -DskipTests -q
    log_info "后端构建完成"
else
    log_warn "未找到后端目录 $BACKEND_DIR，跳过构建"
fi

echo ""

# ----------------------------------------------------------
# 4. 启动后端
# ----------------------------------------------------------
log_info ">>> 启动后端服务..."

JAR_FILE=$(find "$BACKEND_DIR/mitedtsm-server/target" -name "*.jar" -not -name "*sources*" 2>/dev/null | head -1)

if [ -n "$JAR_FILE" ]; then
    if [ -f "$SCRIPT_DIR/app.pid" ]; then
        OLD_PID=$(cat "$SCRIPT_DIR/app.pid")
        if kill -0 $OLD_PID 2>/dev/null; then
            log_warn "后端进程已运行 (PID: $OLD_PID)，先停止..."
            kill $OLD_PID
            sleep 2
        fi
    fi

    if [ "$ENV" = "prod" ]; then
        nohup java -jar \
            -Xms512m -Xmx1024m \
            -Dspring.profiles.active=prod \
            "$JAR_FILE" \
            > "$SCRIPT_DIR/app.log" 2>&1 &
    else
        nohup java -jar \
            -Xms256m -Xmx512m \
            -Dspring.profiles.active=local \
            "$JAR_FILE" \
            > "$SCRIPT_DIR/app.log" 2>&1 &
    fi

    APP_PID=$!
    echo $APP_PID > "$SCRIPT_DIR/app.pid"
    log_info "后端启动成功 (PID: $APP_PID)"
else
    log_warn "未找到可执行 JAR，跳过启动"
fi

echo ""

# ----------------------------------------------------------
# 5. 验证
# ----------------------------------------------------------
log_info ">>> 验证服务状态..."

sleep 3

if [ -f "$SCRIPT_DIR/app.pid" ]; then
    APP_PID=$(cat "$SCRIPT_DIR/app.pid")
    if kill -0 $APP_PID 2>/dev/null; then
        log_info "后端服务: 运行中 (PID: $APP_PID)"
    else
        log_error "后端服务: 未启动"
    fi
fi

docker ps --format "table {{.Names}}\t{{.Status}}" 2>/dev/null | grep mitedtsm || true

echo ""
log_info "=========================================="
log_info "  MITEDTSM 系统启动完成！"
log_info "=========================================="
echo ""
log_info "后端 API:       http://localhost:8080"
log_info "Web Admin:      http://localhost:3000 (pnpm dev)"
log_info "Admin Mobile:   http://localhost:5173 (pnpm dev:h5)"
log_info "Portal Web:     http://localhost:3001 (pnpm dev)"
log_info "MinIO 控制台:   http://localhost:9001"
log_info "RabbitMQ 管理:  http://localhost:15672"
log_info "后端日志:       tail -f $SCRIPT_DIR/app.log"
echo ""
log_info "默认登录: admin / admin123 @ 密讯科技"
```

---

## 三、一键停止脚本 (stop.sh)

> 用法: `bash stop.sh`

```bash
#!/bin/bash
# ============================================================
# MITEDTSM - 一键停止脚本
# 版本: v1.0
# 日期: 2026-03-19
# 用法: bash stop.sh
# ============================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

log_info "=========================================="
log_info "  MITEDTSM 系统停止"
log_info "=========================================="
echo ""

# ----------------------------------------------------------
# 1. 停止后端进程
# ----------------------------------------------------------
log_info ">>> 停止后端服务..."

if [ -f "$SCRIPT_DIR/app.pid" ]; then
    APP_PID=$(cat "$SCRIPT_DIR/app.pid")
    if kill -0 $APP_PID 2>/dev/null; then
        log_info "发送停止信号 (PID: $APP_PID)..."
        kill $APP_PID

        # 等待优雅关闭
        for i in $(seq 1 15); do
            if ! kill -0 $APP_PID 2>/dev/null; then
                log_info "后端服务已停止"
                break
            fi
            sleep 1
        done

        # 强制杀死
        if kill -0 $APP_PID 2>/dev/null; then
            log_warn "强制停止后端服务..."
            kill -9 $APP_PID 2>/dev/null || true
            log_info "后端服务已强制停止"
        fi
    else
        log_warn "后端进程不存在 (PID: $APP_PID)"
    fi
    rm -f "$SCRIPT_DIR/app.pid"
else
    log_warn "未找到 app.pid 文件"
fi

echo ""

# ----------------------------------------------------------
# 2. 停止前端开发服务器 (Vite)
# ----------------------------------------------------------
log_info ">>> 停止前端开发服务器..."

VITE_PIDS=$(ps aux | grep -i "vite" | grep -v grep | awk '{print $2}')
if [ -n "$VITE_PIDS" ]; then
    for pid in $VITE_PIDS; do
        log_info "停止 Vite 进程 (PID: $pid)..."
        kill $pid 2>/dev/null || true
    done
    log_info "前端开发服务器已停止"
else
    log_info "未发现运行中的 Vite 进程"
fi

echo ""

# ----------------------------------------------------------
# 3. 停止 Docker Compose 服务
# ----------------------------------------------------------
log_info ">>> 停止 Docker 服务..."

PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

if [ -f "$PROJECT_ROOT/InstallPackage/dev/docker-compose.yml" ]; then
    cd "$PROJECT_ROOT/InstallPackage/dev"
    docker compose down 2>/dev/null && \
        log_info "Docker Compose 服务已停止" || \
        log_warn "Docker Compose 停止失败，请手动检查"
else
    # 手动停止容器
    docker stop mitedtsm-mysql mitedtsm-redis mitedtsm-rabbitmq 2>/dev/null || true
    docker rm mitedtsm-mysql mitedtsm-redis mitedtsm-rabbitmq 2>/dev/null || true
    log_info "Docker 容器已停止"
fi

echo ""

# ----------------------------------------------------------
# 4. 验证
# ----------------------------------------------------------
log_info ">>> 验证服务状态..."

RUNNING=$(docker ps --format "table {{.Names}}" 2>/dev/null | grep mitedtsm || true)
if [ -z "$RUNNING" ]; then
    log_info "所有 MITEDTSM Docker 容器已停止"
else
    log_warn "仍有容器运行:"
    echo "$RUNNING"
fi

echo ""
log_info "=========================================="
log_info "  MITEDTSM 系统已停止！"
log_info "=========================================="
```

---

## 四、常用运维命令速查

```bash
# 查看后端日志
tail -f app.log

# 查看 Docker 容器状态
docker compose ps

# 重启某个服务
docker compose restart mysql

# 进入 MySQL
docker exec -it mitedtsm-mysql mysql -u root -p1234 mitedtsm_database

# 进入 Redis
docker exec -it mitedtsm-redis redis-cli

# 查看 RabbitMQ 状态
docker exec -it mitedtsm-rabbitmq rabbitmqctl status

# 清理所有容器和数据
docker compose down -v

# 重建并启动
docker compose up -d --build

# 查看端口占用
sudo netstat -tlnp | grep -E '3306|6379|8080|3000|15672'
```

---

## 参考资料

- [Docker 开发环境搭建](./02-Docker-Setup.md)
- [Ubuntu 开发手册](./07-Ubuntu-Dev-Guide.md)
- [Docker Compose 完整配置](./08-DockerCompose.yml)
