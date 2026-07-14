# 订单域 (Order) 分析

## 1. 子模块

### 1.1 订单管理
- 全部订单列表
- 订单详情 (审批中/已撤销/已通过/待审批/未提交/被否决/被驳回)
- 添加订单
- 订单记录 (客户维度)

### 1.2 产品与报价
- 选择产品
- 产品报价
- 确认报价

### 1.3 订单审批
- 订单审批列表
- 订单审批详情 (多状态变体)

### 1.4 报表
- 订单记录报表
- 成交订单报表 (客户/产品/人员/时间维度汇总)

## 2. 核心实体

### Order (订单)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| orderNo | VARCHAR(50) | 订单编号 |
| title | VARCHAR(200) | 订单标题 |
| totalAmount | DECIMAL(18,2) | 订单总金额 |
| category | VARCHAR(30) | 订单分类 |
| type | VARCHAR(30) | 订单类型 |
| status | VARCHAR(30) | 订单状态 (审批中/已通过/已撤销/被否决/被驳回/未提交) |
| customerId | BIGINT | 关联客户 |
| contractNo | VARCHAR(50) | 合同编号 |
| signingDate | DATE | 签署日期 |
| responsiblePersonId | BIGINT | 负责人员 |
| notes | TEXT | 备注 |
| opportunityId | BIGINT | 来源商机 (可为空) |
| tenantId | BIGINT | 租户ID |

### OrderLineItem (订单产品行)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| orderId | BIGINT | 关联订单 |
| productId | BIGINT | 产品ID |
| productName | VARCHAR(100) | 产品名称 |
| productCode | VARCHAR(50) | 产品编号 |
| standardPrice | DECIMAL(18,2) | 标准单价 |
| actualPrice | DECIMAL(18,2) | 实际售价 |
| quantity | INT | 数量 |
| discount | DECIMAL(5,2) | 折扣 |
| total | DECIMAL(18,2) | 行总价 |
| specifications | VARCHAR(200) | 规格 |
| tenantId | BIGINT | 租户ID |

## 3. 订单审批状态流转

```
未提交 → 待审批 → 审批中 → 已通过
            ↓          ↓
          已撤销    被驳回 / 被否决
```

## 4. 业务规则

1. **订单编号**: 自动生成，如 PO-2026-0001
2. **商机转订单**: 商机成交后可一键生成订单
3. **订单审批**: 超过一定金额需多层审批
4. **订单关联**: 一个订单可关联多个回款记录和发票

## 5. 与MITEDTSM的关系

| 能力 | 复用来源 | 复用度 |
|------|---------|--------|
| 产品报价 | 新建商机模块 | 共享字段结构 |
| 审批流 | module-bpm (Flowable) | 80% |
| 编号生成 | 新建规则引擎 | 需开发 |
| CRM已有订单 | module-crm 可能已有类似实体 | 需对比 |
