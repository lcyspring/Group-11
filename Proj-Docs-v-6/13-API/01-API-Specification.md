# CRM + OA 子系统 REST API 规范

## 1. 通用规范

### 1.1 URL 前缀

```
/admin-api/crm/{domain}/{resource}   # 管理后台接口 (Web Admin)
/app-api/crm/{domain}/{resource}     # 移动端接口 (AdminMobile/Portal)
```

### 1.2 HTTP 方法约定

| 方法 | 用途 | URL 示例 |
|------|------|---------|
| GET | 单条查询 | `/admin-api/crm/customer/get?id=1` |
| POST | 分页查询/新增 | `/admin-api/crm/customer/page`, `/admin-api/crm/customer/create` |
| PUT | 修改 | `/admin-api/crm/customer/update` |
| DELETE | 逻辑删除 | `/admin-api/crm/customer/delete?id=1` |

### 1.3 统一返回格式

```json
// 成功 (CommonResult<T>)
{ "code": 0, "msg": "操作成功", "data": {...} }

// 分页 (CommonResult<PageResult<T>>)
{ "code": 0, "data": { "list": [...], "total": 100 } }

// 失败
{ "code": 1001001, "msg": "客户名称已存在" }
```

### 1.4 认证与权限

- Header: `Authorization: Bearer {accessToken}`
- 权限控制: `@PreAuthorize("@ss.hasPermission('crm:customer:create')")`
- 操作日志: `@BizLog(module = "CRM", type = "客户管理", value = "创建客户")`
- 多租户: 自动通过 `TenantContextHolder` 注入 `tenant_id`

### 1.5 分页参数标准

```json
// Request (POST /page)
{
  "pageNo": 1,
  "pageSize": 20,
  "keyword": "搜索关键词",
  "status": 1,
  "createTimeStart": "2026-01-01",
  "createTimeEnd": "2026-03-30",
  "orderBy": "create_time desc"
}
```

---

## 2. 客户域 API (Customer Domain)

### 2.1 客户管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 分页查询 | POST | `/admin-api/crm/customer/page` | `crm:customer:query` | 支持关键词/状态/来源/行业/归属/时间筛选 |
| 详情查询 | GET | `/admin-api/crm/customer/get` | `crm:customer:query` | 含联系人列表+跟进记录 |
| 创建客户 | POST | `/admin-api/crm/customer/create` | `crm:customer:create` | 自动查重校验 |
| 更新客户 | PUT | `/admin-api/crm/customer/update` | `crm:customer:update` | 含星级/状态/标签更新 |
| 删除客户 | DELETE | `/admin-api/crm/customer/delete` | `crm:customer:delete` | 逻辑删除, 关联联系人一并删除 |
| 客户转移 | PUT | `/admin-api/crm/customer/transfer` | `crm:customer:transfer` | 变更归属人, 生成转移记录 |
| 客户查重 | POST | `/admin-api/crm/customer/check-duplicate` | `crm:customer:create` | 名称+手机模糊匹配 |
| 导入Excel | POST | `/admin-api/crm/customer/import` | `crm:customer:import` | MultipartFile, FastExcel解析 |
| 导出Excel | GET | `/admin-api/crm/customer/export` | `crm:customer:export` | 按筛选条件导出 |

#### 2.1.1 创建客户 Request

```json
{
  "name": "XX科技有限公司",
  "status": 1,
  "source": 1,
  "industry": 2,
  "starRating": 3,
  "country": "中国",
  "province": "广东省",
  "city": "深圳市",
  "address": "南山区科技园",
  "ownerId": 10001,
  "contacts": [
    {
      "name": "张三",
      "mobile": "13800138000",
      "email": "zhangsan@xx.com",
      "department": "技术部",
      "position": "CTO"
    }
  ],
  "notes": "重点客户"
}
```

#### 2.1.2 客户分页查询 Response

```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "name": "XX科技有限公司",
        "status": 1,
        "statusName": "正常",
        "source": 1,
        "sourceName": "线上推广",
        "industry": 2,
        "industryName": "信息技术",
        "starRating": 3,
        "firstContactName": "张三",
        "firstContactMobile": "13800138000",
        "ownerName": "李销售",
        "createTime": "2026-03-24 10:00:00"
      }
    ],
    "total": 100
  }
}
```

### 2.2 联系人管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 列表查询 | GET | `/admin-api/crm/contact/list?customerId=1` | `crm:contact:query` | 按客户ID查所有联系人 |
| 创建 | POST | `/admin-api/crm/contact/create` | `crm:contact:create` | 关联客户 |
| 更新 | PUT | `/admin-api/crm/contact/update` | `crm:contact:update` | |
| 删除 | DELETE | `/admin-api/crm/contact/delete` | `crm:contact:delete` | 逻辑删除 |

### 2.3 公海管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 公海列表 | POST | `/admin-api/crm/sea/page` | `crm:sea:query` | 公海客户池 |
| 领取客户 | PUT | `/admin-api/crm/sea/claim` | `crm:sea:claim` | 从公海领取到个人 |
| 公海规则配置 | PUT | `/admin-api/crm/sea/config` | `crm:sea:config` | 配置自动掉入天数/条件 |
| 手动掉入 | PUT | `/admin-api/crm/sea/move-in` | `crm:sea:move-in` | 手动将客户移入公海 |

### 2.4 客户分析

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 属性分析 | GET | `/admin-api/crm/customer/analysis/attribute` | `crm:customer:analysis` | 按状态/来源/行业聚合 |
| 区域分布 | GET | `/admin-api/crm/customer/analysis/region` | `crm:customer:analysis` | 国家/省份地图数据 |
| 成交分析 | GET | `/admin-api/crm/customer/analysis/conversion` | `crm:customer:analysis` | 成交率/周期/TOP10 |

---

## 3. 商机域 API (Opportunity Domain)

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 分页查询 | POST | `/admin-api/crm/opportunity/page` | `crm:opportunity:query` | 支持阶段/金额/时间筛选 |
| 详情 | GET | `/admin-api/crm/opportunity/get?id=1` | `crm:opportunity:query` | 含报价列表+跟进记录 |
| 创建 | POST | `/admin-api/crm/opportunity/create` | `crm:opportunity:create` | 关联客户 |
| 更新 | PUT | `/admin-api/crm/opportunity/update` | `crm:opportunity:update` | |
| 删除 | DELETE | `/admin-api/crm/opportunity/delete` | `crm:opportunity:delete` | 逻辑删除 |
| 阶段推进 | PUT | `/admin-api/crm/opportunity/advance-stage` | `crm:opportunity:advance` | 状态机校验, 记录阶段变更日志 |
| 成交 | PUT | `/admin-api/crm/opportunity/win` | `crm:opportunity:win` | 发布 OpportunityWonEvent → 创建订单 |
| 输单 | PUT | `/admin-api/crm/opportunity/lose` | `crm:opportunity:lose` | 记录输单原因 |
| 销售漏斗 | GET | `/admin-api/crm/opportunity/funnel` | `crm:opportunity:analysis` | 各阶段数量+金额+转化率 |
| 销售预测 | GET | `/admin-api/crm/opportunity/prediction` | `crm:opportunity:analysis` | 按时间段的预测金额 |

### 3.1 阶段推进 Request

```json
{
  "id": 100,
  "targetStage": "PROPOSAL",
  "notes": "已发送方案, 等待客户反馈"
}
```

### 3.2 销售漏斗 Response

```json
{
  "code": 0,
  "data": {
    "stages": [
      { "stage": "LEAD", "name": "初步接触", "count": 50, "amount": 5000000, "rate": "100%" },
      { "stage": "NEEDS", "name": "需求分析", "count": 30, "amount": 3500000, "rate": "60%" },
      { "stage": "PROPOSAL", "name": "方案报价", "count": 15, "amount": 2000000, "rate": "50%" },
      { "stage": "NEGOTIATION", "name": "谈判", "count": 8, "amount": 1200000, "rate": "53%" },
      { "stage": "CLOSED_WON", "name": "已成交", "count": 5, "amount": 800000, "rate": "62%" }
    ]
  }
}
```

### 3.3 产品报价

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 报价行列表 | GET | `/admin-api/crm/quotation/list?opportunityId=1` | `crm:quotation:query` | |
| 添加报价行 | POST | `/admin-api/crm/quotation/create` | `crm:quotation:create` | 选择产品+数量+折扣 |
| 更新报价行 | PUT | `/admin-api/crm/quotation/update` | `crm:quotation:update` | |
| 删除报价行 | DELETE | `/admin-api/crm/quotation/delete` | `crm:quotation:delete` | |

### 3.4 跟进记录

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 跟进列表 | GET | `/admin-api/crm/follow-up/list?opportunityId=1` | `crm:follow-up:query` | |
| 添加跟进 | POST | `/admin-api/crm/follow-up/create` | `crm:follow-up:create` | |
| 添加跟进(客户拜访自动) | POST | `/admin-api/crm/follow-up/create-from-visit` | 内部调用 | OA客户拜访通过后自动生成 |

---

## 4. 订单域 API (Order Domain)

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 分页查询 | POST | `/admin-api/crm/order/page` | `crm:order:query` | 支持状态/客户/时间筛选 |
| 详情 | GET | `/admin-api/crm/order/get?id=1` | `crm:order:query` | 含产品行+审批记录 |
| 创建 | POST | `/admin-api/crm/order/create` | `crm:order:create` | 自动生成编号 |
| 更新 | PUT | `/admin-api/crm/order/update` | `crm:order:update` | 仅草稿/已驳回状态可编辑 |
| 删除 | DELETE | `/admin-api/crm/order/delete` | `crm:order:delete` | 仅草稿可删除 |
| 从商机创建 | POST | `/admin-api/crm/order/create-from-opportunity` | `crm:order:create` | 接收 OpportunityWonEvent, 自动填充 |
| 提交审批 | POST | `/admin-api/crm/order/submit-approval` | `crm:order:submit` | 启动 BPM 流程 |
| 审批操作 | PUT | `/admin-api/crm/order/approve` | `crm:order:approve` | 通过/驳回/否决 |
| 订单报表 | POST | `/admin-api/crm/order/report` | `crm:order:report` | 按产品/人员/时间/客户聚合 |

### 4.1 创建订单 Request

```json
{
  "customerId": 1,
  "opportunityId": 100,
  "items": [
    { "productId": 10, "productName": "CRM标准版", "quantity": 2, "unitPrice": 50000, "discount": 0.9 },
    { "productId": 11, "productName": "实施服务", "quantity": 1, "unitPrice": 20000, "discount": 1.0 }
  ],
  "totalAmount": 110000,
  "discountAmount": 5000,
  "finalAmount": 105000,
  "notes": "合同有效期1年"
}
```

### 4.2 订单状态枚举

| 状态码 | 状态名 | 说明 | 可执行操作 |
|:------:|--------|------|-----------|
| 0 | 草稿 | 初始状态 | 编辑/删除/提交 |
| 1 | 待审批 | 已提交BPM | 撤销 |
| 2 | 审批中 | 一级审批通过, 进入二级 | - |
| 3 | 已通过 | 审批全部通过 | 履约 |
| 4 | 已驳回 | 审批驳回 | 编辑/重新提交 |
| 5 | 已否决 | 审批否决(终态) | - |
| 6 | 已撤销 | 提交人撤销 | 重新编辑 |

### 4.3 合同管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 合同分页 | POST | `/admin-api/crm/contract/page` | `crm:contract:query` | |
| 创建合同 | POST | `/admin-api/crm/contract/create` | `crm:contract:create` | 关联订单 |
| 上传合同文件 | POST | `/admin-api/crm/contract/upload` | `crm:contract:update` | 关联文件管理 |

---

## 5. 财务域 API (Finance Domain)

### 5.1 回款管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 分页查询 | POST | `/admin-api/crm/receipt/page` | `crm:receipt:query` | 支持状态/客户/逾期筛选 |
| 创建 | POST | `/admin-api/crm/receipt/create` | `crm:receipt:create` | 关联订单, 可分期 |
| 提交审批 | POST | `/admin-api/crm/receipt/submit-approval` | `crm:receipt:submit` | BPM审批 |
| 回款计划 | POST | `/admin-api/crm/receipt/plan/create` | `crm:receipt:create` | 分期回款计划 |
| 逾期列表 | GET | `/admin-api/crm/receipt/overdue` | `crm:receipt:query` | 定时任务自动标记 |

### 5.2 发票管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 分页查询 | POST | `/admin-api/crm/invoice/page` | `crm:invoice:query` | |
| 开票申请 | POST | `/admin-api/crm/invoice/create` | `crm:invoice:create` | 关联订单/回款 |
| 开票记录 | PUT | `/admin-api/crm/invoice/record` | `crm:invoice:update` | 记录发票号/开票日期 |

### 5.3 报销管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 分页查询 | POST | `/admin-api/crm/reimbursement/page` | `crm:reimbursement:query` | |
| 创建 | POST | `/admin-api/crm/reimbursement/create` | `crm:reimbursement:create` | 含费用明细 |
| 提交审批 | POST | `/admin-api/crm/reimbursement/submit-approval` | `crm:reimbursement:submit` | BPM审批 |
| 关联出差 | PUT | `/admin-api/crm/reimbursement/link-trip` | `crm:reimbursement:update` | 关联OA出差申请 |

### 5.4 退款管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 分页查询 | POST | `/admin-api/crm/refund/page` | `crm:refund:query` | |
| 创建 | POST | `/admin-api/crm/refund/create` | `crm:refund:create` | 关联订单 |
| 提交审批 | POST | `/admin-api/crm/refund/submit-approval` | `crm:refund:submit` | BPM审批 |

### 5.5 财务分析

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 财务汇总 | POST | `/admin-api/crm/finance/summary` | `crm:finance:analysis` | 应收/应付/回款率/逾期率 |
| 回款趋势 | GET | `/admin-api/crm/finance/receipt-trend` | `crm:finance:analysis` | 按月回款趋势图 |

---

## 6. 工单域 API (WorkOrder Domain)

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 分页查询 | POST | `/admin-api/crm/work-order/page` | `crm:work-order:query` | 支持状态/类型/优先级/处理人筛选 |
| 详情 | GET | `/admin-api/crm/work-order/get?id=1` | `crm:work-order:query` | 含处理记录 |
| 创建 | POST | `/admin-api/crm/work-order/create` | `crm:work-order:create` | |
| 分配 | PUT | `/admin-api/crm/work-order/assign` | `crm:work-order:assign` | 分配给处理人 |
| 开始处理 | PUT | `/admin-api/crm/work-order/start` | `crm:work-order:process` | 状态: 待处理→处理中 |
| 完结 | PUT | `/admin-api/crm/work-order/complete` | `crm:work-order:complete` | 填写处理结果 |
| 退回 | PUT | `/admin-api/crm/work-order/reject` | `crm:work-order:reject` | 退回发起人 |
| SLA配置 | PUT | `/admin-api/crm/work-order/sla-config` | `crm:work-order:sla` | 配置超时规则 |
| 工单统计 | POST | `/admin-api/crm/work-order/statistics` | `crm:work-order:analysis` | 处理量/效率/满意度 |

### 6.1 知识库 (扩展)

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 文章列表 | POST | `/admin-api/crm/kb/article/page` | `crm:kb:query` | 全文搜索 |
| 文章详情 | GET | `/admin-api/crm/kb/article/get` | `crm:kb:query` | |
| 创建文章 | POST | `/admin-api/crm/kb/article/create` | `crm:kb:create` | 富文本编辑器 |
| 更新文章 | PUT | `/admin-api/crm/kb/article/update` | `crm:kb:update` | |

---

## 7. 营销域 API (Marketing Domain)

### 7.1 营销活动

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 分页查询 | POST | `/admin-api/crm/campaign/page` | `crm:campaign:query` | |
| 创建 | POST | `/admin-api/crm/campaign/create` | `crm:campaign:create` | |
| 开始/结束 | PUT | `/admin-api/crm/campaign/change-status` | `crm:campaign:update` | |

### 7.2 短信管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 模板列表 | POST | `/admin-api/crm/sms/template/page` | `crm:sms:template:query` | |
| 创建模板 | POST | `/admin-api/crm/sms/template/create` | `crm:sms:template:create` | 含变量占位符 |
| 群发任务 | POST | `/admin-api/crm/sms/broadcast/create` | `crm:sms:broadcast:create` | 选择客户群体+模板 |
| 提交审核 | POST | `/admin-api/crm/sms/broadcast/submit-audit` | `crm:sms:broadcast:submit` | BPM审核 |
| 群发记录 | POST | `/admin-api/crm/sms/log/page` | `crm:sms:log:query` | 发送状态/到达率 |
| 发送分析 | GET | `/admin-api/crm/sms/analysis` | `crm:sms:analysis` | 到达率/打开率统计 |

### 7.3 邮件管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 模板列表 | POST | `/admin-api/crm/email/template/page` | `crm:email:template:query` | |
| 创建模板 | POST | `/admin-api/crm/email/template/create` | `crm:email:template:create` | 富文本HTML编辑 |
| 群发任务 | POST | `/admin-api/crm/email/broadcast/create` | `crm:email:broadcast:create` | |
| 提交审核 | POST | `/admin-api/crm/email/broadcast/submit-audit` | `crm:email:broadcast:submit` | BPM审核 |
| 群发记录 | POST | `/admin-api/crm/email/log/page` | `crm:email:log:query` | |
| 发送分析 | GET | `/admin-api/crm/email/analysis` | `crm:email:analysis` | |

### 7.4 客户关怀

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 关怀规则列表 | POST | `/admin-api/crm/customer-care/rule/page` | `crm:care:query` | |
| 创建规则 | POST | `/admin-api/crm/customer-care/rule/create` | `crm:care:create` | 生日/节日/周年 |
| 发送记录 | POST | `/admin-api/crm/customer-care/log/page` | `crm:care:query` | |

---

## 8. OA 域 API (Office Automation Domain)

### 8.1 审批中心 (统一入口)

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 我的待办 | POST | `/admin-api/oa/approval/pending` | `oa:approval:query` | 跨域聚合所有待审批项 |
| 我的已办 | POST | `/admin-api/oa/approval/done` | `oa:approval:query` | |
| 我发起的 | POST | `/admin-api/oa/approval/initiated` | `oa:approval:query` | |
| 流程详情 | GET | `/admin-api/oa/approval/detail` | `oa:approval:query` | 含审批历史+流程图 |

### 8.2 请假管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 请假列表(个人) | POST | `/admin-api/oa/leave/my/page` | `oa:leave:query` | |
| 创建申请 | POST | `/admin-api/oa/leave/create` | `oa:leave:create` | 自动计算天数+余额校验 |
| 提交审批 | POST | `/admin-api/oa/leave/submit-approval` | `oa:leave:submit` | BPM |
| 假期余额 | GET | `/admin-api/oa/leave/balance` | `oa:leave:query` | 当前剩余年假/事假/病假 |

### 8.3 出差管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 出差列表 | POST | `/admin-api/oa/trip/my/page` | `oa:trip:query` | |
| 创建申请 | POST | `/admin-api/oa/trip/create` | `oa:trip:create` | |
| 提交审批 | POST | `/admin-api/oa/trip/submit-approval` | `oa:trip:submit` | BPM |

### 8.4 借款管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 借款列表 | POST | `/admin-api/oa/loan/my/page` | `oa:loan:query` | |
| 创建申请 | POST | `/admin-api/oa/loan/create` | `oa:loan:create` | |
| 提交审批 | POST | `/admin-api/oa/loan/submit-approval` | `oa:loan:submit` | BPM |
| 还款记录 | POST | `/admin-api/oa/loan/repayment/create` | `oa:loan:update` | |

### 8.5 客户拜访

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 拜访列表 | POST | `/admin-api/oa/visit/my/page` | `oa:visit:query` | |
| 创建申请 | POST | `/admin-api/oa/visit/create` | `oa:visit:create` | 关联客户/商机 |
| 提交审批 | POST | `/admin-api/oa/visit/submit-approval` | `oa:visit:submit` | BPM, 通过后自动生成跟进记录 |

### 8.6 工作报告

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 我的报告 | POST | `/admin-api/oa/work-report/my/page` | `oa:work-report:query` | |
| 写日报 | POST | `/admin-api/oa/work-report/create` | `oa:work-report:create` | 类型: 日报/周报/月报 |
| 查看下属报告 | POST | `/admin-api/oa/work-report/subordinate/page` | `oa:work-report:view-sub` | 管理者视图 |

### 8.7 任务管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 任务列表 | POST | `/admin-api/oa/task/page` | `oa:task:query` | 看板视图+列表视图 |
| 创建任务 | POST | `/admin-api/oa/task/create` | `oa:task:create` | 可设置截止时间/优先级 |
| 更新状态 | PUT | `/admin-api/oa/task/change-status` | `oa:task:update` | 待办→进行中→已完成 |
| 分配任务 | PUT | `/admin-api/oa/task/assign` | `oa:task:assign` | |

### 8.8 日程管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 日程列表 | GET | `/admin-api/oa/calendar/events?view=month&date=2026-03` | `oa:calendar:query` | 日/周/月视图 |
| 创建日程 | POST | `/admin-api/oa/calendar/create` | `oa:calendar:create` | |
| 更新日程 | PUT | `/admin-api/oa/calendar/update` | `oa:calendar:update` | |
| 删除日程 | DELETE | `/admin-api/oa/calendar/delete` | `oa:calendar:delete` | |

### 8.9 文档管理

| 接口 | 方法 | URL | 权限 | 说明 |
|------|:----:|-----|------|------|
| 目录树 | GET | `/admin-api/oa/document/tree` | `oa:document:query` | 文件夹结构 |
| 文档列表 | GET | `/admin-api/oa/document/list?folderId=1` | `oa:document:query` | |
| 上传文档 | POST | `/admin-api/oa/document/upload` | `oa:document:create` | 关联infra文件服务 |
| 创建文件夹 | POST | `/admin-api/oa/document/folder/create` | `oa:document:create` | |
| 文档搜索 | GET | `/admin-api/oa/document/search?keyword=xxx` | `oa:document:query` | |

---

## 9. 移动端 API (App API)

移动端接口前缀为 `/app-api/crm/`，与 Web Admin 接口逻辑相同，但返回数据更精简，分页默认 10 条。

| 模块 | 关键接口 | 说明 |
|------|---------|------|
| 客户 | `/app-api/crm/customer/page`, `/app-api/crm/customer/get` | 移动端简化字段 |
| 商机 | `/app-api/crm/opportunity/page`, `/app-api/crm/opportunity/funnel` | 快速录入跟进 |
| 订单 | `/app-api/crm/order/page`, `/app-api/crm/order/approve` | 移动审批 |
| 审批 | `/app-api/oa/approval/pending` | 移动端统一待办 |
| OA | `/app-api/oa/leave/create`, `/app-api/oa/work-report/create` | 移动端OA |

---

## 10. 错误码规范

| 错误码 | 说明 |
|:------:|------|
| 0 | 成功 |
| 1001001 | 客户名称已存在 |
| 1001002 | 客户不存在 |
| 1001003 | 公海领取超过每日上限 |
| 1002001 | 商机阶段流转不合法 |
| 1002002 | 商机已成交/已输单, 不可修改 |
| 1003001 | 订单编号生成失败 |
| 1003002 | 订单非草稿状态, 不可编辑 |
| 1003003 | 订单审批流转异常 |
| 1004001 | 回款金额超过订单金额 |
| 1004002 | 发票已开具, 不可重复 |
| 1005001 | 工单已超SLA, 自动升级 |
| 1006001 | 短信余额不足 |
| 1006002 | 群发审核未通过 |
| 1007001 | 假期余额不足 |
| 1007002 | 工作日计算异常 |
| 1009999 | 系统内部错误 |

---

## 11. WebSocket 推送

| 事件 | 推送对象 | 内容 |
|------|---------|------|
| `approval.new` | 审批人 | { "type": "leave", "title": "张三-请假申请", "url": "..." } |
| `task.overdue` | 任务负责人 | { "taskId": 1, "title": "完成报告", "overdueDays": 2 } |
| `workorder.overdue` | 工单处理人 | { "woId": 1, "slaLevel": "CRITICAL" } |
| `sea.reminder` | 客户负责人 | { "customerId": 1, "name": "XX公司", "daysLeft": 3 } |

WebSocket 连接: `ws://host:port/ws?token={accessToken}`
