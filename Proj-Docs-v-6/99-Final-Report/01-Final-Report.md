# MITEDTSM CRM 子系统 - 架构分析总报告

## 1. 项目概述

本报告基于 MITEDTSM（密讯）平台现有系统分析 + CRM 原型分析，输出了完整的 DDD 领域驱动设计方案和工程实施计划。

**目标**: 在 MITEDTSM 平台基础上构建 CRM + OA 子系统。

## 2. 分析成果

### 2.1 现有系统分析 (10 docs)
- Spring Boot 3.5.9 + MyBatis Plus + 18 业务模块
- 多租户 (TenantBaseDO), RBAC权限, OAuth2认证
- BPM审批流 (Flowable 7.2.0), 操作日志, 文件存储
- Vue 3 + Element Plus 管理后台 + uni-app 移动端

### 2.2 原型分析 (8 docs, 7 CRM域)
- **客户域**: 客户/联系人/公海机制
- **商机域**: 商机阶段/产品报价/销售漏斗
- **订单域**: 订单/产品行/审批流
- **财务域**: 回款/发票/报销/退款
- **工单域**: 工单处理流程
- **营销域**: 活动/短信/邮件群发/客户关怀
- **OA域**: 请假/出差/借款/拜访/报告/任务

### 2.3 DDD 方案 (4 docs)
- 7个限界上下文 + 1个共享审批域
- 15+ 聚合根，明确聚合边界
- 30+ 领域事件，支持跨域协作
- 通过 ID 引用 + 事件实现最终一致性

### 2.4 工程计划
- 10 Sprints (21周) 
- 4名开发 + 1测试 + 0.5 DevOps
- 复用70%+现有能力

## 3. 核心设计决策

| 决策 | 方案 | 理由 |
|------|------|------|
| 模块组织 | 统一放入 module-crm (非多模块拆分) | 避免过多微模块，简化依赖 |
| 审批流 | 复用 Flowable BPM | 已有成熟集成，支持灵活配置 |
| 公海机制 | 定时任务批量处理 | 简单可靠，用户量不大时可行 |
| 前端方案 | 作为 Web Admin 的 views/crm/ 扩展 | 复用路由/权限/请求封装 |
| 数据库 | 共享现有 MySQL，crm_ 前缀 | 复用连接池和多租户拦截器 |

## 4. 技术架构总览

```
┌──────────────────────────────────────────────┐
│                 Frontend (Vue 3)             │
│   Web Admin (views/crm/) + Portal + Mobile   │
├──────────────────────────────────────────────┤
│           Controller Layer (REST)            │
│    /admin-api/crm/*   |   /app-api/crm/*     │
├──────┬──────┬──────┬──────┬──────┬───────────┤
│Customer│Oppty │Order │Finance│WO   │Marketing │ OA │
│ Context│Ctx   │Ctx   │Ctx   │Ctx  │Ctx       │ Ctx│
├──────┴──────┴──────┴──────┴──────┴───────────┤
│          Shared: Approval (BPM)              │
├──────────────────────────────────────────────┤
│  Framework: Security, Tenant, MyBatis, Redis, MQ│
├──────────────────────────────────────────────┤
│  MySQL 8.0 | Redis 6 | RabbitMQ 3           │
└──────────────────────────────────────────────┘
```

## 5. 数据库规模估算

| 域 | 核心表 | 预估表数(含关联/日志) |
|----|--------|---------------------|
| 客户域 | crm_customer, crm_contact, crm_customer_sea | 5 |
| 商机域 | crm_opportunity, crm_quotation_item, crm_follow_up | 5 |
| 订单域 | crm_order, crm_order_item | 4 |
| 财务域 | crm_receipt, crm_invoice, crm_reimbursement, crm_refund | 8 |
| 工单域 | crm_work_order | 3 |
| 营销域 | crm_campaign, crm_sms_broadcast, crm_email_broadcast, crm_customer_care | 6 |
| OA域 | crm_leave, crm_trip, crm_loan, crm_visit, crm_work_report, crm_task | 8 |
| **合计** | | **~39 表** |

## 6. 下一步行动

1. 对比现有 `module-crm` 表结构，确定复用策略
2. 创建 `mitedtsm-module-crm` 模块骨架
3. Sprint 1 启动: 客户域开发
4. 数据库 DDL 编写 (InstallPackage/database/new/crm/)
5. 菜单+权限数据准备 (system_menu + system_menu_i18n)

---

*报告生成时间: 2026-03-31*
*基于: MITEDTSM 2026.01-SNAPSHOT + PROTOTYPE CRM原型*
*方法: TRAE 5-Agent 并行分析 + DDD 领域驱动设计*
