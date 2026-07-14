# MITEDTSM 系统架构分析

## 1. 系统概述

MITEDTSM（密讯信息技术）是企业级多模块综合管理平台，基于 RuoYi-Vue-Pro 深度定制。支持多租户和 8 种语言国际化（中/英/阿/德/法/意/葡/俄）。

- **Group ID**: com.meession.etm
- **Version**: 2026.01-SNAPSHOT
- **Java Base Package**: com.meession.etm
- **构建工具**: Maven 多模块

## 2. 系统分层架构

```
客户端层:
  Admin Web (Vue 3) | Admin Mobile (uni-app) | Mall (uni-app) | Mall Android | Portal Web (Vue 3)
        ↓
网关层: Nginx (路由分发 /admin-api, /app-api, /portal-api)
        ↓
服务层: Spring Boot 3.5.9 (JDK 17)
  ├── mitedtsm-server (主入口)
  ├── mitedtsm-framework (14 个自定义 Starter)
  │   ├── web, security, mybatis, redis, mq, job
  │   ├── monitor, protection, tenant, data-permission
  │   ├── ip, websocket, excel, test
  │   └── mitedtsm-common (通用工具/枚举/异常)
  └── 18 个业务模块 (各含 api/ 接口 + biz/ 实现)
        ↓
中间件层: MySQL 8.0 | Redis 6 | RabbitMQ 3
```

## 3. 模块间通信

- 各模块通过 `module-xxx-api/` 暴露 Feign 风格接口 + DTO
- mitedtsm-common 定义通用接口 (OAuth2TokenCommonApi, TenantCommonApi)
- 严禁跨模块直接调用 Service

## 4. 部署架构

```
Docker Compose:
  mysql (3306) + redis (6379) + rabbitmq (5672)
  server (8080, Spring Boot, eclipse-temurin:17)
  frontend (80/443, Nginx 四虚拟主机:
    meession.com.cn → Web/
    mall.meession.com.cn → Mall/
    m.meession.com.cn → Mobile/
    portal.meession.com.cn → portal-web/)
```

## 5. 业务模块清单

| 模块 | 包名 | 功能 |
|------|------|------|
| system | module-system | 认证、用户、角色、菜单、部门、字典、租户 |
| infra | module-infra | 文件存储、配置管理、代码生成 |
| bpm | module-bpm | Flowable 工作流 |
| crm | module-crm | 客户关系管理 |
| erp | module-erp | 采购/销售/库存/财务 |
| mall | module-mall | 电商商城 |
| member | module-member | 会员管理 |
| pay | module-pay | 统一支付 |
| mp | module-mp | 微信公众号 |
| ai | module-ai | LLM/AI 集成 |
| report | module-report | 报表与 BI |
| wms | module-wms | 仓储管理 |
| mes | module-mes | 制造执行 |
| zatca | module-zatca | 沙特电子发票 |
| unified-product | module-unified-product | 统一产品管理 |
| overseas-service | module-overseas-service | 海外服务商城(GSM) |
| service-center | module-service-center | 服务中心 |
