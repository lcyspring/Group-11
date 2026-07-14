# DDD 限界上下文 - 密讯ETM系统 (mitedtsm)

---

## 文档信息

| 字段 | 内容 |
|------|------|
| 项目名称 | 密讯ETM企业管理系统 (mitedtsm) |
| 文档版本 | v1.0 |
| 创建日期 | 2026-06-25 |
| 负责人 | 架构团队 |
| 状态 | 已发布 |

---

## 一、限界上下文总览

基于领域分析结果，将密讯ETM系统划分为 **16 个限界上下文**。

| 编号 | 上下文名称 | 所属模块 | 类型 |
|------|-----------|----------|------|
| BC-01 | 用户与权限上下文 | module-system | 支撑 |
| BC-02 | 租户管理上下文 | module-system | 支撑 |
| BC-03 | 客户管理上下文 | module-crm | 核心 |
| BC-04 | 商机管理上下文 | module-crm | 核心 |
| BC-05 | 订单管理上下文 | module-crm + module-erp | 核心 |
| BC-06 | 采购管理上下文 | module-erp | 核心 |
| BC-07 | 库存管理上下文 | module-erp + module-wms | 核心 |
| BC-08 | 财务管理上下文 | module-erp | 核心 |
| BC-09 | 商城上下文 | module-mall | 核心 |
| BC-10 | 会员上下文 | module-member | 核心 |
| BC-11 | 生产制造上下文 | module-mes | 核心 |
| BC-12 | 仓库管理上下文 | module-wms | 核心 |
| BC-13 | 工作流引擎上下文 | module-bpm | 支撑 |
| BC-14 | 统一支付上下文 | module-pay | 支撑 |
| BC-15 | AI 服务上下文 | module-ai | 支撑 |
| BC-16 | 基础设施上下文 | module-infra | 通用 |

---

## 二、上下文关系类型

| 缩写 | 全称 | 说明 | 应用场景 |
|------|------|------|----------|
| **ACL** | Anti-Corruption Layer | 防腐层，隔离外部模型 | 支付→第三方渠道、ZATCA→税务系统 |
| **OHS** | Open Host Service | 开放主机服务，提供统一 API | BPM、支付、文件服务 |
| **CS** | Customer-Supplier | 客户-供应商关系 | 商机→订单、订单→财务 |
| **SK** | Shared Kernel | 共享内核 | 用户↔租户、ERP↔WMS 共享 Inventory |
| **CF** | Conformist | 随者 | 下游完全跟随上游 |

---

## 三、核心上下文详细设计

### BC-01：用户与权限上下文

**边界**：管理用户身份、认证、授权、角色、菜单、权限。

| 元素 | 说明 |
|------|------|
| **聚合根** | User, Role, Menu |
| **值对象** | Password, UserStatus |
| **领域服务** | AuthDomainService |
| **包路径** | `com.meession.etm.module.system` |

### BC-03：客户管理上下文

**边界**：管理客户档案、联系人、公海规则。

| 元素 | 说明 |
|------|------|
| **聚合根** | Customer, CustomerSeaPool |
| **实体** | Customer, Contact, FollowUpRecord |
| **值对象** | Address, CustomerLevel, CustomerSource |
| **领域服务** | CustomerTransferDomainService |
| **包路径** | `com.meession.etm.module.crm` |

### BC-04：商机管理上下文

**边界**：管理商机创建、阶段推进、销售漏斗。

| 元素 | 说明 |
|------|------|
| **聚合根** | Opportunity |
| **实体** | Opportunity, QuotationItem |
| **值对象** | SalesStage, Money, WinRate |
| **领域服务** | OpportunityConversionDomainService |

### BC-05：订单管理上下文

**边界**：管理报价、合同、订单、发货。

| 元素 | 说明 |
|------|------|
| **聚合根** | Order, Contract |
| **实体** | Order, OrderItem, Contract |
| **值对象** | Money, OrderStatus, ContractStatus |
| **领域服务** | OrderDomainService |

### BC-06：采购管理上下文

**边界**：管理供应商、采购订单、入库验收。

| 元素 | 说明 |
|------|------|
| **聚合根** | PurchaseOrder, Supplier |
| **实体** | PurchaseOrder, PurchaseOrderItem, Supplier |
| **值对象** | Money, PurchaseStatus |

### BC-07：库存管理上下文

**边界**：管理库存、仓库、入库、出库、盘点。

| 元素 | 说明 |
|------|------|
| **聚合根** | Inventory, Warehouse |
| **实体** | Inventory, InventoryTransaction, Warehouse |
| **值对象** | Quantity, StockStatus, WarehouseLocation |

### BC-11：生产制造上下文

**边界**：管理生产工单、工艺路线、报工质检。

| 元素 | 说明 |
|------|------|
| **聚合根** | WorkOrder, Routing |
| **实体** | WorkOrder, Routing, Operation, QualityCheck |
| **值对象** | WorkOrderStatus, OperationType |

### BC-12：仓库管理上下文

**边界**：管理仓库、库位、拣货、波次、盘点。

| 元素 | 说明 |
|------|------|
| **聚合根** | Warehouse, PickTask |
| **实体** | Warehouse, Location, InboundOrder, OutboundOrder, PickTask |
| **值对象** | LocationCode, PickStatus |

### BC-13：工作流引擎上下文

**边界**：管理流程定义、流程实例、任务分配（基于 Flowable）。

| 元素 | 说明 |
|------|------|
| **聚合根** | ProcessDefinition, ProcessInstance, Task |
| **值对象** | ApprovalAction, TaskStatus |
| **领域服务** | ApprovalDomainService |

### BC-14：统一支付上下文

**边界**：管理支付订单、退款、对账（对接微信/支付宝）。

| 元素 | 说明 |
|------|------|
| **聚合根** | PaymentOrder, RefundOrder |
| **值对象** | Money, PayChannel, PayStatus |
| **领域服务** | PaymentDomainService |

---

## 四、上下文映射图

```
                          ┌──────────────────┐
                          │ BC-01: 用户与权限  │
                          │ BC-02: 租户管理   │
                          └────────┬─────────┘
                                   │ ACL (被所有上下文依赖)
                                   │
    ┌──────────────┬───────────────┼───────────────┬──────────────┐
    │              │               │               │              │
    ▼              ▼               ▼               ▼              ▼
┌────────┐   ┌────────┐     ┌────────┐     ┌────────┐     ┌────────┐
│BC-03   │   │BC-04   │     │BC-05   │     │BC-06   │     │BC-07   │
│客户管理 │   │商机管理 │ CS  │订单管理 │ CS  │采购管理 │ CS  │库存管理 │
└───┬────┘   └───┬────┘ ───▶└───┬────┘ ───▶└───┬────┘ ───▶└───┬────┘
    │            │               │               │               │
    │            │               ▼               │               │
    │            │         ┌────────┐            │               │
    │            │         │BC-08   │◄───────────┘               │
    │            │         │财务管理 │                            │
    │            │         └────────┘                            │
    │            │                                               │
    ▼            ▼                                               ▼
┌────────┐  ┌────────┐                                     ┌────────┐
│BC-09   │  │BC-10   │                                     │BC-12   │
│商城     │  │会员     │                                     │仓库管理 │
└───┬────┘  └────────┘                                     └───┬────┘
    │                                                          │
    │                      共享上下文                           │
    ▼              ┌──────────────────────┐                    │
┌────────┐         │BC-13: 工作流引擎 (BPM)│◄───────────────────┘
│BC-14   │         │BC-16: 基础设施 (Infra)│
│统一支付 │         └──────────────────────┘
└────────┘
```

---

## 五、分层架构约定

每个限界上下文采用 DDD 四层架构：

```
com.meession.etm.module.{context}/
├── controller/    # 接口层（Controller, DTO）
├── service/       # 应用服务层（ApplicationService, Assembler → 对应领域层的应用服务）
├── dal/           # 数据访问层
│   ├── dataobject/   # DO 类（继承 TenantBaseDO/BaseDO）
│   └── mapper/       # MyBatis-Plus Mapper
├── convert/       # MapStruct 转换器
├── enums/         # 枚举常量
├── framework/     # 模块级框架工具
├── job/           # 定时任务
└── util/          # 工具类
```

> 注：项目当前采用 MyBatis-Plus 数据层模式，DO 类同时承担实体和数据对象的角色。领域模型的核心行为通过 Service 层和聚合设计约束来体现。

---

> 下一步：基于限界上下文，进行聚合设计。
