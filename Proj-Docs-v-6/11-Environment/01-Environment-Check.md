# 开发环境检查报告

> 项目: MITEDTSM (密讯ETM系统)
> 版本: v1.0
> 更新日期: 2026-03-07

---

## 1. 操作系统

| 项目 | 值 |
|------|-----|
| OS | Ubuntu 26.04 LTS (Linux 7.0.0-22-generic) |
| 架构 | x86_64 |
| 主机名 | ranbow-Precision-M6700 |
| Kernel | 7.0.0-22-generic |

### 系统信息检查

```bash
cat /etc/os-release
uname -a
```

---

## 2. 运行时版本 (从项目配置推断)

| 工具 | 检测值 | 项目要求 | 验证命令 |
|------|--------|----------|---------|
| Java | JDK 17 | JDK 17 (OpenJDK) | `java -version` |
| Maven | 需确认 | 3.9+ (项目使用 3.9.9) | `mvn --version` |
| Node.js | v24.14.1 | >= 16 (Web), >= 20 (Mobile) | `node --version` |
| npm | 需确认 | 最新稳定版 | `npm --version` |
| pnpm | 需确认 | >= 8.6 (Web), >= 9 (Mobile) | `pnpm --version` |
| Docker | 需确认 | 最新稳定版 | `docker --version` |
| Docker Compose | 需确认 | v2.x | `docker compose version` |
| Git | 需确认 | 2.x+ | `git --version` |
| MySQL | 需确认 | 8.0 (Client) | `mysql --version` |
| Redis | 需确认 | 7.x (Docker) | `redis-cli --version` |

---

## 3. 项目配置版本

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.5.9 |
| Maven Compiler | 3.14.0 |
| Maven Surefire | 3.5.3 |
| MyBatis Plus | 3.5.15 |
| Vue (Web Admin) | 3.5.12 |
| Vue (Admin Mobile) | ^3.4.21 |
| Vite (Web) | 5.1.4 |
| Vite (Mobile) | 5.2.8 |
| Element Plus | 2.11.1 |
| TypeScript (Web) | 5.3.3 |
| TypeScript (Mobile) | ~5.8.0 |
| uni-app | 3.0.0-4070620250821001 |
| Docker MySQL | 8.0 |
| Docker Redis | 7-alpine |
| Docker RabbitMQ | 3-management-alpine |

---

## 4. 需手动验证项目

```bash
# 基础工具
java -version
mvn --version
node --version
npm --version
pnpm --version
docker --version
docker compose version
git --version
cat /etc/os-release

# 中间件 (Docker方式)
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"

# 数据库连通性
mysql -h 127.0.0.1 -u root -p1234 -e "SELECT VERSION();"
redis-cli -h 127.0.0.1 ping
```

---

## 5. 完整验证脚本

保存为 `env_check.sh`，执行 `bash env_check.sh` 即可完成全量验证：

```bash
#!/bin/bash
# env_check.sh - MITEDTSM 开发环境验证脚本
# 用法: bash env_check.sh

echo "========== MITEDTSM 环境验证 =========="
echo ""

echo "--- 基础工具 ---"
echo -n "Java:    "; java -version 2>&1 | head -1
echo -n "Maven:   "; mvn -version 2>&1 | head -1
echo -n "Node:    "; node -v
echo -n "npm:     "; npm -v
echo -n "pnpm:    "; pnpm -v
echo -n "MySQL:   "; mysql --version 2>/dev/null || echo "未安装 (使用Docker)"
echo -n "Redis:   "; redis-cli --version 2>/dev/null || echo "未安装 (使用Docker)"
echo -n "Docker:  "; docker --version
echo -n "Compose: "; docker compose version
echo -n "Git:     "; git --version

echo ""
echo "--- Docker 服务状态 ---"
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}" 2>/dev/null | grep -E "mitedtsm|NAMES" || echo "无运行中的 mitedtsm 容器"

echo ""
echo "--- 网络连通性 ---"
ping -c 1 -W 2 mirrors.aliyun.com > /dev/null 2>&1 && echo "APT镜像 (aliyun)  : OK" || echo "APT镜像 (aliyun)  : FAIL"
ping -c 1 -W 2 gitee.com > /dev/null 2>&1 && echo "Gitee             : OK" || echo "Gitee             : FAIL"
ping -c 1 -W 2 registry.npmmirror.com > /dev/null 2>&1 && echo "NPM镜像 (npmmirror): OK" || echo "NPM镜像 (npmmirror): FAIL"
curl -s --connect-timeout 3 https://maven.aliyun.com > /dev/null 2>&1 && echo "Maven镜像 (aliyun) : OK" || echo "Maven镜像 (aliyun) : FAIL"

echo ""
echo "--- Docker 中间件连通性 ---"
docker exec mitedtsm-mysql mysqladmin ping -h localhost -u root -p1234 --silent 2>/dev/null && echo "MySQL   : OK" || echo "MySQL   : FAIL"
docker exec mitedtsm-redis redis-cli ping 2>/dev/null && echo "Redis   : OK" || echo "Redis   : FAIL"

echo ""
echo "========== 验证完成 =========="
```

---

## 6. 镜像源配置参考

### 6.1 APT 镜像 (阿里云)

```bash
sudo tee /etc/apt/sources.list << 'EOF'
deb https://mirrors.aliyun.com/ubuntu/ noble main restricted universe multiverse
deb https://mirrors.aliyun.com/ubuntu/ noble-updates main restricted universe multiverse
deb https://mirrors.aliyun.com/ubuntu/ noble-backports main restricted universe multiverse
deb https://mirrors.aliyun.com/ubuntu/ noble-security main restricted universe multiverse
EOF
sudo apt update && sudo apt upgrade -y
```

### 6.2 Maven 镜像 (settings.xml)

```xml
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
```

### 6.3 NPM 镜像

```bash
npm config set registry https://registry.npmmirror.com
pnpm config set registry https://registry.npmmirror.com
```

### 6.4 Docker 镜像加速

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://dockerhub.icu",
    "https://docker.chenby.cn"
  ]
}
```

---

## 参考资料

- [Ubuntu 官方文档](https://help.ubuntu.com/)
- [阿里云开源镜像站](https://developer.aliyun.com/mirror/)
- [Docker 官方文档](https://docs.docker.com/)
- [清华大学镜像站](https://mirrors.tuna.tsinghua.edu.cn/)
