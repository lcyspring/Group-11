# CRM 数据库设计

---

## 文档信息

| 项目 | 内容 |
|------|------|
| 项目名称 | MITEDTSM 密讯ETM系统 — CRM子系统 |
| 文档类型 | 数据库设计文档 |
| 数据库 | MySQL 8.0（InnoDB / utf8mb4） |
| 文档版本 | V1.0 |
| 创建日期 | 2026-06-25 |

---

## 1. 设计原则

- **MySQL only**：统一使用 MySQL 8.0，引擎 InnoDB，字符集 utf8mb4
- **多租户隔离**：CRM业务表统一继承 `tenant_id` 字段
- **命名规范**：表名小写+下划线，前缀 `crm_` 标识CRM模块
- **审计字段**：每表包含 `create_time/create_by/update_time/update_by`
- **软删除**：使用 `deleted` 标记（0=正常，1=删除）

---

## 2. 客户域（Customer Domain）

### 2.1 crm_customer（客户表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| name | VARCHAR(128) | NOT NULL | 客户名称 |
| status | VARCHAR(32) | NOT NULL | 客户状态（潜在/意向/成交/流失） |
| industry | VARCHAR(32) | | 所属行业（字典） |
| source | VARCHAR(32) | | 客户来源（字典） |
| star_level | TINYINT | DEFAULT 0 | 星级（0-5） |
| region | VARCHAR(64) | | 所在区域 |
| country | VARCHAR(64) | | 国家 |
| province | VARCHAR(64) | | 省份 |
| city | VARCHAR(64) | | 城市 |
| address | VARCHAR(256) | | 详细地址 |
| owner_user_id | BIGINT | | 负责人（归属人） |
| owner_dept_id | BIGINT | | 归属部门 |
| primary_contact_id | BIGINT | | 首要联系人ID |
| remark | VARCHAR(512) | | 备注 |
| last_follow_time | DATETIME | | 最后跟进时间 |
| high_seas_status | VARCHAR(16) | DEFAULT 'NORMAL' | 公海状态（NORMAL/IN_SEA） |
| high_seas_enter_time | DATETIME | | 掉入公海时间 |
| deleted | TINYINT | DEFAULT 0 | 软删除 |
| create_time | DATETIME | NOT NULL | 创建时间 |
| create_by | VARCHAR(64) | | 创建人 |
| update_time | DATETIME | | 更新时间 |
| update_by | VARCHAR(64) | | 更新人 |

**索引**: `idx_tenant_id`, `idx_owner_user_id`, `idx_status`, `idx_name`, `idx_last_follow_time`

### 2.2 crm_contact（联系人表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| customer_id | BIGINT | NOT NULL | 关联客户ID |
| name | VARCHAR(64) | NOT NULL | 联系人姓名 |
| gender | TINYINT | | 性别（0=未知/1=男/2=女） |
| mobile | VARCHAR(20) | | 手机号码 |
| email | VARCHAR(128) | | 邮箱 |
| position | VARCHAR(64) | | 职位 |
| decision_level | VARCHAR(32) | | 决策层级（关键决策人/影响者/使用者） |
| is_primary | TINYINT | DEFAULT 0 | 是否首要联系人 |
| remark | VARCHAR(256) | | 备注 |
| deleted | TINYINT | DEFAULT 0 | 软删除 |
| create_time | DATETIME | NOT NULL | |
| create_by | VARCHAR(64) | | |
| update_time | DATETIME | | |
| update_by | VARCHAR(64) | | |

**索引**: `idx_customer_id`, `idx_mobile`

### 2.3 crm_high_seas_record（公海记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| customer_id | BIGINT | NOT NULL | 客户ID |
| action_type | VARCHAR(32) | | 操作类型（ENTER/CLAIM/ASSIGN/BACK） |
| from_user_id | BIGINT | | 原归属人 |
| to_user_id | BIGINT | | 新归属人 |
| reason | VARCHAR(256) | | 原因 |
| create_time | DATETIME | NOT NULL | 操作时间 |

### 2.4 crm_follow_record（跟进记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| customer_id | BIGINT | NOT NULL | 客户ID |
| type | VARCHAR(32) | | 跟进类型（电话/拜访/邮件/其他） |
| content | TEXT | | 跟进内容 |
| next_follow_time | DATETIME | | 下次跟进时间 |
| create_by | VARCHAR(64) | | 跟进人 |
| create_time | DATETIME | NOT NULL | 跟进时间 |

---

## 3. 销售域（Sales Domain）

### 3.1 crm_lead（线索表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| name | VARCHAR(128) | | 线索名称 |
| company_name | VARCHAR(128) | | 公司名称 |
| mobile | VARCHAR(20) | | 联系电话 |
| source | VARCHAR(32) | | 线索来源 |
| status | VARCHAR(32) | NOT NULL | 状态（新线索/跟进中/已转化/无效） |
| owner_user_id | BIGINT | | 负责人 |
| converted_customer_id | BIGINT | | 转化后的客户ID |
| converted_opportunity_id | BIGINT | | 转化后的商机ID |
| remark | VARCHAR(512) | | 备注 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 3.2 crm_opportunity（商机表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| name | VARCHAR(128) | NOT NULL | 商机名称 |
| customer_id | BIGINT | NOT NULL | 关联客户ID |
| stage | VARCHAR(32) | NOT NULL | 商机阶段（初步接触/需求分析/方案报价/谈判/成交/丢单） |
| amount | DECIMAL(18,2) | | 预计金额 |
| probability | TINYINT | | 赢单概率(%) |
| expected_close_date | DATE | | 预计成交日期 |
| competitor | VARCHAR(256) | | 竞争对手 |
| owner_user_id | BIGINT | | 负责人 |
| remark | VARCHAR(512) | | 备注 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

**索引**: `idx_customer_id`, `idx_stage`, `idx_owner_user_id`

### 3.3 crm_quotation（报价表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| opportunity_id | BIGINT | NOT NULL | 关联商机ID |
| total_amount | DECIMAL(18,2) | | 报价总金额 |
| discount | DECIMAL(5,2) | DEFAULT 1.00 | 折扣率 |
| final_amount | DECIMAL(18,2) | | 折后金额 |
| status | VARCHAR(32) | | 状态（草稿/已报价/已确认/已过期） |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 3.4 crm_quotation_item（报价明细表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| quotation_id | BIGINT | NOT NULL | 报价单ID |
| product_id | BIGINT | | 产品ID |
| product_name | VARCHAR(128) | | 产品名称 |
| unit_price | DECIMAL(18,2) | | 单价 |
| quantity | INT | | 数量 |
| subtotal | DECIMAL(18,2) | | 小计 |

### 3.5 crm_order（订单表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| order_no | VARCHAR(32) | NOT NULL UNIQUE | 订单编号（自动生成） |
| customer_id | BIGINT | NOT NULL | 客户ID |
| opportunity_id | BIGINT | | 来源商机ID |
| total_amount | DECIMAL(18,2) | | 订单总金额 |
| status | VARCHAR(32) | NOT NULL | 状态（草稿/待审批/已通过/已驳回/已撤回/已发货/已完成） |
| order_date | DATE | | 下单日期 |
| contract_no | VARCHAR(64) | | 合同编号 |
| contract_signed_date | DATE | | 合同签署日期 |
| remark | VARCHAR(512) | | 备注 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

**索引**: `idx_order_no`, `idx_customer_id`, `idx_status`

### 3.6 crm_order_item（订单明细表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| order_id | BIGINT | NOT NULL | 订单ID |
| product_id | BIGINT | | 产品ID |
| product_name | VARCHAR(128) | | 产品名称 |
| unit_price | DECIMAL(18,2) | | 单价 |
| quantity | INT | | 数量 |
| subtotal | DECIMAL(18,2) | | 小计 |

---

## 4. 财务域（Finance Domain）

### 4.1 crm_receivable（回款表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| order_id | BIGINT | NOT NULL | 关联订单ID |
| plan_id | BIGINT | | 回款计划ID |
| amount | DECIMAL(18,2) | NOT NULL | 回款金额 |
| payment_method | VARCHAR(32) | | 付款方式 |
| payment_date | DATE | | 付款日期 |
| status | VARCHAR(32) | | 状态（待审批/已回款/已驳回） |
| remark | VARCHAR(256) | | 备注 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 4.2 crm_receivable_plan（回款计划表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| order_id | BIGINT | NOT NULL | 订单ID |
| plan_amount | DECIMAL(18,2) | | 计划回款金额 |
| plan_date | DATE | | 计划回款日期 |
| received_amount | DECIMAL(18,2) | DEFAULT 0 | 已回款金额 |
| is_overdue | TINYINT | DEFAULT 0 | 是否逾期 |
| status | VARCHAR(32) | | 状态（待回款/部分回款/全部回款/已逾期） |

### 4.3 crm_invoice（发票表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| order_id | BIGINT | NOT NULL | 关联订单ID |
| invoice_no | VARCHAR(64) | | 发票号码 |
| type | VARCHAR(32) | | 发票类型（增值税普通/增值税专用） |
| amount | DECIMAL(18,2) | | 开票金额 |
| title | VARCHAR(128) | | 发票抬头 |
| tax_no | VARCHAR(64) | | 税号 |
| status | VARCHAR(32) | | 状态（待开票/已开票/已作废） |
| issue_date | DATE | | 开票日期 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 4.4 crm_reimbursement（报销表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| title | VARCHAR(128) | NOT NULL | 报销标题 |
| amount | DECIMAL(18,2) | NOT NULL | 报销金额 |
| type | VARCHAR(32) | | 报销类型（差旅/办公/招待/其他） |
| status | VARCHAR(32) | | 状态（草稿/待审批/已通过/已驳回） |
| process_instance_id | VARCHAR(64) | | BPM流程实例ID |
| applicant_id | BIGINT | | 申请人ID |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 4.5 crm_refund（退款表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| order_id | BIGINT | NOT NULL | 关联订单ID |
| amount | DECIMAL(18,2) | NOT NULL | 退款金额 |
| reason | VARCHAR(512) | | 退款原因 |
| status | VARCHAR(32) | | 状态（待审批/已退款/已驳回） |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 4.6 crm_expense（费用表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| type | VARCHAR(32) | | 费用类型 |
| amount | DECIMAL(18,2) | | 金额 |
| customer_id | BIGINT | | 关联客户 |
| order_id | BIGINT | | 关联订单 |
| remark | VARCHAR(512) | | 备注 |
| expense_date | DATE | | 费用日期 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

---

## 5. 工单域（WorkOrder Domain）

### 5.1 crm_work_order（工单表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| title | VARCHAR(256) | NOT NULL | 工单标题 |
| type | VARCHAR(32) | | 工单类型（字典） |
| priority | VARCHAR(16) | DEFAULT 'NORMAL' | 优先级（LOW/NORMAL/HIGH/URGENT） |
| status | VARCHAR(32) | NOT NULL | 状态（待处理/处理中/已完结/已退回） |
| customer_id | BIGINT | | 关联客户ID |
| assignee_id | BIGINT | | 处理人ID |
| sla_deadline | DATETIME | | SLA截止时间 |
| is_sla_breached | TINYINT | DEFAULT 0 | 是否超SLA |
| content | TEXT | | 工单内容 |
| solution | TEXT | | 解决方案 |
| resolved_time | DATETIME | | 完结时间 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

---

## 6. 营销域（Marketing Domain）

### 6.1 crm_campaign（营销活动表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| name | VARCHAR(128) | NOT NULL | 活动名称 |
| type | VARCHAR(32) | | 活动类型 |
| start_date | DATE | | 开始日期 |
| end_date | DATE | | 结束日期 |
| budget | DECIMAL(18,2) | | 预算 |
| actual_cost | DECIMAL(18,2) | | 实际花费 |
| target_description | TEXT | | 目标客户描述 |
| status | VARCHAR(32) | | 状态（计划中/进行中/已结束） |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 6.2 crm_broadcast（群发表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| channel | VARCHAR(16) | NOT NULL | 渠道（SMS/EMAIL） |
| template_id | VARCHAR(64) | | 模板ID |
| content | TEXT | | 发送内容 |
| target_count | INT | | 目标数量 |
| success_count | INT | DEFAULT 0 | 成功数量 |
| fail_count | INT | DEFAULT 0 | 失败数量 |
| status | VARCHAR(32) | | 状态（待审核/审核通过/发送中/已完成） |
| process_instance_id | VARCHAR(64) | | 审核流程ID |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 6.3 crm_care_rule（关怀规则表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| name | VARCHAR(128) | | 规则名称 |
| trigger_type | VARCHAR(32) | NOT NULL | 触发类型（BIRTHDAY/HOLIDAY/REGULAR） |
| trigger_config | VARCHAR(256) | | 触发配置（cron表达式/日期） |
| template_id | VARCHAR(64) | | 消息模板ID |
| channel | VARCHAR(16) | | 发送渠道（SMS/EMAIL） |
| is_enabled | TINYINT | DEFAULT 1 | 是否启用 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

---

## 7. 办公域（Office Domain）

### 7.1 crm_work_report（工作报告表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| type | VARCHAR(16) | NOT NULL | 类型（DAILY/WEEKLY/MONTHLY） |
| title | VARCHAR(256) | | 标题 |
| content | TEXT | | 内容 |
| report_date | DATE | NOT NULL | 报告日期 |
| user_id | BIGINT | NOT NULL | 提交人 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 7.2 crm_task（任务表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| title | VARCHAR(256) | NOT NULL | 任务标题 |
| description | TEXT | | 任务描述 |
| priority | VARCHAR(16) | | 优先级 |
| status | VARCHAR(32) | | 状态（待办/进行中/已完成） |
| assignee_id | BIGINT | | 负责人 |
| due_date | DATE | | 截止日期 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 7.3 crm_schedule（日程表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| title | VARCHAR(256) | NOT NULL | 日程标题 |
| description | TEXT | | 日程描述 |
| start_time | DATETIME | NOT NULL | 开始时间 |
| end_time | DATETIME | NOT NULL | 结束时间 |
| is_all_day | TINYINT | DEFAULT 0 | 是否全天 |
| remind_before | INT | | 提前提醒（分钟） |
| user_id | BIGINT | NOT NULL | 所属用户 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

### 7.4 crm_document（文档表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| name | VARCHAR(256) | NOT NULL | 文档名称 |
| parent_id | BIGINT | | 父目录ID（目录结构） |
| file_path | VARCHAR(512) | | 文件路径（MinIO） |
| file_size | BIGINT | | 文件大小 |
| file_type | VARCHAR(32) | | 文件类型 |
| is_dir | TINYINT | DEFAULT 0 | 是否目录 |
| is_shared | TINYINT | DEFAULT 0 | 是否共享 |
| deleted | TINYINT | DEFAULT 0 | |
| create_time | DATETIME | NOT NULL | |
| update_time | DATETIME | | |

---

## 8. CRM表统计

| 域 | 表数 | 核心表 |
|----|------|--------|
| 客户域 | 4 | crm_customer, crm_contact, crm_high_seas_record, crm_follow_record |
| 销售域 | 6 | crm_lead, crm_opportunity, crm_quotation, crm_quotation_item, crm_order, crm_order_item |
| 财务域 | 6 | crm_receivable, crm_receivable_plan, crm_invoice, crm_reimbursement, crm_refund, crm_expense |
| 工单域 | 1 | crm_work_order |
| 营销域 | 3 | crm_campaign, crm_broadcast, crm_care_rule |
| 办公域 | 4 | crm_work_report, crm_task, crm_schedule, crm_document |
| **合计** | **24** | |

---

> **备注**：所有CRM表均需在 `InstallPackage/database/base/` 或 `new/` 目录下创建对应的DDL SQL脚本。每张表包含多租户字段 `tenant_id` 和软删除字段 `deleted`。
