#DDD 领域驱动设计

## 学习目标

- 理解 DDD 战略设计核心概念
- 掌握限界上下文的划分方法
- 学会识别聚合、实体和值对象
- 能够绘制领域模型图

## 前置知识与课时

- **前置知识**：面向对象设计、UML 类图


---

## 核心内容

### 3.1 什么是 DDD

领域驱动设计（Domain-Driven Design）是一种以业务领域为核心建模的软件设计方法，由 Eric Evans 在 2003 年提出。

**核心思想**：软件设计应该反映业务领域的真实结构，而不是技术实现。

### 3.2 DDD 战略设计

#### 限界上下文（Bounded Context）

限界上下文是 DDD 最重要的概念，它定义了特定模型的适用范围。

```
密讯ETM 系统的限界上下文：

┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ 系统管理  │ │   CRM    │ │   ERP    │ │   BPM    │ │   Mall   │
│ (system)  │ │  (crm)   │ │  (erp)   │ │  (bpm)   │ │  (mall)  │
│ ──────── │ │ ──────── │ │ ──────── │ │ ──────── │ │ ──────── │
│ - 用户   │ │ - 客户   │ │ - 产品   │ │ - 流程   │ │ - 商品   │
│ - 角色   │ │ - 商机   │ │ - 采购   │ │ - 任务   │ │ - 订单   │
│ - 菜单   │ │ - 报价   │ │ - 销售   │ │ - OA审批 │ │ - 营销   │
│ - 租户   │ │ - 跟进   │ │ - 库存   │ │ - 分类   │ │ - 物流   │
│ - 部门   │ │ - 合同   │ │ - 财务   │ │          │ │          │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘

┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│   Pay    │ │   WMS    │ │   MES    │ │   AI     │ │ Overseas │
│  (pay)   │ │  (wms)   │ │  (mes)   │ │  (ai)    │ │(overseas)│
│ ──────── │ │ ──────── │ │ ──────── │ │ ──────── │ │ ──────── │
│ - 支付   │ │ - 仓库   │ │ - 工单   │ │ - 聊天   │ │ - 跨境   │
│ - 退款   │ │ - 库存   │ │ - 工序   │ │ - 绘图   │ │ - 多语言 │
│ - 渠道   │ │ - 出入库 │ │ - 质量   │ │ - 知识库 │ │ - 海外   │
│ - 账单   │ │ - 盘点   │ │ - 排产   │ │ - MCP    │ │ - ZATCA  │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

#### 上下文映射（Context Map）

```
┌──────────┐   共享内核    ┌──────────┐
│   系统管理 │◄────────────►│   CRM    │
│  (system) │              │  (crm)   │
└─────┬─────┘              └─────┬─────┘
      │ 上游/下游                 │ 上游/下游
      │                           │
┌─────┴─────┐              ┌─────┴─────┐
│    ERP    │               │    Mall   │
│   (erp)   │               │   (mall)  │
└─────┬─────┘              └─────┬─────┘
      │                           │
      │  防腐层                    │  开放主机服务
      │                           │
┌─────┴─────┐              ┌─────┴─────┐
│    WMS    │               │    Pay    │
│   (wms)   │               │   (pay)   │
└───────────┘              └───────────┘
```

### 3.3 DDD 战术设计

#### 实体（Entity）

具有唯一标识的对象，标识在其生命周期中保持不变。

```java
// 用户实体（位于 com.meession.etm.module.system）
public class UserDO extends TenantBaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;           // 唯一标识
    private String username;   // 用户名（业务标识）
    private String password;
    private String nickname;
    private Integer status;    // 状态: 0正常 1禁用

    // 领域行为
    public void disable() {
        this.status = 1;
    }

    public boolean isActive() {
        return this.status == 0;
    }
}
```

#### 值对象（Value Object）

没有唯一标识，通过属性值来定义相等性。

```java
// 地址值对象
public class Address {
    private final String province;
    private final String city;
    private final String district;
    private final String detail;

    // 不可变，无 setter
    // equals/hashCode 基于所有属性
}
```

#### 聚合（Aggregate）

一组相关对象的集合，有一个聚合根。

```
用户聚合（User Aggregate）
│
├── 聚合根: UserDO
│   ├── id: Long
│   ├── username: String
│   └── status: Integer
│
└── 实体: UserProfileDO
    ├── avatar: String
    ├── email: String
    └── mobile: String
```

#### 领域事件（Domain Event）

```java
// 用户创建事件
public class UserCreatedEvent {
    private final Long userId;
    private final String username;
    private final Long tenantId;
    private final LocalDateTime occurredOn;

    public UserCreatedEvent(Long userId, String username, Long tenantId) {
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.occurredOn = LocalDateTime.now();
    }
}
```

### 3.4 密讯ETM系统聚合设计

| 限界上下文 | 聚合 | 聚合根 | 核心实体/值对象 |
|-----------|------|--------|---------------|
| 系统管理 | 用户聚合 | UserDO | UserDO, RoleDO |
| 系统管理 | 角色聚合 | RoleDO | RoleDO, MenuDO |
| 系统管理 | 租户聚合 | TenantDO | TenantDO, TenantPackageDO |
| 系统管理 | 部门聚合 | DeptDO | DeptDO, PostDO |
| CRM | 客户聚合 | CustomerDO | CustomerDO, FollowUpRecordDO |
| CRM | 商机聚合 | OpportunityDO | OpportunityDO, StageChangeDO |
| ERP | 产品聚合 | ProductDO | ProductDO, ProductSkuDO |
| ERP | 订单聚合 | OrderDO | OrderDO, OrderItemDO |
| BPM | 流程聚合 | ProcessInstance | ProcessInstance, ApprovalRecord |
| Mall | 商品聚合 | MallProductDO | MallProductDO, MallSkuDO |
| Mall | 订单聚合 | MallOrderDO | MallOrderDO, MallOrderItemDO |
| Pay | 支付聚合 | PayOrderDO | PayOrderDO, PayRefundDO |
| WMS | 库存聚合 | StockDO | StockDO, WarehouseDO |
| MES | 工单聚合 | WorkOrderDO | WorkOrderDO, ProcessRouteDO |
| AI | 知识库聚合 | KnowledgeBaseDO | KnowledgeBaseDO, KnowledgeDocDO |

### 3.5 多租户设计要点

密讯ETM的多租户体系：

```
┌──────────────────────────────────────┐
│         数据库级租户隔离              │
│                                      │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐│
│  │ 租户A   │ │ 租户B   │ │ 租户C   ││
│  │ tenant  │ │ tenant  │ │ tenant  ││
│  │  = 1    │ │  = 2    │ │  = 3    ││
│  └─────────┘ └─────────┘ └─────────┘│
│                                      │
│  BaseDO ──── 无需租户隔离的共享表     │
│  TenantBaseDO ─ 需要租户隔离的业务表  │
│                                      │
│  注解: @TenantIgnore 跳过租户过滤     │
└──────────────────────────────────────┘
```

---

## 实战练习

### 练习 1：识别聚合

为密讯ETM系统的"WMS仓库管理"模块设计聚合，识别聚合根和边界。

### 练习 2：领域事件

列出"商城下单"流程可能触发的领域事件（包含跨限界上下文的事件）。

### 练习 3：上下文映射

画出 System-CRM-ERP-Pay 四个限界上下文之间的上下文映射图，标注关系类型。

---

## 常见问题

**Q：一个聚合应该多大？**

聚合应该尽可能小。一个好的经验法则是：一个事务只修改一个聚合实例。如果发现需要同时修改两个聚合，说明聚合划分可能有问题。

**Q：实体和值对象如何区分？**

关键问题："这个对象有独立的生命周期吗？" 如果有，就是实体；如果只是描述另一个对象，就是值对象。比如"地址"不独立于"人"存在，所以是值对象。

**Q：密讯ETM的租户隔离策略是什么？**

采用数据库级别隔离：每个业务表包含 `tenant_id` 字段，通过 MyBatis-Plus 拦截器自动拼接。共享表（如系统配置）使用 `BaseDO`，业务表使用 `TenantBaseDO`。

---

## 小结

- 限界上下文是 DDD 战略设计的核心
- 实体有唯一标识，值对象没有
- 聚合是一致性边界，遵循"一次事务修改一个聚合"
- 密讯ETM划分为 15 个限界上下文配合多租户架构

---

## 参考资料

- 《领域驱动设计》Eric Evans
- 《实现领域驱动设计》Vaughn Vernon
- DDD 社区：https://www.domainlanguage.com/
