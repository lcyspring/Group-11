# DDD 领域事件 - 密讯ETM系统 (mitedtsm)

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

## 一、事件设计原则

1. **不可变性**：事件一旦创建不可修改，携带事件发生时的完整快照
2. **自描述**：事件命名使用过去式（如 `CustomerCreatedEvent`），清晰表达已发生的事实
3. **最终一致性**：跨聚合操作通过事件实现最终一致性
4. **异步处理**：事件处理尽量异步，避免阻塞主流程
5. **幂等性**：事件处理器必须支持幂等，防止重复消费

---

## 二、事件基础设施

- **同步事件**：Spring `ApplicationEventPublisher` + `@TransactionalEventListener(phase = AFTER_COMMIT)`
- **异步事件**：RabbitMQ（跨模块通信）
- **事件存储**：可选 `event_outbox` 表保证可靠投递

---

## 三、领域事件清单

### 3.1 系统管理上下文

| 事件 | 触发时机 | 消费者 |
|------|---------|--------|
| UserCreatedEvent | 用户创建 | 消息通知（发送欢迎邮件） |
| UserDisabledEvent | 用户禁用 | 权限系统（立即失效Token） |
| RoleMenuChangedEvent | 角色菜单变更 | 前端（刷新动态路由缓存） |
| TenantCreatedEvent | 租户创建 | 基础设施（初始化租户Schema/默认数据） |

### 3.2 CRM 上下文

| 事件 | 触发时机 | 消费者 |
|------|---------|--------|
| CustomerCreatedEvent | 客户创建 | 营销上下文（创建默认关怀配置） |
| CustomerMovedToSeaEvent | 客户移入公海 | 消息通知（通知原负责人） |
| CustomerClaimedEvent | 客户被领取 | 消息通知（通知领取人） |
| CustomerOwnerChangedEvent | 客户归属变更 | 跟进记录（转移历史跟进） |
| OpportunityCreatedEvent | 商机创建 | 消息通知 |
| OpportunityStageAdvancedEvent | 阶段推进 | 报表（更新漏斗数据） |
| OpportunityWonEvent | 商机成交 | 订单上下文（自动/手动创建订单） |
| OpportunityLostEvent | 商机流失 | 报表（更新流失统计） |
| OrderSubmittedEvent | 订单提交 | BPM（启动审批流程） |
| OrderApprovedEvent | 订单通过 | 库存上下文（扣减库存）、财务上下文 |
| OrderCancelledEvent | 订单取消 | 库存上下文（释放锁定库存） |

### 3.3 ERP 上下文

| 事件 | 触发时机 | 消费者 |
|------|---------|--------|
| PurchaseOrderCreatedEvent | 采购单创建 | WMS（预占库位） |
| PurchaseOrderReceivedEvent | 采购入库 | 库存上下文（增加库存）、财务上下文（生成应付） |
| SalesOrderShippedEvent | 销售出库 | 库存上下文（减少库存）、财务上下文（生成应收） |
| InventoryLowStockEvent | 库存低于警戒线 | 采购上下文（建议补货）、消息通知 |
| ReceiptCreatedEvent | 回款记录创建 | 订单上下文（更新回款状态） |
| ReceiptOverdueEvent | 回款逾期 | 消息通知（逾期提醒） |

### 3.4 MES 上下文

| 事件 | 触发时机 | 消费者 |
|------|---------|--------|
| WorkOrderReleasedEvent | 工单下达 | WMS（物料配送） |
| OperationCompletedEvent | 工序完成 | 工单（更新进度）、质检（触发检验） |
| QualityCheckFailedEvent | 质检不合格 | 工单（创建返工任务）、消息通知 |

### 3.5 支付上下文

| 事件 | 触发时机 | 消费者 |
|------|---------|--------|
| PaymentSuccessEvent | 支付成功 | 订单上下文（更新支付状态）、商城上下文 |
| PaymentFailedEvent | 支付失败 | 消息通知（提醒用户重新支付） |
| RefundSuccessEvent | 退款成功 | 订单上下文（更新退款状态）、消息通知 |

### 3.6 BPM 上下文

| 事件 | 触发时机 | 消费者 |
|------|---------|--------|
| ApprovalSubmittedEvent | 审批提交 | 流程引擎（创建流程实例） |
| ApprovalCompletedEvent | 审批完成 | 各业务上下文（根据审批结果执行后续操作） |
| ApprovalRejectedEvent | 审批驳回 | 消息通知（通知申请人） |

---

## 四、事件结构规范

```java
// 事件基类
public abstract class DomainEvent {
    private String eventId;          // UUID
    private Long tenantId;           // 租户ID
    private Long userId;             // 触发用户ID
    private LocalDateTime occurredAt; // 发生时间

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
    }
}

// 示例：客户创建事件
public class CustomerCreatedEvent extends DomainEvent {
    private Long customerId;
    private String customerName;
    private Long ownerUserId;

    public CustomerCreatedEvent(Long customerId, String customerName, Long ownerUserId) {
        super();
        this.customerId = customerId;
        this.customerName = customerName;
        this.ownerUserId = ownerUserId;
    }
}

// 示例：商机成交事件
public class OpportunityWonEvent extends DomainEvent {
    private Long opportunityId;
    private Long customerId;
    private BigDecimal actualAmount;

    public OpportunityWonEvent(Long opportunityId, Long customerId, BigDecimal actualAmount) {
        super();
        this.opportunityId = opportunityId;
        this.customerId = customerId;
        this.actualAmount = actualAmount;
    }
}

// 示例：支付成功事件
public class PaymentSuccessEvent extends DomainEvent {
    private Long paymentId;
    private String paymentNo;
    private String businessType;
    private Long businessId;
    private BigDecimal amount;
    private String channel;

    public PaymentSuccessEvent(Long paymentId, String paymentNo,
                                String businessType, Long businessId,
                                BigDecimal amount, String channel) {
        super();
        this.paymentId = paymentId;
        this.paymentNo = paymentNo;
        this.businessType = businessType;
        this.businessId = businessId;
        this.amount = amount;
        this.channel = channel;
    }
}
```

---

## 五、事件处理器示例

```java
// 商机成交事件处理器（在订单模块中）
@Component
public class OpportunityWonEventHandler {

    private final OrderService orderService;

    @Async
    @EventListener
    @Transactional
    public void handle(OpportunityWonEvent event) {
        log.info("处理商机成交事件: opportunityId={}", event.getOpportunityId());
        // 创建订单（或通知用户手动创建）
        orderService.createOrderFromOpportunity(
            event.getOpportunityId(),
            event.getCustomerId(),
            event.getActualAmount()
        );
    }
}

// 支付成功事件处理器（在订单模块中）
@Component
public class PaymentSuccessHandler {

    @Async
    @EventListener
    @Transactional
    public void handle(PaymentSuccessEvent event) {
        if (!"ORDER".equals(event.getBusinessType())) return;
        log.info("处理支付成功事件: paymentNo={}, orderId={}",
            event.getPaymentNo(), event.getBusinessId());
        orderService.markAsPaid(event.getBusinessId(), event.getPaymentNo());
    }
}

// 审批完成事件处理器
@Component
public class ApprovalCompletedHandler {

    @Async
    @EventListener
    public void handle(ApprovalCompletedEvent event) {
        if (!event.isApproved()) return;

        switch (event.getBusinessType()) {
            case "ORDER"    -> orderService.approve(event.getBusinessId());
            case "PURCHASE" -> purchaseService.approve(event.getBusinessId());
            case "REFUND"   -> refundService.process(event.getBusinessId());
            default -> log.warn("未知业务类型: {}", event.getBusinessType());
        }
    }
}
```

---

## 六、关键流程事件链

### 商机成交→订单→支付流程

```
OpportunityWonEvent
  → Order Context: 创建订单
  → OrderSubmittedEvent
    → BPM: 启动审批流程
    → ApprovalCompletedEvent (通过)
      → Order Context: 订单生效
      → 用户发起支付
      → PaymentSuccessEvent
        → Order Context: 更新支付状态
        → Inventory Context: 扣减库存
```

### 采购入库流程

```
PurchaseOrderApprovedEvent
  → WMS: 创建入库单
  → InboundCompletedEvent
    → Inventory: 增加库存
    → Finance (ERP): 生成应付账款
```

---

## 七、事件存储（可选）

对于高可靠性场景，可将事件持久化：

```sql
CREATE TABLE domain_event_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    event_type VARCHAR(128) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_data JSON NOT NULL,
    occurred_at DATETIME NOT NULL,
    published TINYINT DEFAULT 0,
    published_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_aggregate (aggregate_type, aggregate_id),
    INDEX idx_published (published)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

> 本文档完成。
