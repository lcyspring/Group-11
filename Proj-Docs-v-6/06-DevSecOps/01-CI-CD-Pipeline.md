# CI/CD 持续集成与持续部署 - 密讯ETM系统 (mitedtsm)

---

## 文档信息

| 字段 | 内容 |
|------|------|
| 项目名称 | 密讯ETM企业管理系统 (mitedtsm) |
| 文档版本 | v1.0 |
| 创建日期 | 2026-06-25 |
| 负责人 | DevSecOps 团队 |
| 状态 | 已发布 |

---

## 1. CI/CD 整体架构

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           CI/CD 流水线全景图                                    │
└──────────────────────────────────────────────────────────────────────────────┘

 代码提交        构建阶段                 测试阶段              安全扫描             部署阶段
 ────────       ────────                ────────              ────────            ────────

┌────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ Git    │   │ Maven 构建    │   │ JUnit 单元测试 │   │ SonarQube    │   │ Docker 构建  │
│ Push   │──▶│ pnpm 构建    │──▶│ JaCoCo 覆盖率 │──▶│ OWASP 扫描   │──▶│ 镜像推送      │
│        │   │ 代码编译      │   │ API集成测试   │   │ Trivy 扫描   │   │ K8s/Docker   │
└────────┘   └──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘
                    │                  │                  │                  │
                    ▼                  ▼                  ▼                  ▼
              ┌──────────┐    ┌──────────┐    ┌──────────┐     ┌──────────────┐
              │ 编译失败  │    │ 测试失败  │    │ 安全漏洞  │     │ DEV 环境      │
              │ → 驳回    │    │ → 驳回    │    │ → 高/严重 │     │ → 自动部署    │
              │           │    │           │    │ → 驳回    │     │              │
              └──────────┘    └──────────┘    └──────────┘     └──────────────┘
                                                                      │
                                                              ┌───────┴───────┐
                                                              ▼               ▼
                                                       ┌──────────┐   ┌──────────┐
                                                       │ TEST     │   │ STAGING  │
                                                       │ 手动触发  │   │ 手动触发  │
                                                       └──────────┘   └──────────┘
                                                                              │
                                                                              ▼
                                                                       ┌──────────┐
                                                                       │ PROD     │
                                                                       │ 审批后   │
                                                                       │ 部署     │
                                                                       └──────────┘
```

---

## 2. 环境管理

### 2.1 环境定义

| 环境 | 标识 | 用途 | 部署方式 | 审批要求 |
|------|------|------|---------|---------|
| **DEV** | `dev` | 开发联调、快速验证 | 自动（develop分支合并） | 无 |
| **TEST** | `test` | 集成测试、功能测试 | 手动触发 | 无 |
| **STAGING** | `staging` | 预发布、UAT、性能测试 | 手动触发 | Tech Lead 审批 |
| **PROD** | `prod` | 生产环境 | 手动触发 | PM + Tech Lead 审批 |

### 2.2 环境配置

| 配置项 | DEV | TEST | STAGING | PROD |
|--------|-----|------|---------|------|
| 数据库 | MySQL 8.0 (共享) | MySQL 8.0 (独立) | MySQL 8.0 (独立) | MySQL 8.0 (主从) |
| Redis | Redis 7.0 (共享) | Redis 7.0 (独立) | Redis 7.0 (集群) | Redis 7.0 (哨兵) |
| RabbitMQ | 共享实例 | 独立实例 | 集群 | 集群 |
| 日志级别 | DEBUG | INFO | WARN | WARN |
| 监控 | 基础 | 基础 | 完整(SkyWalking) | 完整(SkyWalking) |
| 资源限制 | 0.5C/512M | 1C/1G | 2C/2G | 4C/4G |
| 副本数 | 1 | 1 | 2 | 3 |
| 域名 | dev.mitedtsm.cn | test.mitedtsm.cn | staging.mitedtsm.cn | mitedtsm.cn |

---

## 3. CI 流水线配置

### 3.1 后端流水线 (Maven)

```yaml
# .gitee-ci.yml - 后端 CI/CD 流水线
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  DOCKER_REGISTRY: "registry.mitedtsm.cn"

cache:
  key: "${CI_COMMIT_REF_SLUG}"
  paths:
    - .m2/repository/
    - target/

stages:
  - compile
  - test
  - quality
  - security
  - package
  - deploy

# Stage 1: 编译阶段
maven-compile:
  stage: compile
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd Code/Server
    - mvn clean compile -DskipTests -q
  artifacts:
    paths:
      - Code/Server/*/target/classes/
    expire_in: 1 hour
  only:
    - branches

# Stage 2: 测试阶段
unit-test:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd Code/Server
    - mvn test -Dmaven.test.failure.ignore=false
  artifacts:
    when: always
    reports:
      junit:
        - Code/Server/*/target/surefire-reports/TEST-*.xml
    paths:
      - Code/Server/*/target/surefire-reports/
      - Code/Server/*/target/jacoco.exec
    expire_in: 7 days
  coverage: '/Total.*?([0-9]{1,3})%/'
  only:
    - branches

integration-test:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  services:
    - mysql:8.0
    - redis:7.0
    - rabbitmq:3.12-management
  variables:
    MYSQL_ROOT_PASSWORD: "test_password"
    MYSQL_DATABASE: "mitedtsm_test"
    SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/mitedtsm_test"
    SPRING_REDIS_HOST: "redis"
  script:
    - cd Code/Server
    - mvn verify -Pintegration-test -Dskip.unit.tests=true
  only:
    - develop
    - merge_requests

# Stage 3: 质量检查
sonarqube-check:
  stage: quality
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd Code/Server
    - mvn sonar:sonar
      -Dsonar.host.url=$SONAR_HOST_URL
      -Dsonar.projectKey=mitedtsm-server
      -Dsonar.login=$SONAR_TOKEN
      -Dsonar.java.binaries=target/classes
      -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
  only:
    - develop
    - merge_requests
    - release/*

# Stage 4: 安全扫描
owasp-dependency-check:
  stage: security
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd Code/Server
    - mvn org.owasp:dependency-check-maven:check
      -DfailBuildOnCVSS=7
      -Dformat=HTML
  artifacts:
    when: always
    paths:
      - Code/Server/target/dependency-check-report.html
    expire_in: 30 days
  only:
    - develop
    - release/*
    - merge_requests

# Stage 5: 打包阶段
maven-package:
  stage: package
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd Code/Server
    - mvn clean package -DskipTests -q
  artifacts:
    paths:
      - Code/Server/mitedtsm-server/target/*.jar
    expire_in: 7 days
  only:
    - develop
    - release/*
    - master

docker-build:
  stage: package
  image: docker:24-dind
  services:
    - docker:24-dind
  variables:
    DOCKER_TLS_CERTDIR: ""
  before_script:
    - echo "$DOCKER_REGISTRY_PASSWORD" | docker login $DOCKER_REGISTRY -u $DOCKER_REGISTRY_USER --password-stdin
  script:
    - |
      if [ "$CI_COMMIT_REF_NAME" == "master" ]; then
        TAG="latest"
      else
        TAG="${CI_COMMIT_REF_NAME//\//-}"
      fi
    - docker build -f Code/Server/Dockerfile -t $DOCKER_REGISTRY/mitedtsm-server:$TAG .
    - docker push $DOCKER_REGISTRY/mitedtsm-server:$TAG
  only:
    - develop
    - release/*
    - master

# Stage 6: 部署阶段
deploy-dev:
  stage: deploy
  image: docker:24
  script:
    - cd InstallPackage/dev
    - docker compose up -d --build
  environment:
    name: development
  only:
    - develop

deploy-staging:
  stage: deploy
  image: docker:24
  script:
    - cd InstallPackage/docker
    - docker compose up -d --build
  environment:
    name: staging
  when: manual
  only:
    - release/*

deploy-prod:
  stage: deploy
  image: docker:24
  script:
    - cd InstallPackage/docker
    - docker compose up -d --build
  environment:
    name: production
  when: manual
  only:
    - master
```

### 3.2 前端流水线 (pnpm + Vite)

```yaml
# .gitee-ci-web.yml - Admin Web 前端 CI/CD
variables:
  DOCKER_REGISTRY: "registry.mitedtsm.cn"

cache:
  key: "${CI_COMMIT_REF_SLUG}"
  paths:
    - Code/Web/node_modules/
    - Code/Web/.pnpm-store/

stages:
  - install
  - lint
  - test
  - quality
  - build
  - deploy

pnpm-install:
  stage: install
  image: node:20-alpine
  before_script:
    - corepack enable
    - corepack prepare pnpm@9 --activate
  script:
    - cd Code/Web
    - pnpm config set store-dir .pnpm-store
    - pnpm install --frozen-lockfile
  artifacts:
    paths:
      - Code/Web/node_modules/
    expire_in: 1 hour
  only:
    - branches

eslint-check:
  stage: lint
  image: node:20-alpine
  before_script:
    - corepack enable && corepack prepare pnpm@9 --activate
  script:
    - cd Code/Web && pnpm run lint:eslint
  only:
    - branches

type-check:
  stage: lint
  image: node:20-alpine
  before_script:
    - corepack enable && corepack prepare pnpm@9 --activate
  script:
    - cd Code/Web && pnpm run ts:check
  only:
    - branches

unit-test-web:
  stage: test
  image: node:20-alpine
  before_script:
    - corepack enable && corepack prepare pnpm@9 --activate
  script:
    - cd Code/Web && pnpm run test:coverage
  artifacts:
    when: always
    paths:
      - Code/Web/coverage/
    expire_in: 7 days
  only:
    - branches

vite-build:
  stage: build
  image: node:20-alpine
  before_script:
    - corepack enable && corepack prepare pnpm@9 --activate
  script:
    - cd Code/Web && pnpm run build:prod
  artifacts:
    paths:
      - Code/Web/dist/
    expire_in: 7 days
  only:
    - develop
    - release/*
    - master

docker-build-web:
  stage: build
  image: docker:24-dind
  services:
    - docker:24-dind
  script:
    - docker build -f Code/Web/Dockerfile -t $DOCKER_REGISTRY/mitedtsm-web:$CI_COMMIT_SHORT_SHA .
    - docker push $DOCKER_REGISTRY/mitedtsm-web:$CI_COMMIT_SHORT_SHA
  only:
    - develop
    - release/*
    - master
```

### 3.3 移动端流水线 (uni-app)

```yaml
# .gitee-ci-mobile.yml - Admin 移动端 CI/CD
stages:
  - install
  - lint
  - build

mobile-install:
  stage: install
  image: node:20-alpine
  before_script:
    - corepack enable && corepack prepare pnpm@9 --activate
  script:
    - cd Code/AdminMobileFrontend && pnpm install --frozen-lockfile
  only:
    - branches

mobile-lint:
  stage: lint
  image: node:20-alpine
  before_script:
    - corepack enable && corepack prepare pnpm@9 --activate
  script:
    - cd Code/AdminMobileFrontend && pnpm lint:fix
  only:
    - branches

mobile-build-h5:
  stage: build
  image: node:20-alpine
  before_script:
    - corepack enable && corepack prepare pnpm@9 --activate
  script:
    - cd Code/AdminMobileFrontend && pnpm build:h5:prod
  artifacts:
    paths:
      - Code/AdminMobileFrontend/dist/build/h5/
    expire_in: 7 days
  only:
    - develop
    - release/*
    - master
```

---

## 4. Maven 构建配置

### 4.1 父 POM 关键配置

```xml
<project>
    <groupId>com.meession.etm</groupId>
    <artifactId>mitedtsm</artifactId>
    <version>${revision}</version>
    <packaging>pom</packaging>

    <name>密讯ETM系统</name>
    <description>mitedtsm - Multi-Module Enterprise Management Platform</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.9</version>
        <relativePath/>
    </parent>

    <properties>
        <revision>2026.01-SNAPSHOT</revision>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- 插件版本 -->
        <sonar-maven-plugin.version>3.11.0.3922</sonar-maven-plugin.version>
        <jacoco-maven-plugin.version>0.8.12</jacoco-maven-plugin.version>
        <maven-checkstyle-plugin.version>3.3.1</maven-checkstyle-plugin.version>
    </properties>

    <modules>
        <module>mitedtsm-dependencies</module>
        <module>mitedtsm-framework</module>
        <module>mitedtsm-server</module>
        <module>mitedtsm-module-system</module>
        <module>mitedtsm-module-infra</module>
        <module>mitedtsm-module-bpm</module>
        <module>mitedtsm-module-crm</module>
        <module>mitedtsm-module-erp</module>
        <module>mitedtsm-module-mall</module>
        <module>mitedtsm-module-member</module>
        <module>mitedtsm-module-pay</module>
        <module>mitedtsm-module-mp</module>
        <module>mitedtsm-module-ai</module>
        <module>mitedtsm-module-report</module>
        <module>mitedtsm-module-unified-product</module>
        <module>mitedtsm-module-overseas-service</module>
        <module>mitedtsm-module-wms</module>
        <module>mitedtsm-module-mes</module>
        <module>mitedtsm-module-zatca</module>
    </modules>

    <build>
        <plugins>
            <!-- JaCoCo 代码覆盖率 -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>${jacoco-maven-plugin.version}</version>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
                        <goals><goal>prepare-agent</goal></goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals><goal>report</goal></goals>
                    </execution>
                </executions>
                <configuration>
                    <excludes>
                        <exclude>**/dto/**</exclude>
                        <exclude>**/dataobject/**</exclude>
                        <exclude>**/vo/**</exclude>
                        <exclude>**/config/**</exclude>
                        <exclude>**/enums/**</exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 5. 前端构建配置

### 5.1 Admin Web (Vue 3 + Vite)

```json
{
  "name": "mitedtsm-web",
  "version": "2026.01",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --mode development",
    "build:local": "vite build --mode local",
    "build:dev": "vite build --mode dev",
    "build:test": "vite build --mode test",
    "build:stage": "vite build --mode stage",
    "build:prod": "vite build --mode prod",
    "ts:check": "vue-tsc --noEmit",
    "lint:eslint": "eslint . --ext .vue,.ts,.tsx --fix",
    "lint:format": "prettier --write \"src/**/*.{ts,vue,css,scss}\"",
    "lint:style": "stylelint --fix \"src/**/*.{css,scss,vue}\""
  },
  "engines": {
    "node": ">=20.0.0",
    "pnpm": ">=9.0.0"
  }
}
```

### 5.2 Admin Mobile (uni-app + Vue 3)

```json
{
  "name": "mitedtsm-admin-mobile",
  "scripts": {
    "dev:h5": "uni",
    "dev:mp-weixin": "uni -p mp-weixin",
    "dev:app": "uni -p app",
    "build:h5:prod": "uni build",
    "lint:fix": "eslint . --ext .vue,.ts --fix"
  },
  "engines": {
    "node": ">=20.0.0",
    "pnpm": ">=9.0.0"
  }
}
```

---

## 6. Docker 镜像构建

### 6.1 后端 Dockerfile

```dockerfile
# 密讯ETM - 后端 Dockerfile (多阶段构建)
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY Code/Server/pom.xml .
COPY Code/Server/ src/
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S mitedtsm && adduser -S mitedtsm -G mitedtsm
WORKDIR /app
COPY --from=builder /build/mitedtsm-server/target/*.jar app.jar
RUN chown -R mitedtsm:mitedtsm /app
USER mitedtsm
EXPOSE 8080
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=docker"]
```

### 6.2 前端 Dockerfile (Nginx)

```dockerfile
# 密讯ETM - Admin Web 前端 Dockerfile
FROM node:20-alpine AS builder
WORKDIR /build
COPY Code/Web/package.json Code/Web/pnpm-lock.yaml ./
RUN corepack enable && corepack prepare pnpm@9 --activate
RUN pnpm install --frozen-lockfile
COPY Code/Web/ .
RUN pnpm run build:prod

FROM nginx:1.25-alpine
COPY Code/Web/deploy/nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /build/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 6.3 Nginx 配置

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # 安全头
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    # SPA 路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /admin-api/ {
        proxy_pass http://mitedtsm-server:8080/admin-api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 7. Docker Compose 部署

```yaml
# InstallPackage/docker/docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
      - ./database/base:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

  redis:
    image: redis:7.0-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS}

  init-service:
    build: ../Code/InitService
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/mitedtsm

  server:
    image: registry.mitedtsm.cn/mitedtsm-server:latest
    depends_on:
      - mysql
      - redis
      - rabbitmq
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/mitedtsm
      SPRING_REDIS_HOST: redis

  web:
    image: registry.mitedtsm.cn/mitedtsm-web:latest
    depends_on:
      - server
    ports:
      - "80:80"

volumes:
  mysql-data:
```

---

## 8. 制品管理

| 制品类型 | 存储位置 | 保留策略 | 清理策略 |
|---------|---------|---------|---------|
| JAR 包 | Gitee Package Registry | 30 天 | 自动清理 |
| Docker 镜像 | Harbor Registry | 90 天 | 保留最近 10 个版本 |
| npm 包 | Gitee Package Registry | 30 天 | 自动清理 |
| 测试报告 | CI Artifacts | 30 天 | 自动清理 |
| 扫描报告 | CI Artifacts | 90 天 | 自动清理 |

### 镜像标签策略

| 标签格式 | 说明 | 示例 |
|---------|------|------|
| `latest` | 最新稳定版（对应 master） | `mitedtsm-server:latest` |
| `2026.01` | 版本号 | `mitedtsm-server:2026.01` |
| `develop` | 开发最新版 | `mitedtsm-server:develop` |
| `<commit-sha>` | 精确构建版本 | `mitedtsm-server:a1b2c3d` |

---

## 9. 流水线监控

| 指标 | 目标值 | 告警阈值 |
|------|--------|---------|
| 流水线成功率 | ≥ 95% | < 90% |
| 平均构建时间 | ≤ 8 分钟 | > 15 分钟 |
| 平均部署时间 | ≤ 5 分钟 | > 10 分钟 |
| 代码审查响应时间 | ≤ 4 小时 | > 24 小时 |
| 合并到部署时间 | ≤ 30 分钟 | > 2 小时 |

### 通知机制

| 事件 | 渠道 | 接收人 |
|------|------|--------|
| 构建失败 | 企业微信 + 邮件 | 提交者 + Team Lead |
| 部署成功 | 企业微信 | 全体成员 |
| 安全漏洞发现 | 企业微信 + 邮件 | 安全负责人 + Team Lead |
| 生产环境部署 | 企业微信 + 邮件 | 全体成员 |

---

## 10. 快速故障排查

| 问题 | 排查步骤 |
|------|---------|
| Maven 构建失败 | 检查依赖版本、网络连接、本地仓库、JDK 17 兼容性 |
| 前端构建失败 | 检查 Node 版本(≥20)、pnpm lock 文件一致性 |
| Docker 镜像构建失败 | 检查 Dockerfile 语法、基础镜像可用性 |
| Docker Compose 部署失败 | 检查端口占用、环境变量、MySQL/Redis 就绪状态 |
| 测试失败 | 检查测试环境配置、数据库连接、Mock 数据 |

---

> **文档维护**: 本文档由 DevSecOps 团队维护，版本变更需经过 Tech Lead 审批。
