# DDD 聚合设计 - 密讯ETM系统 (mitedtsm)

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

## 一、聚合设计原则

1. **单一聚合根**：每个聚合只有一个聚合根，外部只能通过聚合根访问聚合内部实体
2. **ID 引用**：聚合间通过唯一标识符（ID）引用，而非对象引用
3. **事务边界**：一个事务只修改一个聚合实例
4. **不变量**：聚合根负责维护聚合内部所有业务不变量
5. **小聚合**：聚合设计尽量小，减少并发冲突

---

## 二、核心聚合设计

### 2.1 UserAggregate（用户聚合）

**所属上下文**：BC-01 用户与权限  
**聚合根**：User

```
User (聚合根)
├── id: Long (自增)
├── username: String (全局唯一)
├── password: Password (值对象 - BCrypt加密)
├── nickName: String
├── email: String
├── phone: String
├── status: UserStatus (值对象: ACTIVE/DISABLED/LOCKED)
├── deptId: Long (引用 Dept)
└── tenantId: Long

内部实体: UserRole (userId + roleId)
引用: RoleAggregate (通过 roleId), DeptAggregate (通过 deptId)

不变量:
- 用户名全局唯一
- 密码必须满足复杂度要求
- 用户状态为禁用时不能登录
- 一个用户至少有一个角色
```

**Java 代码示例**：

```java
package com.meession.etm.module.system.dal.dataobject;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_user")
public class UserDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;     // 全局唯一
    private String password;     // BCrypt 加密
    private String nickName;
    private String email;
    private String phone;
    private Integer status;      // 0:正常 1:禁用 2:锁定
    private Long deptId;

    // 审计字段由 TenantBaseDO/BaseDO 提供
}
```

### 2.2 CustomerAggregate（客户聚合）

**所属上下文**：BC-03 客户管理  
**聚合根**：Customer

```
Customer (聚合根)
├── id: Long
├── name: String
├── industry: String
├── level: CustomerLevel (值对象: VIP/GOLD/SILVER/BRONZE/NORMAL)
├── source: CustomerSource (值对象)
├── ownerUserId: Long (引用 User)
├── status: Integer
├── inSea: Boolean (公海标志)
├── address: Address (值对象)
└── tenantId: Long

内部实体: Contact[] (联系人), FollowUpRecord[] (跟进记录)

不变量:
- 客户名称不能为空
- 客户未跟进 N 天后自动掉入公海
- 客户转移归属后原负责人失去访问权限
```

### 2.3 OpportunityAggregate（商机聚合）

**所属上下文**：BC-04 商机管理  
**聚合根**：Opportunity

```
Opportunity (聚合根)
├── id: Long
├── title: String
├── customerId: Long (引用 Customer)
├── stage: SalesStage (值对象: LEAD→QUALIFIED→PROPOSAL→NEGOTIATION→WON/LOST)
├── totalAmount: BigDecimal
├── expectedCloseDate: LocalDate
├── winRate: BigDecimal
├── ownerUserId: Long
└── tenantId: Long

内部实体: QuotationItem[] (报价行)

状态机:
LEAD → QUALIFIED → PROPOSAL → NEGOTIATION → WON (不可回退)
                                           → LOST

不变量:
- 商机阶段只能向前流转
- 赢单金额不能为 0
- 商机必须关联客户
```

### 2.4 OrderAggregate（订单聚合）

**所属上下文**：BC-05 订单管理  
**聚合根**：Order

```
Order (聚合根)
├── id: Long
├── orderNo: String (全局唯一)
├── customerId: Long (引用 Customer)
├── opportunityId: Long (引用 Opportunity, 可为空)
├── totalAmount: BigDecimal
├── status: OrderStatus (DRAFT→PENDING→PAID→COMPLETED/CANCELLED)
├── ownerUserId: Long
└── tenantId: Long

内部实体: OrderItem[] (订单行)

状态机:
DRAFT → PENDING → PAID → COMPLETED
  ↓        ↓
CANCELLED (仅草稿/待支付可取消)

不变量:
- 订单金额必须大于 0
- 已支付订单不可取消
- 订单必须关联客户
```

### 2.5 InventoryAggregate（库存聚合）

**所属上下文**：BC-07 库存管理  
**聚合根**：Inventory

```
Inventory (聚合根)
├── id: Long
├── productId: Long (引用统一商品)
├── warehouseId: Long (引用仓库)
├── quantity: BigDecimal
├── lockedQuantity: BigDecimal (锁定库存)
└── tenantId: Long

不变量:
- 可用库存 = quantity - lockedQuantity >= 0
- 出库时 lockedQuantity 不能超过 quantity
```

### 2.6 PaymentAggregate（支付聚合）

**所属上下文**：BC-14 统一支付  
**聚合根**：PaymentOrder

```
PaymentOrder (聚合根)
├── id: Long
├── paymentNo: String
├── businessType: String (订单/充值/退款)
├── businessId: Long
├── amount: BigDecimal
├── channel: PayChannel (WECHAT/ALIPAY)
├── status: PayStatus (PENDING→PAYING→SUCCESS/FAILED/CLOSED)
├── callbackTime: LocalDateTime
└── tenantId: Long

不变量:
- 支付金额必须与业务单据金额一致
- 支付回调必须验证签名
- 已成功支付不可重复回调
```

### 2.7 WorkOrderAggregate（生产工单聚合）

**所属上下文**：BC-11 生产制造  
**聚合根**：WorkOrder

```
WorkOrder (聚合根)
├── id: Long
├── workOrderNo: String
├── productId: Long
├── quantity: BigDecimal
├── routingId: Long (引用工艺路线)
├── status: WorkOrderStatus (CREATED→RELEASED→IN_PROGRESS→COMPLETED)
├── startDate/endDate: LocalDate
└── tenantId: Long

内部实体: OperationRecord[] (工序记录), QualityCheck[] (质检记录)

状态机:
CREATED → RELEASED → IN_PROGRESS → COMPLETED
                           ↓
                       CANCELLED
```

### 2.8 ApprovalAggregate（审批聚合）

**所属上下文**：BC-13 工作流引擎  
**聚合根**：ApprovalTask

```
ApprovalTask (聚合根)
├── id: Long
├── processInstanceId: Long
├── taskName: String
├── assigneeId: Long (引用 User)
├── action: ApprovalAction (APPROVED/REJECTED/DELEGATED)
├── comment: String
├── status: TaskStatus (PENDING/COMPLETED)
└── processTime: LocalDateTime

不变量:
- 审批任务只能由指定审批人处理
- 已处理任务不可重复审批
- 驳回后需要重新提交流程
```

---

## 三、聚合间引用规则

```
聚合间永远通过 ID 引用，不使用对象引用。
需要跨聚合操作时，通过 领域服务 或 领域事件 实现。
一个事务只修改一个聚合实例，最终一致性由领域事件保证。

示例：
- Order.customerId → Customer.id (ID引用)
- PaymentOrder.businessId → Order.id (ID引用)
- Opportunity.convertToOrder() → 发布 OpportunityWonEvent → Order Context 监听创建订单
```

---

## 四、聚合持久化约定

| 约定 | 说明 |
|------|------|
| DO 类 | 使用 `@TableName` 映射数据库表名 |
| 主键 | `@TableId(type = IdType.AUTO)` MySQL 自增 |
| 多租户 | 租户表 DO 继承 `TenantBaseDO`（自动注入 tenantId） |
| 共享表 | 非多租户表 DO 继承 `BaseDO` |
| 逻辑删除 | `@TableLogic` 标记 deleted 字段 |
| 审计字段 | createBy/createTime/updateBy/updateTime 自动填充 |

---

> 下一步：基于聚合设计，进行实体详细设计。
