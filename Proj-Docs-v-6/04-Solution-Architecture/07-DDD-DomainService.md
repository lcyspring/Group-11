# DDD 领域服务设计 - 密讯ETM系统 (mitedtsm)

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

## 一、领域服务设计原则

1. **无状态**：领域服务不持有私有状态，所有数据通过参数传递
2. **跨聚合**：当一个业务操作跨越多个聚合时，使用领域服务协调
3. **纯领域逻辑**：不包含基础设施代码（如 HTTP 调用），通过 Mapper/Repository 操作数据
4. **单一职责**：每个领域服务只负责一个明确的业务功能

---

## 二、领域服务 vs 应用服务

| 维度 | 领域服务 | 应用服务 (Service 层) |
|------|---------|----------------------|
| 关注点 | 业务逻辑、业务规则 | 用例编排、事务管理 |
| 状态 | 无状态 | 无状态 |
| 事务 | 不管理 | 管理 `@Transactional` |
| 依赖 | Mapper、领域事件 | 领域服务、Mapper、外部 API |
| 输入 | 领域对象 | DTO |
| 输出 | 领域对象 | VO/DTO |

> 注：在 mitedtsm 项目中，领域服务逻辑通常下沉到 Service 层或作为独立的 DomainService 类存在。

---

## 三、核心领域服务设计

### 3.1 CustomerTransferDomainService（客户转移领域服务）

**职责**：客户归属人变更时，同步转移关联的商机、订单。

```java
package com.meession.etm.module.crm.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.module.crm.dal.dataobject.CustomerDO;
import com.meession.etm.module.crm.dal.dataobject.OpportunityDO;
import com.meession.etm.module.crm.dal.mapper.CustomerMapper;
import com.meession.etm.module.crm.dal.mapper.OpportunityMapper;
import com.meession.etm.module.crm.dal.mapper.OrderMapper;
import com.meession.etm.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户转移领域服务
 * 跨聚合协调：客户归属转移 + 关联商机/订单同步转移
 */
@Service
@RequiredArgsConstructor
public class CustomerTransferService {

    private final CustomerMapper customerMapper;
    private final OpportunityMapper opportunityMapper;
    private final OrderMapper orderMapper;

    /**
     * 转移客户归属
     * 1. 更新客户归属人
     * 2. 同步转移关联商机
     * 3. 同步转移关联订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void transferCustomer(Long customerId, Long newOwnerId, String newOwnerName) {
        // 1. 验证客户存在
        CustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException("客户不存在: " + customerId);
        }

        Long oldOwnerId = customer.getOwnerUserId();
        if (oldOwnerId.equals(newOwnerId)) {
            throw new BusinessException("不能转移给相同归属人");
        }

        // 2. 更新客户归属
        customer.setOwnerUserId(newOwnerId);
        customer.setOwnerUserName(newOwnerName);
        customerMapper.updateById(customer);

        // 3. 同步转移关联商机
        LambdaUpdateWrapper<OpportunityDO> oppWrapper = new LambdaUpdateWrapper<>();
        oppWrapper.eq(OpportunityDO::getCustomerId, customerId)
                  .set(OpportunityDO::getOwnerUserId, newOwnerId);
        opportunityMapper.update(null, oppWrapper);

        // 4. 同步转移关联订单
        orderMapper.updateOwnerByCustomerId(customerId, newOwnerId);
    }
}
```

### 3.2 OpportunityConversionService（商机转化领域服务）

**职责**：商机成交后，创建订单。

```java
package com.meession.etm.module.crm.service;

import com.meession.etm.module.crm.dal.dataobject.OpportunityDO;
import com.meession.etm.module.crm.dal.dataobject.OrderDO;
import com.meession.etm.module.crm.dal.mapper.OpportunityMapper;
import com.meession.etm.module.crm.dal.mapper.OrderMapper;
import com.meession.etm.module.crm.enums.OrderStatus;
import com.meession.etm.module.crm.enums.SalesStage;
import com.meession.etm.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商机转化领域服务
 */
@Service
@RequiredArgsConstructor
public class OpportunityConversionService {

    private final OpportunityMapper opportunityMapper;
    private final OrderMapper orderMapper;

    @Transactional(rollbackFor = Exception.class)
    public OrderDO convertToOrder(Long opportunityId) {
        // 1. 验证商机
        OpportunityDO opp = opportunityMapper.selectById(opportunityId);
        if (opp == null) {
            throw new BusinessException("商机不存在: " + opportunityId);
        }
        if (opp.getStage() != SalesStage.WON.getOrder()) {
            throw new BusinessException("仅赢单状态可转为订单");
        }
        if (opp.getActualAmount() == null
                || opp.getActualAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("赢单金额不能为 0");
        }

        // 2. 创建订单
        OrderDO order = new OrderDO();
        order.setOrderNo(generateOrderNo());
        order.setTitle(opp.getOpportunityName());
        order.setCustomerId(opp.getCustomerId());
        order.setOpportunityId(opp.getId());
        order.setTotalAmount(opp.getActualAmount());
        order.setStatus(OrderStatus.DRAFT.getCode());
        order.setOwnerUserId(opp.getOwnerUserId());
        order.setSignDate(LocalDateTime.now());

        orderMapper.insert(order);
        return order;
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
```

### 3.3 InventoryDomainService（库存领域服务）

**职责**：管理库存扣减、释放、盘点等核心库存逻辑。

```java
package com.meession.etm.module.erp.service;

import com.meession.etm.module.erp.dal.dataobject.InventoryDO;
import com.meession.etm.module.erp.dal.mapper.InventoryMapper;
import com.meession.etm.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 库存领域服务
 */
@Service
@RequiredArgsConstructor
public class InventoryDomainService {

    private final InventoryMapper inventoryMapper;

    /**
     * 锁定库存（订单创建时）
     */
    @Transactional(rollbackFor = Exception.class)
    public void lockStock(Long productId, Long warehouseId, BigDecimal quantity) {
        InventoryDO inventory = getOrCreate(productId, warehouseId);

        BigDecimal available = inventory.getQuantity()
                .subtract(inventory.getLockedQuantity());
        if (available.compareTo(quantity) < 0) {
            throw new BusinessException(String.format(
                    "库存不足: 可用 %s, 需要 %s", available, quantity));
        }

        inventory.setLockedQuantity(inventory.getLockedQuantity().add(quantity));
        inventoryMapper.updateById(inventory);
    }

    /**
     * 确认出库（支付完成后）
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmDeduct(Long productId, Long warehouseId, BigDecimal quantity) {
        InventoryDO inventory = getOrCreate(productId, warehouseId);

        inventory.setLockedQuantity(inventory.getLockedQuantity().subtract(quantity));
        inventory.setQuantity(inventory.getQuantity().subtract(quantity));
        inventoryMapper.updateById(inventory);
    }

    /**
     * 释放锁定库存（订单取消时）
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseLock(Long productId, Long warehouseId, BigDecimal quantity) {
        InventoryDO inventory = getOrCreate(productId, warehouseId);

        BigDecimal newLocked = inventory.getLockedQuantity().subtract(quantity);
        if (newLocked.compareTo(BigDecimal.ZERO) < 0) {
            newLocked = BigDecimal.ZERO;
        }
        inventory.setLockedQuantity(newLocked);
        inventoryMapper.updateById(inventory);
    }

    /**
     * 入库
     */
    @Transactional(rollbackFor = Exception.class)
    public void inbound(Long productId, Long warehouseId, BigDecimal quantity) {
        InventoryDO inventory = getOrCreate(productId, warehouseId);
        inventory.setQuantity(inventory.getQuantity().add(quantity));
        inventoryMapper.updateById(inventory);
    }

    private InventoryDO getOrCreate(Long productId, Long warehouseId) {
        InventoryDO inventory = inventoryMapper.selectByProductAndWarehouse(productId, warehouseId);
        if (inventory == null) {
            inventory = new InventoryDO();
            inventory.setProductId(productId);
            inventory.setWarehouseId(warehouseId);
            inventory.setQuantity(BigDecimal.ZERO);
            inventory.setLockedQuantity(BigDecimal.ZERO);
            inventoryMapper.insert(inventory);
        }
        return inventory;
    }
}
```

### 3.4 PaymentDomainService（支付领域服务）

**职责**：处理支付回调、验签、状态同步。

```java
package com.meession.etm.module.pay.service;

import com.meession.etm.module.pay.dal.dataobject.PaymentOrderDO;
import com.meession.etm.module.pay.dal.mapper.PaymentOrderMapper;
import com.meession.etm.module.pay.enums.PayStatus;
import com.meession.etm.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付领域服务
 */
@Service
@RequiredArgsConstructor
public class PaymentDomainService {

    private final PaymentOrderMapper paymentOrderMapper;

    /**
     * 处理支付成功回调
     * 1. 验签（由外部调用前完成）
     * 2. 更新支付状态
     * 3. 幂等处理
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderDO handlePaymentCallback(String paymentNo,
                                                 String transactionId,
                                                 String callbackData) {
        PaymentOrderDO order = paymentOrderMapper.selectByPaymentNo(paymentNo);
        if (order == null) {
            throw new BusinessException("支付订单不存在: " + paymentNo);
        }

        // 幂等处理：已成功/已关闭的订单不重复处理
        if (PayStatus.SUCCESS.getCode() == order.getStatus()
                || PayStatus.CLOSED.getCode() == order.getStatus()) {
            return order;
        }

        // 更新支付状态
        order.setStatus(PayStatus.SUCCESS.getCode());
        order.setTransactionId(transactionId);
        order.setPayTime(LocalDateTime.now());
        order.setCallbackTime(LocalDateTime.now());
        order.setCallbackData(callbackData);
        paymentOrderMapper.updateById(order);

        return order;
    }

    /**
     * 关闭支付订单（超时未支付）
     */
    @Transactional(rollbackFor = Exception.class)
    public void closePayment(Long paymentId) {
        PaymentOrderDO order = paymentOrderMapper.selectById(paymentId);
        if (order == null) return;

        // 仅待支付状态可关闭
        if (PayStatus.PENDING.getCode() == order.getStatus()
                || PayStatus.PAYING.getCode() == order.getStatus()) {
            order.setStatus(PayStatus.CLOSED.getCode());
            paymentOrderMapper.updateById(order);
        }
    }
}
```

### 3.5 ApprovalDomainService（审批领域服务）

**职责**：启动审批流程、处理审批动作。

```java
package com.meession.etm.module.bpm.service;

import com.meession.etm.module.bpm.dal.dataobject.ApprovalTaskDO;
import com.meession.etm.module.bpm.dal.dataobject.ProcessInstanceDO;
import com.meession.etm.module.bpm.dal.mapper.ProcessInstanceMapper;
import com.meession.etm.module.bpm.dal.mapper.ApprovalTaskMapper;
import com.meession.etm.module.bpm.enums.ApprovalAction;
import com.meession.etm.module.bpm.enums.TaskStatus;
import com.meession.etm.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审批领域服务
 */
@Service
@RequiredArgsConstructor
public class ApprovalDomainService {

    private final ProcessInstanceMapper processMapper;
    private final ApprovalTaskMapper taskMapper;

    /**
     * 审批通过
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long taskId, Long userId, String comment) {
        ApprovalTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("审批任务不存在: " + taskId);
        }
        if (!task.getAssigneeId().equals(userId)) {
            throw new BusinessException("非当前审批人，不可审批");
        }
        if (task.getStatus() != TaskStatus.PENDING.getCode()) {
            throw new BusinessException("任务已处理，不可重复审批");
        }

        // 更新任务
        task.setAction(ApprovalAction.APPROVED.name());
        task.setComment(comment);
        task.setStatus(TaskStatus.COMPLETED.getCode());
        task.setProcessTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 检查流程是否完成，创建下一节点或完成流程
        checkAndAdvanceProcess(task.getProcessInstanceId());
    }

    /**
     * 审批驳回
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, Long userId, String comment) {
        ApprovalTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("审批任务不存在: " + taskId);
        }
        if (!task.getAssigneeId().equals(userId)) {
            throw new BusinessException("非当前审批人，不可审批");
        }

        task.setAction(ApprovalAction.REJECTED.name());
        task.setComment(comment);
        task.setStatus(TaskStatus.COMPLETED.getCode());
        task.setProcessTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 终止流程
        ProcessInstanceDO process = processMapper.selectById(task.getProcessInstanceId());
        if (process != null) {
            process.setStatus(2); // 驳回终止
            processMapper.updateById(process);
        }
    }

    private void checkAndAdvanceProcess(Long processInstanceId) {
        // 查询是否还有未完成的审批节点
        // 如果全部完成，更新流程状态为通过（1）
        // 如果有下一节点，创建下一节点审批任务
    }
}
```

---

## 四、领域服务依赖图

```
┌──────────────────────────────────────────────────────────┐
│                    领域服务依赖关系                         │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  CustomerTransferService                                 │
│    ├── CustomerMapper                                    │
│    ├── OpportunityMapper                                 │
│    └── OrderMapper                                       │
│                                                          │
│  OpportunityConversionService                            │
│    ├── OpportunityMapper                                 │
│    └── OrderMapper                                       │
│                                                          │
│  InventoryDomainService                                  │
│    └── InventoryMapper                                   │
│                                                          │
│  PaymentDomainService                                    │
│    └── PaymentOrderMapper                                │
│                                                          │
│  ApprovalDomainService                                   │
│    ├── ProcessInstanceMapper                             │
│    └── ApprovalTaskMapper                                │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

> 下一步：基于领域服务设计，进行应用服务设计。
