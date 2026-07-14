# 财务域 (Finance) 分析

## 1. 子模块

### 1.1 回款管理
- 回款管理列表 (回款日期/付款方式/金额/逾期)
- 回款计划 / 回款计划汇总
- 回款审批 (多状态: 审批中/已通过/已撤销/待审批/被否决/被驳回)
- 回款记录报表 / 计划回款报表

### 1.2 发票管理
- 发票管理列表 (开票日期/票据类型/金额/发票号码/经手人)
- 开票记录报表 (已开票金额汇总)
- 发票记录

### 1.3 报销管理
- 全部报销单 (员工维度)
- 报销审批 (多状态变体)
- 费用管理 / 费用记录

### 1.4 退款管理
- 退款管理列表
- 退款审批 (多状态变体)
- 退款记录

### 1.5 财务分析
- 财务数据分析
- 回款金额统计
- 按产品/分类/人员/时间/国家/省份汇总

## 2. 核心实体

### Receipt (回款单)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| systemNo | VARCHAR(50) | 系统编号 |
| orderId | BIGINT | 关联订单 |
| customerId | BIGINT | 客户 (冗余) |
| paymentDate | DATE | 回款日期 |
| paymentMethod | VARCHAR(30) | 付款方式 |
| amount | DECIMAL(18,2) | 回款金额 |
| approvalStatus | VARCHAR(20) | 审批状态 |
| overdueStatus | VARCHAR(20) | 逾期状态 |
| planDate | DATE | 计划回款日期 |
| tenantId | BIGINT | 租户ID |

### Invoice (发票)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| invoiceNo | VARCHAR(50) | 发票号码 |
| orderId | BIGINT | 关联订单 |
| customerId | BIGINT | 客户 |
| invoiceDate | DATE | 开票日期 |
| type | VARCHAR(20) | 票据类型 (普票/专票) |
| amount | DECIMAL(18,2) | 开票金额 |
| content | VARCHAR(255) | 票据内容 |
| orderOwnerId | BIGINT | 订单所属人员 |
| handlerId | BIGINT | 发票经手人员 |
| tenantId | BIGINT | 租户ID |

### Reimbursement (报销单)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| reimbursementNo | VARCHAR(50) | 报销单号 |
| amount | DECIMAL(18,2) | 报销金额 |
| applicantId | BIGINT | 报销人员 |
| departmentId | BIGINT | 报销部门 |
| approvalStatus | VARCHAR(20) | 审批状态 |
| approvalType | VARCHAR(30) | 审批类型 |
| submitTime | DATETIME | 提交时间 |
| approvalTime | DATETIME | 审批时间 |
| notes | TEXT | 备注 |
| tenantId | BIGINT | 租户ID |

### Refund (退款单)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| refundNo | VARCHAR(50) | 退款单号 |
| orderId | BIGINT | 关联订单 |
| amount | DECIMAL(18,2) | 退款金额 |
| reason | VARCHAR(255) | 退款原因 |
| approvalStatus | VARCHAR(20) | 审批状态 |
| tenantId | BIGINT | 租户ID |

## 3. 业务规则

1. **回款与订单关联**: 一个订单可分多次回款
2. **逾期预警**: 计划回款日过后未回款标记逾期
3. **发票与订单关联**: 可先开票后回款，或先回款后开票
4. **报销审批流程**: 按金额分级审批
5. **退款审批**: 必须关联已有订单

## 4. 与MITEDTSM的关系

| 能力 | 复用来源 | 复用度 |
|------|---------|--------|
| 审批流 | module-bpm (Flowable) | 80% |
| 财务编号规则 | 需自建 | 0% |
| 统计分析 | module-report | 70% |
| 现有CRM财务 | module-crm 可能有回款功能 | 需对比 |
