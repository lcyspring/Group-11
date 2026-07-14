# DDD 实体设计 - 密讯ETM系统 (mitedtsm)

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

## 一、实体设计原则

1. **唯一标识**：每个实体拥有全局唯一标识符（MySQL 自增 ID）
2. **可变性**：实体是可变的，属性可以随时间改变
3. **连续性**：实体具有生命周期，通过状态机管理状态流转
4. **DO 承载**：数据对象（DO）类同时承担实体和数据持久化角色

---

## 二、标识符策略

| 策略 | 适用场景 | 示例 |
|------|----------|------|
| MySQL 自增 ID | 所有内部实体 | `@TableId(type = IdType.AUTO) Long id` |
| 业务编码 | 对外唯一标识 | `String orderNo`, `String customerNo` |
| UUID | 全局唯一标识 | `String uuid` |

---

## 三、核心实体设计

### 3.1 User（用户实体）

**所属聚合**：UserAggregate | **标识符**：Long id + String username（业务唯一）

**状态机**：`ACTIVE → DISABLED → ACTIVE` / `ACTIVE → LOCKED → ACTIVE`

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_user")
public class UserDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;        // 全局唯一
    private String password;        // BCrypt 加密
    private String nickName;
    private String email;
    private String phone;
    private Integer sex;
    private String avatar;
    private Integer status;         // 0:正常 1:禁用 2:锁定
    private Long deptId;
    private String loginIp;
    private LocalDateTime loginDate;
    private String remark;

    // 领域行为
    public boolean isActive() {
        return Integer.valueOf(0).equals(this.status);
    }

    public void updateLoginInfo(String ip) {
        this.loginIp = ip;
        this.loginDate = LocalDateTime.now();
    }
}
```

### 3.2 Customer（客户实体）

**所属聚合**：CustomerAggregate | **所属上下文**：BC-03

**状态**：无硬状态机，通过 `inSea`（公海标志）和 `status` 管理。

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_customer")
public class CustomerDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String customerName;    // 客户名称
    private String customerNo;      // 客户编号（全局唯一）
    private String industry;        // 行业
    private String scale;           // 规模
    private Integer level;          // 等级: 5-VIP 4-金牌 3-银牌 2-铜牌 1-普通
    private Integer source;         // 来源: 1-官网 2-展会 3-推荐 4-广告 5-其他
    private Long ownerUserId;       // 归属人ID
    private String ownerUserName;   // 归属人姓名
    private String province;
    private String city;
    private String detailAddress;
    private Boolean inSea;          // 是否在公海
    private Boolean active;         // 是否活跃
    private String lostReason;      // 流失原因
    private String remark;
}
```

### 3.3 Opportunity（商机实体）

**所属聚合**：OpportunityAggregate | **所属上下文**：BC-04

**状态机**：`LEAD → QUALIFIED → PROPOSAL → NEGOTIATION → WON / LOST`

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity")
public class OpportunityDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String opportunityName;
    private Long customerId;        // 引用 Customer
    private Integer stage;          // 阶段: 1-线索 2-已验证 3-方案 4-谈判 5-赢单 6-丢单
    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private LocalDate expectedCloseDate;
    private LocalDateTime winDate;
    private String lostReason;
    private Long ownerUserId;
    private Integer winRate;        // 赢单率(%)

    // 领域行为
    public boolean isWon() {
        return Integer.valueOf(5).equals(this.stage);
    }
}
```

### 3.4 Order（订单实体）

**所属聚合**：OrderAggregate | **所属上下文**：BC-05

**状态机**：`DRAFT → PENDING → PAID → COMPLETED` / `DRAFT/PENDING → CANCELLED`

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_order")
public class OrderDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;         // 订单编号（全局唯一）
    private String title;
    private Long customerId;        // 引用 Customer
    private Long opportunityId;     // 引用 Opportunity（可为空）
    private BigDecimal totalAmount;
    private Integer status;         // 0-草稿 1-待支付 2-已支付 3-已完成 4-已取消
    private String cancelReason;
    private String contractNo;
    private LocalDateTime signDate;
    private Long ownerUserId;
    private String remark;

    // 领域行为
    public boolean isPaid() {
        return Integer.valueOf(2).equals(this.status);
    }

    public boolean canCancel() {
        int s = this.status == null ? 0 : this.status;
        return s == 0 || s == 1; // 仅草稿/待支付可取消
    }
}
```

### 3.5 PurchaseOrder（采购单实体）

**所属上下文**：BC-06

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_purchase_order")
public class PurchaseOrderDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String purchaseNo;
    private Long supplierId;
    private BigDecimal totalAmount;
    private Integer status;         // 0-草稿 1-待审批 2-已审批 3-已入库 4-已取消
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private Long purchaserId;
    private String remark;
}
```

### 3.6 Inventory（库存实体）

**所属上下文**：BC-07

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_inventory")
public class InventoryDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;         // 引用统一商品
    private Long warehouseId;       // 引用仓库
    private BigDecimal quantity;    // 库存数量
    private BigDecimal lockedQuantity; // 锁定库存
    private BigDecimal minStock;    // 最低库存警戒
    private BigDecimal maxStock;    // 最高库存上限

    // 领域行为
    public BigDecimal getAvailableQuantity() {
        return quantity.subtract(lockedQuantity);
    }

    public boolean isLowStock() {
        return getAvailableQuantity().compareTo(minStock) <= 0;
    }
}
```

### 3.7 PaymentOrder（支付订单实体）

**所属上下文**：BC-14

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_order")
public class PaymentOrderDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;
    private String businessType;    // ORDER/RECHARGE
    private Long businessId;
    private BigDecimal amount;
    private String channel;         // WECHAT/ALIPAY
    private Integer status;         // 0-待支付 1-支付中 2-成功 3-失败 4-关闭
    private String transactionId;   // 第三方交易号
    private LocalDateTime payTime;
    private LocalDateTime callbackTime;
    private String callbackData;    // JSON 回调原始数据
}
```

### 3.8 WorkOrder（生产工单实体）

**所属上下文**：BC-11

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_work_order")
public class WorkOrderDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String workOrderNo;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal completedQuantity;
    private Long routingId;         // 引用工艺路线
    private Integer status;         // 0-已创建 1-已下达 2-进行中 3-已完成 4-已取消
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Long workshopId;
    private String remark;
}
```

---

## 四、DO 类继承规范

| 基类 | 适用场景 | 提供字段 |
|------|---------|---------|
| `TenantBaseDO` | 所有租户隔离表 | id, tenantId, createBy, createTime, updateBy, updateTime, deleted |
| `BaseDO` | 全局共享表 | id, createBy, createTime, updateBy, updateTime, deleted |

```java
// 租户表 DO 必须继承 TenantBaseDO
@TableName("crm_customer")
public class CustomerDO extends TenantBaseDO { ... }

// 共享表 DO 继承 BaseDO
@TableName("system_dict_type")
public class DictTypeDO extends BaseDO { ... }
```

---

## 五、实体状态机总览

```
User:    ACTIVE ←→ DISABLED / LOCKED

Customer: (无硬状态机，通过 inSea/active 标志管理)

Opportunity:
  LEAD → QUALIFIED → PROPOSAL → NEGOTIATION → WON
                 ↘ LOST (任意非终态阶段)

Order:
  DRAFT → PENDING → PAID → COMPLETED
    ↓        ↓
    CANCELLED

PurchaseOrder:
  DRAFT → PENDING → APPROVED → RECEIVED
    ↓        ↓
    CANCELLED

PaymentOrder:
  PENDING → PAYING → SUCCESS
                   → FAILED
                   → CLOSED

WorkOrder:
  CREATED → RELEASED → IN_PROGRESS → COMPLETED
                            ↓
                        CANCELLED
```

---

> 下一步：基于实体设计，进行值对象设计。
