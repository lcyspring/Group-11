# DDD 应用服务设计 - 密讯ETM系统 (mitedtsm)

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

## 一、应用服务设计原则

1. **用例编排**：应用服务负责编排领域服务、Mapper 来完成一个完整的用户用例
2. **事务管理**：应用服务层管理事务边界，使用 `@Transactional`
3. **DTO 转换**：负责将外部 DTO 转换为 DO，将 DO 转换为 VO 返回
4. **权限校验**：在 Controller 层通过 `@PreAuthorize` 完成权限校验
5. **薄层设计**：应用服务应尽量薄，不包含业务逻辑，业务逻辑下沉到领域服务层

---

## 二、分层架构

```
┌─────────────────────────────────────────┐
│  Controller（接口层）                      │
│  接收 HTTP 请求，参数校验 (@Valid)         │
│  权限校验 (@PreAuthorize)                  │
├─────────────────────────────────────────┤
│  Service（应用服务层）                     │
│  用例编排、事务管理、DTO 转换              │
├─────────────────────────────────────────┤
│  DomainService（领域服务层 - 可选）        │
│  纯业务逻辑、跨聚合协调                    │
├─────────────────────────────────────────┤
│  DO / Mapper（数据层）                    │
│  MyBatis-Plus 数据访问                   │
└─────────────────────────────────────────┘
```

> 注：mitedtsm 项目的 Service 层同时承担应用服务和领域服务职责。对于复杂场景，可将跨聚合逻辑抽取到独立的 DomainService。

---

## 三、核心应用服务设计

### 3.1 CustomerService（客户应用服务）

```java
package com.meession.etm.module.crm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meession.etm.module.crm.controller.admin.customer.vo.CustomerCreateReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.CustomerPageReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.CustomerVO;
import com.meession.etm.module.crm.convert.CustomerConvert;
import com.meession.etm.module.crm.dal.dataobject.CustomerDO;
import com.meession.etm.module.crm.dal.mapper.CustomerMapper;
import com.meession.etm.module.crm.enums.CustomerLevel;
import com.meession.etm.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户应用服务
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final CustomerTransferService customerTransferService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomer(CustomerCreateReqVO reqVO) {
        // 1. 校验客户名称唯一性
        if (customerMapper.existsByCustomerName(reqVO.getCustomerName())) {
            throw new BusinessException("客户名称已存在");
        }

        // 2. VO 转 DO
        CustomerDO customer = CustomerConvert.INSTANCE.convert(reqVO);
        customer.setLevel(CustomerLevel.NORMAL.getPriority()); // 默认普通客户
        customer.setInSea(false);
        customer.setActive(true);

        // 3. 持久化
        customerMapper.insert(customer);
        return customer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferCustomer(Long customerId, Long newOwnerId, String newOwnerName) {
        customerTransferService.transferCustomer(customerId, newOwnerId, newOwnerName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveToSea(Long customerId) {
        CustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException("客户不存在: " + customerId);
        }
        customer.setInSea(true);
        customer.setOwnerUserId(null);
        customer.setOwnerUserName(null);
        customerMapper.updateById(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimCustomer(Long customerId, Long userId, String userName) {
        CustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null || !Boolean.TRUE.equals(customer.getInSea())) {
            throw new BusinessException("客户不在公海中");
        }
        customer.setInSea(false);
        customer.setOwnerUserId(userId);
        customer.setOwnerUserName(userName);
        customerMapper.updateById(customer);
    }

    @Override
    public IPage<CustomerVO> getCustomerPage(CustomerPageReqVO reqVO) {
        return customerMapper.selectPage(reqVO)
                .convert(CustomerConvert.INSTANCE::convert);
    }
}
```

### 3.2 OpportunityService（商机应用服务）

```java
package com.meession.etm.module.crm.service;

import com.meession.etm.module.crm.controller.admin.opportunity.vo.*;
import com.meession.etm.module.crm.convert.OpportunityConvert;
import com.meession.etm.module.crm.dal.dataobject.OpportunityDO;
import com.meession.etm.module.crm.dal.mapper.OpportunityMapper;
import com.meession.etm.module.crm.enums.SalesStage;
import com.meession.etm.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商机应用服务
 */
@Service
@RequiredArgsConstructor
public class OpportunityServiceImpl implements OpportunityService {

    private final OpportunityMapper opportunityMapper;
    private final OpportunityConversionService conversionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOpportunity(OpportunityCreateReqVO reqVO) {
        OpportunityDO opp = OpportunityConvert.INSTANCE.convert(reqVO);
        opp.setStage(SalesStage.LEAD.getOrder()); // 默认初始阶段
        opportunityMapper.insert(opp);
        return opp.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advanceStage(Long opportunityId, Integer nextStage) {
        OpportunityDO opp = opportunityMapper.selectById(opportunityId);
        if (opp == null) {
            throw new BusinessException("商机不存在: " + opportunityId);
        }

        SalesStage currentStage = SalesStage.values()[opp.getStage() - 1];
        SalesStage targetStage = SalesStage.values()[nextStage - 1];

        if (!currentStage.canAdvanceTo(targetStage)) {
            throw new BusinessException("商机阶段只能向前流转，当前: "
                    + currentStage.getDescription());
        }

        opp.setStage(nextStage);

        // 如果赢单，记录实际金额和时间
        if (targetStage == SalesStage.WON) {
            opp.setWinDate(java.time.LocalDateTime.now());
        }
        // 如果丢单，记录流失原因
        if (targetStage == SalesStage.LOST) {
            // 流失原因由调用方传入
        }

        opportunityMapper.updateById(opp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToOrder(Long opportunityId) {
        return conversionService.convertToOrder(opportunityId).getId();
    }
}
```

### 3.3 OrderService（订单应用服务）

```java
package com.meession.etm.module.crm.service;

import com.meession.etm.module.crm.controller.admin.order.vo.*;
import com.meession.etm.module.crm.convert.OrderConvert;
import com.meession.etm.module.crm.dal.dataobject.OrderDO;
import com.meession.etm.module.crm.dal.mapper.OrderMapper;
import com.meession.etm.module.crm.enums.OrderStatus;
import com.meession.etm.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单应用服务
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateReqVO reqVO) {
        OrderDO order = OrderConvert.INSTANCE.convert(reqVO);
        order.setOrderNo(generateOrderNo());
        order.setStatus(OrderStatus.DRAFT.getCode());
        orderMapper.insert(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOrder(Long orderId) {
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在: " + orderId);
        }
        if (order.getStatus() != OrderStatus.DRAFT.getCode()) {
            throw new BusinessException("仅草稿状态可提交");
        }
        order.setStatus(OrderStatus.PENDING.getCode());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId) {
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在: " + orderId);
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException("仅待支付状态可支付");
        }
        order.setStatus(OrderStatus.PAID.getCode());
        order.setSignDate(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, String reason) {
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在: " + orderId);
        }
        if (!order.canCancel()) {
            throw new BusinessException("当前状态不可取消");
        }
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelReason(reason);
        orderMapper.updateById(order);
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
```

### 3.4 PaymentService（支付应用服务）

```java
package com.meession.etm.module.pay.service;

import com.meession.etm.module.pay.controller.admin.vo.*;
import com.meession.etm.module.pay.dal.dataobject.PaymentOrderDO;
import com.meession.etm.module.pay.dal.mapper.PaymentOrderMapper;
import com.meession.etm.module.pay.enums.PayChannel;
import com.meession.etm.module.pay.enums.PayStatus;
import com.meession.etm.module.pay.service.PaymentDomainService;
import com.meession.etm.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付应用服务
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentDomainService paymentDomainService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderDO createPayment(String businessType, Long businessId,
                                         java.math.BigDecimal amount, String channel) {
        // 校验无重复支付单
        PaymentOrderDO existing = paymentOrderMapper.selectByBusiness(businessType, businessId);
        if (existing != null && existing.getStatus() == PayStatus.SUCCESS.getCode()) {
            throw new BusinessException("该业务单据已支付成功");
        }

        PaymentOrderDO order = new PaymentOrderDO();
        order.setPaymentNo("PAY" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        order.setBusinessType(businessType);
        order.setBusinessId(businessId);
        order.setAmount(amount);
        order.setChannel(channel);
        order.setStatus(PayStatus.PENDING.getCode());
        paymentOrderMapper.insert(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderDO handleCallback(String paymentNo, String transactionId,
                                          String callbackData, String sign) {
        // 1. 验签（微信/支付宝验签逻辑）
        if (!verifySign(callbackData, sign)) {
            throw new BusinessException("回调签名验证失败");
        }

        // 2. 委托领域服务处理
        return paymentDomainService.handlePaymentCallback(paymentNo, transactionId, callbackData);
    }

    private boolean verifySign(String callbackData, String sign) {
        // TODO: 根据支付渠道验签
        return true;
    }
}
```

### 3.5 WorkOrderService（生产工单应用服务）

```java
package com.meession.etm.module.mes.service;

import com.meession.etm.module.mes.controller.admin.vo.*;
import com.meession.etm.module.mes.dal.dataobject.WorkOrderDO;
import com.meession.etm.module.mes.dal.mapper.WorkOrderMapper;
import com.meession.etm.module.mes.enums.WorkOrderStatus;
import com.meession.etm.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 生产工单应用服务
 */
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderMapper workOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(WorkOrderCreateReqVO reqVO) {
        WorkOrderDO wo = new WorkOrderDO();
        wo.setWorkOrderNo("WO" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        wo.setProductId(reqVO.getProductId());
        wo.setQuantity(reqVO.getQuantity());
        wo.setCompletedQuantity(java.math.BigDecimal.ZERO);
        wo.setRoutingId(reqVO.getRoutingId());
        wo.setStatus(WorkOrderStatus.CREATED.getCode());
        wo.setPlanStartDate(reqVO.getPlanStartDate());
        wo.setPlanEndDate(reqVO.getPlanEndDate());
        wo.setWorkshopId(reqVO.getWorkshopId());
        workOrderMapper.insert(wo);
        return wo.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseWorkOrder(Long workOrderId) {
        WorkOrderDO wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) {
            throw new BusinessException("工单不存在: " + workOrderId);
        }
        if (wo.getStatus() != WorkOrderStatus.CREATED.getCode()) {
            throw new BusinessException("仅已创建状态可下达");
        }
        wo.setStatus(WorkOrderStatus.RELEASED.getCode());
        wo.setActualStartDate(LocalDate.now());
        workOrderMapper.updateById(wo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProduction(Long workOrderId) {
        WorkOrderDO wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) {
            throw new BusinessException("工单不存在: " + workOrderId);
        }
        if (wo.getStatus() != WorkOrderStatus.RELEASED.getCode()) {
            throw new BusinessException("仅已下达状态可开始生产");
        }
        wo.setStatus(WorkOrderStatus.IN_PROGRESS.getCode());
        workOrderMapper.updateById(wo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeWorkOrder(Long workOrderId) {
        WorkOrderDO wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) {
            throw new BusinessException("工单不存在: " + workOrderId);
        }
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS.getCode()) {
            throw new BusinessException("仅进行中状态可完工");
        }
        wo.setStatus(WorkOrderStatus.COMPLETED.getCode());
        wo.setActualEndDate(LocalDate.now());
        wo.setCompletedQuantity(wo.getQuantity()); // 全部完工
        workOrderMapper.updateById(wo);
    }
}
```

---

## 四、应用服务调用流程

```
Controller (接收请求)
    │
    ├── @Valid 参数校验
    ├── @PreAuthorize 权限校验
    │
    ▼
Service (应用服务)
    │
    ├── DTO → DO 转换 (MapStruct Converter)
    ├── 业务校验（唯一性、存在性等）
    ├── 调用领域服务 (跨聚合逻辑)
    ├── 调用 Mapper (数据持久化)
    ├── 发布领域事件 (可选的)
    │
    ▼
Controller 返回 VO/Result
```

**典型调用链示例**：

```
POST /admin-api/crm/customer/create
  → CustomerController.create(@Valid CustomerCreateReqVO)
    → @PreAuthorize("@ss.hasPermission('crm:customer:create')")
    → CustomerService.createCustomer(reqVO)
      → CustomerConvert.INSTANCE.convert(reqVO)  // VO → DO
      → customerMapper.existsByCustomerName()     // 唯一性校验
      → customerMapper.insert(customer)           // 持久化
    → Result.success(customerId)
```

---

## 五、MapStruct 转换器示例

```java
package com.meession.etm.module.crm.convert;

import com.meession.etm.module.crm.controller.admin.customer.vo.CustomerCreateReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.CustomerVO;
import com.meession.etm.module.crm.dal.dataobject.CustomerDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CustomerConvert {

    CustomerConvert INSTANCE = Mappers.getMapper(CustomerConvert.class);

    CustomerDO convert(CustomerCreateReqVO reqVO);

    CustomerVO convert(CustomerDO customerDO);
}
```

---

> 本文档完成。DDD领域设计文档集已全部生成。
