# Ubuntu 26.04 开发环境搭建手册

> 项目: MITEDTSM (密讯ETM系统)
> 适用对象: 开发人员
> 版本: v1.0
> 更新日期: 2026-03-12

---

## 一、Ubuntu 26.04 系统安装

### 1.1 下载镜像

从 [Ubuntu 官方镜像](https://mirrors.aliyun.com/ubuntu-releases/26.04/) 或 [清华大学镜像站](https://mirrors.tuna.tsinghua.edu.cn/ubuntu-releases/26.04/) 下载 Ubuntu 26.04 LTS Desktop ISO。

### 1.2 制作启动盘

使用 Rufus（Windows）或 balenaEtcher（跨平台）将 ISO 写入 U 盘。

### 1.3 安装步骤

1. 从 U 盘启动，选择 "Install Ubuntu"
2. 选择语言：中文（简体）
3. 键盘布局：Chinese
4. 安装类型：正常安装（勾选安装第三方驱动）
5. 分区：建议使用 LVM，分区方案如下：
   - `/boot`：1GB
   - `/`：50GB+
   - `/home`：剩余空间
6. 设置用户名和密码
7. 等待安装完成，重启

---

## 二、中国镜像源配置

### 2.1 备份原有源

```bash
sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak
```

### 2.2 阿里云镜像源（推荐）

```bash
sudo tee /etc/apt/sources.list << 'EOF'
deb https://mirrors.aliyun.com/ubuntu/ noble main restricted universe multiverse
deb https://mirrors.aliyun.com/ubuntu/ noble-updates main restricted universe multiverse
deb https://mirrors.aliyun.com/ubuntu/ noble-backports main restricted universe multiverse
deb https://mirrors.aliyun.com/ubuntu/ noble-security main restricted universe multiverse
EOF
```

### 2.3 清华大学镜像源

```bash
sudo tee /etc/apt/sources.list << 'EOF'
deb https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ noble main restricted universe multiverse
deb https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ noble-updates main restricted universe multiverse
deb https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ noble-backports main restricted universe multiverse
deb https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ noble-security main restricted universe multiverse
EOF
```

### 2.4 更新索引

```bash
sudo apt update && sudo apt upgrade -y
```

---

## 三、OpenJDK 17 安装配置

### 3.1 安装

```bash
sudo apt install -y openjdk-17-jdk openjdk-17-doc openjdk-17-source
```

### 3.2 验证

```bash
java -version
javac -version
```

预期输出：

```
openjdk version "17.0.x" 2026-xx-xx
OpenJDK Runtime Environment (build 17.0.x+xx)
OpenJDK 64-Bit Server VM (build 17.0.x+xx, mixed mode, sharing)
```

### 3.3 配置 JAVA_HOME

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

---

## 四、Maven 3.9+ 安装配置

### 4.1 下载安装

```bash
cd /opt
sudo wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
sudo tar -xzf apache-maven-3.9.9-bin.tar.gz
sudo mv apache-maven-3.9.9 maven
```

### 4.2 配置环境变量

```bash
echo 'export MAVEN_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$MAVEN_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### 4.3 配置阿里云仓库（settings.xml）

编辑 `~/.m2/settings.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
          https://maven.apache.org/xsd/settings-1.2.0.xsd">

    <localRepository>/home/${user.name}/.m2/repository</localRepository>

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
```

### 4.4 验证

```bash
mvn -version
```

---

## 五、Node.js LTS + pnpm 安装

### 5.1 安装 Node.js LTS（使用 nvm 推荐）

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
source ~/.bashrc
nvm install --lts
nvm use --lts
```

### 5.2 配置 npm 镜像

```bash
npm config set registry https://registry.npmmirror.com
```

### 5.3 安装 pnpm

```bash
npm install -g pnpm
pnpm config set registry https://registry.npmmirror.com
```

### 5.4 验证

```bash
node -v
npm -v
pnpm -v
```

---

## 六、MySQL 8.0 安装与初始化

### 6.1 安装

```bash
sudo apt install -y mysql-server mysql-client
```

### 6.2 安全配置

```bash
sudo mysql_secure_installation
```

交互式配置：
- VALIDATE PASSWORD COMPONENT：N（或根据需要选择）
- New password：`1234`（与项目开发环境保持一致）
- Remove anonymous users：Y
- Disallow root login remotely：N
- Remove test database：Y
- Reload privilege tables：Y

### 6.3 创建数据库与用户

```sql
sudo mysql -u root -p
```

```sql
CREATE DATABASE mitedtsm_database DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
FLUSH PRIVILEGES;
EXIT;
```

### 6.4 允许远程访问

```bash
sudo sed -i 's/^bind-address.*/bind-address = 0.0.0.0/' /etc/mysql/mysql.conf.d/mysqld.cnf
sudo systemctl restart mysql
```

---

## 七、Redis 7 安装配置

### 7.1 安装

```bash
sudo apt install -y redis-server
```

### 7.2 配置

编辑 `/etc/redis/redis.conf`：

```
bind 0.0.0.0
maxmemory 512mb
maxmemory-policy allkeys-lru
```

> 注意：开发环境不设密码，与项目 `application-local.yaml` 一致。

### 7.3 启动

```bash
sudo systemctl enable redis-server
sudo systemctl restart redis-server
```

### 7.4 验证

```bash
redis-cli ping
```

---

## 八、Docker + Docker Compose 安装

### 8.1 安装 Docker

```bash
curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER
```

### 8.2 配置 Docker 镜像加速

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

### 8.3 安装 Docker Compose

```bash
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 8.4 验证

```bash
docker --version
docker compose version
```

---

## 九、MinIO 安装（Docker 方式）

### 9.1 拉取并启动

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

### 9.2 创建 Bucket

登录 http://localhost:9001，使用 `minioadmin / Minio@2026` 登录，创建 Bucket：

- `mitedtsm-file` — 文件存储
- `mitedtsm-avatar` — 头像存储

---

## 十、Git 安装与 Gitee 配置

### 10.1 安装 Git

```bash
sudo apt install -y git
git config --global user.name "Your Name"
git config --global user.email "your@email.com"
```

### 10.2 生成 SSH Key

```bash
ssh-keygen -t ed25519 -C "your@email.com"
```

### 10.3 添加公钥到 Gitee

```bash
cat ~/.ssh/id_ed25519.pub
```

将输出的公钥内容复制到 Gitee → 设置 → SSH 公钥

### 10.4 验证

```bash
ssh -T git@gitee.com
```

预期输出：`Hi xxx! You've successfully authenticated...`

---

## 十一、项目代码克隆与初始化

### 11.1 克隆项目

```bash
git clone git@gitee.com:meession/MITEDTSM.git
cd MITEDTSM
```

### 11.2 启动开发中间件

```bash
cd InstallPackage/dev
docker compose up -d
```

### 11.3 初始化数据库

```bash
mysql -h 127.0.0.1 -u root -p1234 mitedtsm_database < InstallPackage/database/base/*.sql
```

### 11.4 安装前端依赖

```bash
cd Code/Web && pnpm install && cd ../..
cd Code/AdminMobileFrontend && pnpm install && cd ../..
cd Code/portal-web && pnpm install && cd ../..
```

### 11.5 启动开发服务器

```bash
# 后端: IDEA 打开 Code/Server/，运行 MitedtsmServerApplication (profile=local)
# Web Admin: cd Code/Web && npm run dev          → http://localhost:3000
# Admin Mobile: cd Code/AdminMobileFrontend && pnpm dev:h5
# Portal Web: cd Code/portal-web && npm run dev  → http://localhost:3001
```

---

## 十二、完整验证脚本

```bash
#!/bin/bash
# env_check.sh - MITEDTSM 开发环境验证脚本

echo "========== MITEDTSM 环境验证 =========="

echo -n "Java:    "; java -version 2>&1 | head -1
echo -n "Maven:   "; mvn -version 2>&1 | head -1
echo -n "Node:    "; node -v
echo -n "npm:     "; npm -v
echo -n "pnpm:    "; pnpm -v
echo -n "MySQL:   "; mysql --version
echo -n "Redis:   "; redis-cli --version
echo -n "Docker:  "; docker --version
echo -n "Compose: "; docker compose version
echo -n "Git:     "; git --version

echo ""
echo "========== 服务状态 =========="

systemctl is-active mysql      2>/dev/null && echo "MySQL  : running" || echo "MySQL  : stopped"
systemctl is-active redis-server 2>/dev/null && echo "Redis  : running" || echo "Redis  : stopped"
systemctl is-active docker     2>/dev/null && echo "Docker : running" || echo "Docker : stopped"

echo ""
echo "========== 网络连通性 =========="

ping -c 1 -W 2 mirrors.aliyun.com > /dev/null 2>&1 && echo "APT镜像 : OK" || echo "APT镜像 : FAIL"
ping -c 1 -W 2 gitee.com > /dev/null 2>&1 && echo "Gitee  : OK" || echo "Gitee  : FAIL"

echo ""
echo "========== Docker 中间件 =========="
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}" 2>/dev/null | grep -E "mitedtsm|NAMES" || echo "无运行中的容器"

echo ""
echo "========== 验证完成 =========="
```

保存为 `env_check.sh`，执行 `bash env_check.sh` 即可完成全量验证。

---

## 参考资料

- [Ubuntu 官方文档](https://help.ubuntu.com/)
- [阿里云开源镜像站](https://developer.aliyun.com/mirror/)
- [Docker 官方文档](https://docs.docker.com/)
- [MinIO 文档](https://min.io/docs/)
- [项目 CLAUDE.md](../../CLAUDE.md)
