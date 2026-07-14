# 模块清单与状态

## 1. 框架层 (mitedtsm-framework)

| Starter | 说明 | 状态 |
|---------|------|------|
| mitedtsm-common | 通用工具/枚举/异常/CommonApi | 稳定 |
| web | Spring MVC + Jackson + 全局异常处理 | 稳定 |
| security | OAuth2 + Spring Security + 权限注解 | 稳定 |
| mybatis | MyBatis-Plus + Druid + 多数据源 | 稳定 |
| redis | Redisson + Cache + 分布式锁 | 稳定 |
| mq | RabbitMQ/RocketMQ/Kafka 适配 | 稳定 |
| job | 定时任务 (XXL-JOB 风格) | 稳定 |
| monitor | Spring Boot Admin + SkyWalking | 稳定 |
| protection | 防重放/限流 | 稳定 |
| tenant | 多租户 SQL 拦截器 | 稳定 |
| data-permission | 数据权限 (@DataPermission) | 稳定 |
| ip | IP 解析 | 稳定 |
| websocket | WebSocket 支持 | 稳定 |
| excel | FastExcel 导入导出 | 稳定 |
| test | 单元测试基类 | 稳定 |

## 2. 业务模块

| 模块 | 状态 | API子模块 | 核心表数量(约) |
|------|------|-----------|--------------|
| system | 核心/稳定 | ✓ | 30+ |
| infra | 稳定 | ✓ | 10+ |
| bpm | 稳定 | ✓ | Flowable 表 + 扩展 |
| crm | 稳定 | ✓ | 20+ |
| erp | 稳定 | ✓ | 30+ |
| mall | 稳定 | ✓ | 25+ |
| member | 稳定 | ✓ | 10+ |
| pay | 稳定 | ✓ | 10+ |
| mp | 稳定 | ✓ | 5+ |
| ai | 稳定 | ✓ | 10+ |
| report | 稳定 | ✓ | 5+ |
| wms | 稳定 | ✓ | 15+ |
| mes | 稳定 | ✓ | 20+ |
| zatca | 稳定 | ✓ | 10+ |
| unified-product | 稳定 | ✓ | 10+ |
| overseas-service | GSM MVP完成 | ✓ | 91 DDL |
| service-center | 开发中 | ✓ | 待定 |

## 3. 前端应用

| 应用 | 目录 | 框架 | 状态 |
|------|------|------|------|
| Admin Web | Code/Web/ | Vue 3.5 + Vite 5 + Element Plus | 稳定 |
| Admin Mobile | Code/AdminMobileFrontend/ | uni-app 3 + wot-design-uni | 稳定 |
| Mall Frontend | Code/MallFrontend/ | uni-app (HBuilderX) | 稳定 |
| Mall Android | Code/MallAndroid/ | Android 原生 | 稳定 |
| Portal Web | Code/portal-web/ | Vue 3 + Tailwind | 稳定 |
