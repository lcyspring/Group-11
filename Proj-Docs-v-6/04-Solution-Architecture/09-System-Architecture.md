# 系统架构设计文档

## MITEDTSM CRM 子系统

---

## 文档信息

| 项目 | 说明 |
|------|------|
| 项目名称 | MITEDTSM（密讯ETM系统）CRM 子系统 |
| 所属平台 | MIT-FMP 密讯基础平台 |
| 基础框架 | mitedtsm（Spring Boot 3.5.9 + JDK 17） |
| 架构模式 | 分层架构 + DDD 领域驱动 |
| 技术栈 | JDK17 + SpringBoot 3.5.9 + Vue3 + TypeScript |
| 参考来源 | 04-架构设计/System_Architecture.md |
| 文档版本 | V2.0 |
| 创建日期 | 2026-06-25 |
| 变更说明 | V2.0 — 模块优先级重排：CRM模块优先，HR模块后置（P2） |

---

## 1. 架构概述

本系统基于 mitedtsm 框架（PV100R20260511）进行扩展，**以CRM为核心**，在复用 system、infra、bpm、report 等基础模块的基础上，扩展CRM六大业务域（客户/销售/财务/工单/营销/办公协作）。HR模块（招聘/员工/考勤/绩效/薪酬）作为P2增值模块，优先级低于CRM。

### 1.1 设计原则

- **CRM优先**：业务开发资源优先保障CRM六大域，HR模块在有余力时投入
- **分层解耦**：严格遵循 interfaces → application → domain → infrastructure 四层架构
- **领域驱动**：以 DDD 方法论指导 CRM 模块划分与聚合设计
- **开闭原则**：对扩展开放，对修改关闭，CRM新模块通过依赖注入无缝集成
- **安全第一**：Sa-Token + RBAC + 数据权限 + 多租户隔离
- **高性能**：Redis 多级缓存 + 读写分离 + 异步处理

---

## 2. 分层架构图

```
┌─────────────────────────────────────────────────────────┐
│  Interfaces 接口层                                       │
│  Vue3 Admin Portal / Mobile H5 / OpenAPI / Knife4j       │
├─────────────────────────────────────────────────────────┤
│  Application 应用层                                       │
│  SystemApp / BpmApp / ── CRM核心应用层 ──               │
│  CustomerApp / SalesApp / FinanceApp / WorkOrderApp      │
│  MarketingApp / OfficeApp / ReportApp                    │
│  ── HR应用层(P2) ──                                     │
│  RecruitApp / EmployeeApp / AttendanceApp /              │
│  PerformanceApp / SalaryApp                              │
├─────────────────────────────────────────────────────────┤
│  Domain 领域层                                            │
│  ── CRM核心域 ──                                        │
│  Customer / Contact / Lead / Opportunity / Order         │
│  Quotation / Contract / Receivable / Invoice             │
│  Reimbursement / Refund / WorkOrder / Campaign           │
│  Broadcast / WorkReport / OaTask / Schedule              │
│  ── HR域(P2) ──                                         │
│  Resume / Employee / Attendance / Performance / Salary   │
├─────────────────────────────────────────────────────────┤
│  Infrastructure 基础设施层                                │
│  MySQL 8.0 / Redis 7.x / MinIO / Elasticsearch           │
│  Flowable 7.x / Sa-Token / MyBatis Plus / RabbitMQ       │
└─────────────────────────────────────────────────────────┘
```

---

## 3. 模块划分

### 3.1 现有模块（复用 mitedtsm）

| 模块 | 包路径 | 功能范围 | 状态 |
|------|--------|----------|------|
| system | com.meession.etm.module.system | 用户、角色、菜单、部门、字典、租户 | 复用 |
| infra | com.meession.etm.module.infra | 消息中心、短信、邮件、公告、文件 | 复用 |
| bpm | com.meession.etm.module.bpm | 流程定义、审批、待办、已办 | 复用 |
| report | com.meession.etm.module.report | 通用报表引擎、数据可视化 | 复用 |
| unified-product | com.meession.etm.module.unified-product | 统一商品管理、产品数据 | 复用 |

### 3.2 CRM新增模块（P0 — 优先交付）

| 模块 | 包路径 | 功能范围 | 状态 |
|------|--------|----------|------|
| crm-customer | com.meession.etm.module.crm.customer | 客户管理、联系人、公海、查重、客户分析 | 新增 |
| crm-sales | com.meession.etm.module.crm.sales | 线索、商机、订单、报价、合同、销售漏斗 | 新增 |
| crm-finance | com.meession.etm.module.crm.finance | 回款、发票、报销、退款、费用 | 新增 |
| crm-workorder | com.meession.etm.module.crm.workorder | 工单管理、SLA、状态流转 | 新增 |
| crm-marketing | com.meession.etm.module.crm.marketing | 营销活动、短信群发、邮件群发、客户关怀 | 新增 |
| crm-office | com.meession.etm.module.crm.office | 工作报告、任务、日程、文档、统一审批中心 | 新增 |

> **说明**：CRM模块可合并为 `mitedtsm-module-crm` 统一管理，上述子包在module-crm内部按DDD限界上下文组织。

### 3.3 HRM新增模块（P2 — 增值功能，不阻塞CRM交付）

| 模块 | 包路径 | 功能范围 | 状态 | 优先级 |
|------|--------|----------|------|--------|
| recruitment | com.meession.etm.module.recruitment | 简历管理、面试、题库、模板、黑名单 | 新增 | P2 |
| employee | com.meession.etm.module.employee | 花名册、入职、转正、调岗、晋升、离职、返聘 | 新增 | P2 |
| attendance | com.meession.etm.module.attendance | 打卡、异常、班次、假期、规则、统计 | 新增 | P2 |
| performance | com.meession.etm.module.performance | 绩效等级、计划、记录、薪资关联 | 新增 | P2 |
| salary | com.meession.etm.module.salary | 核算规则、字段管理、工资表、调薪 | 新增 | P2 |

---

## 4. 包结构设计（以CRM模块为核心）

```
com.meession.etm
├── module-system / module-system-biz       # 系统管理（复用）
├── module-infra / module-infra-biz         # 基础设施（复用）
├── module-bpm / module-bpm-biz             # 工作流（复用）
├── module-crm / module-crm-biz             # ★ CRM核心模块（新增）
│   ├── controller
│   │   ├── admin-api                       # 管理端API
│   │   │   ├── customer                    # 客户域
│   │   │   ├── sales                       # 销售域
│   │   │   ├── finance                     # 财务域
│   │   │   ├── workorder                   # 工单域
│   │   │   ├── marketing                   # 营销域
│   │   │   └── office                      # 办公域
│   │   └── app-api                         # 移动端API
│   ├── service                             # 应用服务 + 领域服务
│   │   ├── customer
│   │   ├── sales
│   │   ├── finance
│   │   ├── workorder
│   │   ├── marketing
│   │   └── office
│   ├── convert                             # MapStruct 对象转换
│   └── dal
│       ├── dataobject                      # DO 数据对象
│       │   ├── customer
│       │   ├── sales
│       │   ├── finance
│       │   ├── workorder
│       │   ├── marketing
│       │   └── office
│       └── mapper                          # MyBatis-Plus Mapper
├── module-recruitment / ...biz             # 招聘管理（P2，后置）
├── module-employee / ...biz                # 员工管理（P2，后置）
├── module-attendance / ...biz              # 考勤管理（P2，后置）
├── module-performance / ...biz             # 绩效管理（P2，后置）
├── module-salary / ...biz                  # 薪酬管理（P2，后置）
├── module-report / module-report-biz       # 报表引擎（复用）
├── module-unified-product                  # 统一商品（复用，支撑CRM报价）
├── mitedtsm-framework                      # 自定义 starters
└── mitedtsm-server                         # 主应用入口
```

---

## 5. 技术选型

### 5.1 后端技术栈（复用 mitedtsm）

| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 17 | 运行环境 |
| Spring Boot | 3.5.9 | 核心框架 |
| Sa-Token | 1.38+ | 认证鉴权 |
| MyBatis Plus | 3.5+ | ORM 框架 |
| Flowable | 7.x | 工作流引擎（CRM审批核心） |
| Knife4j | 4.x | API 文档 |
| RabbitMQ | 3.x | 消息队列（CRM群发/异步处理） |
| XXL-Job | 2.4+ | 定时任务（公海回收/逾期检测/客户关怀） |
| MapStruct | 1.5+ | 对象映射 |
| Lombok | 1.18+ | 代码简化 |
| Hutool | 5.8+ | 工具库 |

### 5.2 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5 | 前端框架 |
| TypeScript | 5.x | 类型安全 |
| Vite | 5.x | 构建工具 |
| Pinia | 2.x | 状态管理 |
| Element Plus | 2.x | UI 组件库 |
| ECharts | 5.x | 图表可视化（销售漏斗/客户分析） |
| Axios | 1.x | HTTP 客户端 |
| uni-app | 3.x | 移动端框架 (AdminMobileFrontend) |

### 5.3 中间件

| 技术 | 版本 | 用途 |
|------|------|------|
| MySQL | 8.0 | 关系型数据库（统一使用MySQL） |
| Redis | 7.x | 缓存与分布式锁 |
| MinIO | latest | 对象存储（CRM文档/导入导出文件） |
| Elasticsearch | 8.x | 搜索引擎（客户/线索全文检索） |
| Nginx | 1.24+ | 反向代理 |

---

## 6. 安全架构

### 6.1 认证鉴权

```
用户请求 → Nginx → Sa-Token 过滤器 → 权限校验 → CRM业务处理
                ↓
        Token 校验（Redis 存储）
                ↓
        RBAC 权限模型（用户 → 角色 → CRM菜单/按钮权限）
```

### 6.2 CRM数据权限设计

| 角色 | 数据范围 | CRM权限说明 |
|------|---------|------------|
| CRM_ADMIN | 全部CRM数据 | 全局客户/商机/订单管理 |
| SALES_MANAGER | 本部门及子部门 | 团队漏斗/业绩分析/审批 |
| SALES_REP | 仅本人 | 个人客户/商机/订单 |
| FINANCE | 全部财务数据 | 回款/发票/报销/退款 |
| SUPPORT | 已分配工单 | 工单处理/SLA |
| MARKETING | 营销相关数据 | 活动/群发/客户关怀 |

### 6.3 安全措施

- XSS 过滤
- SQL 注入防护（MyBatis Plus 参数化查询）
- CSRF Token
- 接口限流（Redis + Lua 脚本）
- 操作日志审计（@BizLog 注解）
- 敏感数据加密存储（BCrypt 密码加密）

---

## 7. 关键技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| CRM架构 | DDD领域驱动 | 六大业务域清晰独立，DDD最适合 |
| 认证框架 | Sa-Token | 轻量级、功能全面、已集成 |
| ORM | MyBatis Plus | 与现有系统一致，开发效率高 |
| 工作流 | Flowable | 社区活跃、已集成，CRM审批核心 |
| 对象存储 | MinIO | 兼容S3、自建成本低 |
| 任务调度 | XXL-Job | 公海回收、逾期检测等定时任务 |
| 搜索引擎 | Elasticsearch | 客户全文检索、线索搜索 |
| 数据库 | MySQL 8.0 | 统一标准，不得使用PostgreSQL |

---

## 8. 文档变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| V1.0 | 2026-06-25 | 初始版本，参考04-架构设计/System_Architecture.md | 架构设计团队 |
| V2.0 | 2026-06-25 | 模块优先级重排：CRM六大域前置（P0），HR模块后置（P2）；架构分层以CRM为核心 | 架构设计团队 |
