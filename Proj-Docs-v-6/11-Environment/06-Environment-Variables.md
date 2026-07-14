# 环境变量与配置说明

> 项目: MITEDTSM (密讯ETM系统)
> 版本: v1.0
> 更新日期: 2026-03-12

---

## 1. 后端配置文件

```
Server/mitedtsm-server/src/main/resources/
├── application.yaml            # 主配置 (公共)
├── application-local.yaml      # 本地开发
├── application-dev.yaml        # 开发环境
├── application-test.yaml       # 测试环境
├── application-stage.yaml      # 预发布
├── application-prod.yaml       # 生产环境
├── application-docker.yaml     # Docker环境
└── application-uat.yaml        # UAT环境
```

### application-local.yaml 关键配置

| 配置 | 值 |
|------|-----|
| 端口 | 8080 |
| MySQL URL | jdbc:mysql://localhost:3306/mitedtsm_database |
| MySQL 用户/密码 | root/1234 |
| Redis Host | localhost:6379 |
| Redis 密码 | (无) |
| RabbitMQ Host | localhost:5672 |
| RabbitMQ 用户/密码 | rabbit/rabbit |
| 验证码 | 关闭 |
| Mock | 开启 |

### application-docker.yaml 关键配置

| 配置 | 值 |
|------|-----|
| 端口 | 8080 |
| MySQL URL | jdbc:mysql://mysql:3306/mitedtsm_database |
| MySQL 用户/密码 | root/1234 |
| Redis Host | redis (容器名) |
| Redis 密码 | (无) |
| RabbitMQ Host | rabbitmq (容器名) |
| RabbitMQ 用户/密码 | rabbit/rabbit |
| MinIO Endpoint | http://minio:9000 |
| MinIO AccessKey | minioadmin |
| MinIO SecretKey | Minio@2026 |
| JVM 参数 | -Xms512m -Xmx1024m -XX:+UseG1GC |

---

## 2. Web Admin 前端环境变量

### .env (基础配置)
```bash
VITE_APP_TITLE=MG.OS.4.EC
VITE_PORT=3000
VITE_OPEN=true
VITE_APP_TENANT_ENABLE=true
VITE_APP_CAPTCHA_ENABLE=true
VITE_APP_DEFAULT_LOGIN_TENANT=密讯科技
VITE_APP_DEFAULT_LOGIN_USERNAME=admin
VITE_APP_DEFAULT_LOGIN_PASSWORD=admin123
VITE_APP_API_ENCRYPT_ENABLE=true
VITE_APP_API_ENCRYPT_ALGORITHM=AES
```

### .env.local (本地开发覆盖)
```bash
VITE_BASE_URL='http://localhost:8080'
VITE_UPLOAD_TYPE=server
VITE_API_URL=/admin-api
VITE_APP_CAPTCHA_ENABLE=false
```

### .env.prod (生产环境)
```bash
VITE_BASE_URL='https://www.meession.com.cn:8080'
VITE_DROP_DEBUGGER=true
VITE_DROP_CONSOLE=true
VITE_OUT_DIR=dist-prod
```

---

## 3. Docker Compose 环境变量

### InstallPackage/docker/.env
```bash
MYSQL_DATABASE=mitedtsm_database
MYSQL_ROOT_PASSWORD=1234
FRONTEND_PORT=443
```

### InstallPackage/dev/.env (开发中间件)
```bash
MYSQL_DATABASE=mitedtsm_database
MYSQL_ROOT_PASSWORD=1234
```

---

## 4. 构建参数传递

```
.env → .env.local → .env.[mode] → Vite define → process.env
```

Vite 构建时:
- `npm run dev` → `.env` + `.env.local`
- `npm run build:prod` → `.env` + `.env.prod`
- VITE_ 前缀的变量会被注入到客户端代码

---

## 5. Docker 镜像构建参数

### 后端 Dockerfile 环境变量
```dockerfile
ENV SPRING_PROFILES_ACTIVE=docker
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
ENV DB_HOST=mysql
ENV DB_PORT=3306
ENV DB_NAME=mitedtsm_database
ENV DB_USER=root
ENV DB_PASSWORD=1234
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV MINIO_ENDPOINT=http://minio:9000
ENV MINIO_ACCESS_KEY=minioadmin
ENV MINIO_SECRET_KEY=Minio@2026
```

### MinIO 环境变量
```bash
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=Minio@2026
```

---

## 6. CRM 新模块配置建议

新增 CRM 相关配置:
```yaml
# application-local.yaml
crm:
  customer-sea:
    auto-reclaim-days: 30      # 公海自动回收天数
    max-claim-per-day: 10      # 每人每天最大领取数
  opportunity:
    max-unfollowed-days: 7     # 未跟进预警天数
  order:
    approval-amount: 100000    # 订单审批金额阈值

# 开发环境特殊配置
logging:
  level:
    com.meession.etm: DEBUG

# 文件上传配置
file:
  upload:
    max-size: 100MB
    allowed-types: jpg,png,pdf,doc,docx,xls,xlsx
```

---

## 7. 多语言环境变量

```bash
# 前端 locale 文件路径
Web Admin:    Code/Web/src/locales/<lang>/
Admin Mobile: Code/AdminMobileFrontend/src/locale/<lang>/

# 后端 i18n 资源
Server/mitedtsm-server/src/main/resources/i18n/messages_<lang>.properties

# 支持的语言: zh_CN, en_US, ar_SA, de_DE, fr_FR, it_IT, pt_BR, ru_RU
```

---

## 8. 安全配置建议

```bash
# 生产环境必须修改
- 数据库密码 (禁止使用默认 1234)
- Redis 密码 (生产环境必须设置)
- MinIO 凭证
- JWT 签名密钥
- Spring Security 相关配置
```

---

## 参考资料

- [Spring Boot 外部化配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Vite 环境变量文档](https://vitejs.dev/guide/env-and-mode.html)
