# 营销域 (Marketing) 分析

## 1. 子模块

### 1.1 营销活动
- 营销活动列表 (活动标题/类型/时间/状态/负责人)
- 创建活动 / 活动详情

### 1.2 短信营销
- 短信模板
- 短信群发 (内容/数量/到达/审核)
- 短信记录
- 短信群发分析

### 1.3 邮件营销
- 邮件模板
- 邮件群发 (标题/数量/到达/审核)
- 邮件群发分析

### 1.4 客户关怀
- 生日自动祝福 (启用/设置)
- 节假日自动祝福 (启用/设置)
- 祝福记录
- 客户生日查询

### 1.5 群发审核
- 群发审核列表 (短信/邮件)

## 2. 核心实体

### Campaign (营销活动)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| title | VARCHAR(200) | 活动标题 |
| type | VARCHAR(30) | 活动类型 |
| startTime | DATETIME | 开始时间 |
| endTime | DATETIME | 结束时间 |
| status | VARCHAR(20) | 活动状态 (筹备/进行中/已结束) |
| ownerId | BIGINT | 负责人员 |
| budget | DECIMAL(18,2) | 预算 |
| description | TEXT | 活动描述 |
| targetCustomers | JSON | 目标客户列表 |
| tenantId | BIGINT | 租户ID |

### SMSBroadcast (短信群发)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| content | TEXT | 短信内容 |
| senderId | BIGINT | 发送人员 |
| status | VARCHAR(20) | 群发状态 (待审核/已通过/已发送) |
| totalCount | INT | 总条数 |
| deliveredCount | INT | 到达条数 |
| submitTime | DATETIME | 提交时间 |
| sendTime | DATETIME | 发送时间 |
| templateId | BIGINT | 模板ID |
| tenantId | BIGINT | 租户ID |

### EmailBroadcast (邮件群发)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| subject | VARCHAR(200) | 邮件标题 |
| senderId | BIGINT | 发送人员 |
| status | VARCHAR(20) | 群发状态 |
| totalCount | INT | 总数量 |
| deliveredCount | INT | 到达数量 |
| submitTime | DATETIME | 提交时间 |
| sendTime | DATETIME | 发送时间 |
| templateId | BIGINT | 模板ID |
| tenantId | BIGINT | 租户ID |

### CustomerCare (客户关怀)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| customerId | BIGINT | 客户 |
| contactId | BIGINT | 联系人 |
| birthday | DATE | 生日 |
| autoBirthdayGreeting | BOOLEAN | 启用生日自动祝福 |
| autoHolidayGreeting | BOOLEAN | 启用节假日自动祝福 |
| greetingTemplateId | BIGINT | 祝福模板 |
| tenantId | BIGINT | 租户ID |

## 3. 业务规则

1. **群发审核**: 短信/邮件群发需先通过审核才能发送
2. **定时发送**: 支持指定时间发送
3. **自动祝福**: 客户生日/节假日自动触发短信+邮件
4. **发送分析**: 统计到达率/打开率
5. **模板变量**: 支持 {客户名称} {联系人} 等变量替换

## 4. 与MITEDTSM的关系

| 能力 | 复用来源 | 复用度 |
|------|---------|--------|
| 短信发送 | module-system (短信服务) | 70% |
| 邮件发送 | module-infra (邮件服务) | 80% |
| 模板引擎 | Velocity | 100% |
| 定时任务 | module-job | 100% |
| 客户数据 | 新建客户模块 | 直接依赖 |
| 审批流 | module-bpm | 60% (群发审核) |
