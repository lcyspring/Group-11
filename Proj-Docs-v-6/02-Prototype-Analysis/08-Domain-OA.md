# OA域 (Office Automation) 分析

## 1. 子模块

### 1.1 申请管理
- **请假申请**: 请假类型/原因/天数
- **出差申请**: 出差天数/原因/起止时间
- **借款申请**: 借款类型/原因/关联业务/金额
- **客户拜访**: 拜访对象/时间/内容

### 1.2 审批中心
- 订单审批 / 回款审批 / 报销审批
- 请假审批 / 出差审批 / 借款审批 / 拜访审批
- 请示审批 / 群发审核
- 审批数据统计

### 1.3 办公工具
- **工作报告**: 日报/周报/月报
- **工作请示**: 请示内容+审批
- **任务管理**: 任务创建/分配/跟踪
- **日程管理**: 日/周/月视图
- **文档管理**: 公共文档/共享文档
- **系统公告**: 公告发布
- **内部消息**: 站内消息

## 2. 核心实体

### LeaveRequest (请假申请)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| type | VARCHAR(20) | 请假类型 (年假/事假/病假/婚假/丧假) |
| reason | TEXT | 请假原因 |
| days | DECIMAL(5,1) | 请假天数 |
| startDate | DATE | 开始日期 |
| endDate | DATE | 结束日期 |
| applicantId | BIGINT | 申请人 |
| approvalType | VARCHAR(30) | 审批类型 |
| approvalStatus | VARCHAR(20) | 审批状态 |
| submitTime | DATETIME | 提交时间 |
| approvalTime | DATETIME | 审批时间 |
| tenantId | BIGINT | 租户ID |

### BusinessTrip (出差申请)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| days | INT | 出差天数 |
| reason | TEXT | 出差原因 |
| startTime | DATETIME | 开始时间 |
| endTime | DATETIME | 结束时间 |
| destination | VARCHAR(200) | 目的地 |
| applicantId | BIGINT | 申请人 |
| approvalType | VARCHAR(30) | 审批类型 |
| approvalStatus | VARCHAR(20) | 审批状态 |
| submitTime | DATETIME | 提交时间 |
| approvalTime | DATETIME | 审批时间 |
| tenantId | BIGINT | 租户ID |

### LoanRequest (借款申请)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| type | VARCHAR(30) | 借款类型 |
| reason | TEXT | 借款原因 |
| amount | DECIMAL(18,2) | 借款金额 |
| associatedBusiness | VARCHAR(100) | 关联业务 |
| applicantId | BIGINT | 申请人 |
| approvalType | VARCHAR(30) | 审批类型 |
| approvalStatus | VARCHAR(20) | 审批状态 |
| submitTime | DATETIME | 提交时间 |
| approvalTime | DATETIME | 审批时间 |
| tenantId | BIGINT | 租户ID |

### CustomerVisit (客户拜访)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| customerId | BIGINT | 拜访客户 |
| visitDate | DATE | 拜访日期 |
| content | TEXT | 拜访内容 |
| result | TEXT | 拜访结果 |
| applicantId | BIGINT | 申请人 |
| approvalStatus | VARCHAR(20) | 审批状态 |
| tenantId | BIGINT | 租户ID |

### WorkReport (工作报告)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| type | VARCHAR(10) | 报告类型 (日报/周报/月报) |
| date | DATE | 报告日期 |
| content | TEXT | 报告内容 |
| reporterId | BIGINT | 报告人 |
| tenantId | BIGINT | 租户ID |

### Task (任务)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| title | VARCHAR(200) | 任务标题 |
| description | TEXT | 任务描述 |
| status | VARCHAR(20) | 状态 (未开始/进行中/已完成/已超时/已取消) |
| priority | VARCHAR(10) | 优先级 |
| assigneeId | BIGINT | 负责人 |
| dueDate | DATE | 截止日期 |
| tenantId | BIGINT | 租户ID |

## 3. 业务规则

1. **审批流程**: 不同申请类型可配置不同审批链
2. **请假天数校验**: 检查假期余额
3. **借款额度**: 按级别限制借款金额
4. **出差报销关联**: 出差后可关联生成报销单
5. **任务超时**: 到期未完成自动标记"已超时"

## 4. 与MITEDTSM的关系

| 能力 | 复用来源 | 复用度 |
|------|---------|--------|
| 审批流引擎 | module-bpm (Flowable) | 80% |
| 任务管理 | 可能有现成的task功能 | 需对比 |
| 文档管理 | module-infra (文件管理) | 70% |
| 系统公告/内部消息 | module-system (通知) | 60% |
| 日程管理 | 新建 | 0% |
| 用户/部门 | AdminUserDO / DeptDO | 100% |
