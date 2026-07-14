# Docker 本地开发环境搭建指南

> 项目: MITEDTSM (密讯ETM系统)
> 版本: v2.0
> 更新日期: 2026-03-07

---

## 1. Docker 安装

### 1.1 安装 Docker

```bash
curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER
# 重新登录使 docker 组生效
```

### 1.2 配置 Docker 镜像加速

```bash
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://dockerhub.icu",
    "https://docker.chenby.cn"
  ]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 1.3 安装 Docker Compose

```bash
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 1.4 验证

```bash
docker --version
docker compose version
```

---

## 2. 开发环境 (启动中间件)

```bash
# 启动 MySQL + Redis + RabbitMQ
cd InstallPackage/dev
docker compose up -d

# 验证
docker compose ps
# 应看到: mitedtsm-mysql, mitedtsm-redis, mitedtsm-rabbitmq
```

### 中间件连接信息

| 服务 | 端口 | 用户名 | 密码 |
|------|------|--------|------|
| MySQL | 3306 | root | 1234 |
| Redis | 6379 | - | (无密码) |
| RabbitMQ | 5672 (AMQP), 15672 (管理) | rabbit | rabbit |

---

## 3. 数据库初始化

```bash
# 数据库初始化 SQL 目录
ls InstallPackage/database/base/
# 连接 MySQL 执行初始化脚本
mysql -h 127.0.0.1 -u root -p1234 mitedtsm_database < InstallPackage/database/base/*.sql
```

---

## 4. 后端启动

### IDEA 方式 (推荐)
1. 打开 `Code/Server/` (Maven项目)
2. Run `MitedtsmServerApplication`
3. Profile: `local`
4. 访问: `http://localhost:8080`

### 命令行方式
```bash
cd Code/Server
mvn clean package -DskipTests
java -jar mitedtsm-server/target/Server.jar --spring.profiles.active=local
```

---

## 5. 前端启动

```bash
# Admin Web
cd Code/Web
pnpm install
npm run dev
# → http://localhost:3000

# Admin Mobile
cd Code/AdminMobileFrontend
pnpm install
pnpm dev:h5
# → http://localhost:5173 (或其他端口)

# Portal Web
cd Code/portal-web
pnpm install
npm run dev
# → http://localhost:3001

# Mall Frontend
cd Code/MallFrontend
pnpm install
# 使用 HBuilderX: 运行 → 运行到浏览器
```

---

## 6. 全栈 Docker 部署

```bash
cd InstallPackage/docker
docker compose up -d --build
# Services: mysql, redis, rabbitmq, server(8080), frontend(80/443)
```

### 完整 Docker Compose 配置

详见 [08-DockerCompose.yml](./08-DockerCompose.yml)。

### 服务组件一览

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| MySQL | mysql:8.0 | 3306 | 数据库 |
| Redis | redis:7-alpine | 6379 | 缓存 |
| RabbitMQ | rabbitmq:3-management-alpine | 5672/15672 | 消息队列 |
| MinIO | minio/minio:latest | 9000/9001 | 对象存储 (API/控制台) |
| Nginx | nginx:alpine | 80/443 | 反向代理 |
| App | 项目构建镜像 | 8080 | Spring Boot 后端 |

### 健康检查

- **MySQL**: `mysqladmin ping -h localhost -u root -p1234` — 10s 间隔，5次重试
- **Redis**: `redis-cli ping` — 10s 间隔，5次重试
- **MinIO**: `curl -f http://localhost:9000/minio/health/live` — 30s 间隔，3次重试
- **App**: `curl -f http://localhost:8080/actuator/health` — 30s 间隔，5次重试，60s 启动等待
- **Nginx**: `nginx -t` — 30s 间隔，3次重试

---

## 7. MinIO 对象存储 (Docker 方式)

### 7.1 拉取并启动

```bash
docker run -d \
  --name mitedtsm-minio \
  --restart=always \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=Minio@2026 \
  -v /data/minio:/data \
  minio/minio server /data --console-address ":9001"
```

### 7.2 创建 Bucket

登录 http://localhost:9001，使用 `minioadmin / Minio@2026` 登录，创建 Bucket：

- `mitedtsm-file` — 文件存储
- `mitedtsm-avatar` — 头像存储

---

## 8. 常用调试

```bash
# 查看后端日志
docker logs -f mitedtsm-server

# 查看MySQL
docker exec -it mitedtsm-mysql mysql -u root -p1234

# 查看Redis
docker exec -it mitedtsm-redis redis-cli

# 查看RabbitMQ
docker exec -it mitedtsm-rabbitmq rabbitmqctl status

# 重启后端
docker compose restart server

# 查看所有容器状态
docker compose ps

# 清理并重建
docker compose down -v
docker compose up -d --build
```

---

## 9. 网络说明

所有服务通过 `mitedtsm-network` (bridge 模式) 互联，服务间使用容器名通信：

- MySQL: `jdbc:mysql://mysql:3306/mitedtsm_database`
- Redis: `redis:6379`
- RabbitMQ: `rabbitmq:5672`
- MinIO: `http://minio:9000`

---

## 参考资料

- [Docker 官方文档](https://docs.docker.com/)
- [MinIO 文档](https://min.io/docs/)
- [Nginx 配置指南](./09-Nginx-Config.md)
- [Docker Compose 完整配置](./08-DockerCompose.yml)
