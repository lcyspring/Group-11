# 工单域 (Work Order) 分析

## 1. 子模块

- 工单管理列表
- 工单记录 (客户维度)
- 工单数据统计
- 工单详情 (发起人/已完结/待处理/被退回)

## 2. 核心实体

### WorkOrder (工单)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| workOrderNo | VARCHAR(50) | 工单编号 |
| title | VARCHAR(200) | 工单标题 |
| type | VARCHAR(30) | 工单类型 (问题/需求/投诉/咨询) |
| status | VARCHAR(20) | 工单状态 (发起/处理中/已完结/已退回) |
| priority | VARCHAR(10) | 优先级 (高/中/低) |
| customerId | BIGINT | 关联客户 |
| handlerId | BIGINT | 处理人员 |
| description | TEXT | 工单描述 |
| solution | TEXT | 解决方案 |
| createTime | DATETIME | 创建时间 |
| processTime | DATETIME | 处理时间 |
| completeTime | DATETIME | 完结时间 |
| tenantId | BIGINT | 租户ID |

## 3. 工单状态流转

```
发起 → 待处理 → 处理中 → 已完结
          ↓
       被退回 (返回发起人修改)
```

## 4. 业务规则

1. **工单编号**: 自动生成 W-YYYYMM-NNNN
2. **SLA 超时预警**: 根据优先级设置处理时限
3. **工单关联**: 可关联客户/订单/商机
4. **工单统计**: 按状态/类型/人员/时间统计

## 5. 与MITEDTSM的关系

| 能力 | 复用来源 | 复用度 |
|------|---------|--------|
| 实体结构 | 全新 | 需从零开发 |
| 用户/处理人 | AdminUserDO | 100% |
| 客户关联 | 新建客户模块 | 直接依赖 |
| 通知 | module-infra (邮件/短信) | 90% |
