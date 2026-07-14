# CRM 前端设计文档

---

## 文档信息

| 项目 | 内容 |
|------|------|
| 项目名称 | MITEDTSM 密讯ETM系统 — CRM子系统 |
| 文档类型 | 前端架构与组件设计 |
| 技术栈 | Vue 3.5 + TypeScript + Vite 5 + Element Plus 2 + Pinia + ECharts 5 |
| 文档版本 | V1.0 |
| 创建日期 | 2026-06-25 |

---

## 1. 前端架构概述

CRM前端基于 MITEDTSM 现有 Web 前端架构（`Code/Web/`），采用 Vue 3 + TypeScript + Element Plus 技术栈，按CRM六大业务域组织路由和组件。

### 1.1 目录结构

```
src/
├── views/
│   └── crm/                          # CRM模块根目录
│       ├── customer/                 # 客户域
│       │   ├── CustomerList.vue      # 客户列表（全部/储备/淘汰 Tab）
│       │   ├── CustomerCreate.vue    # 客户创建
│       │   ├── CustomerEdit.vue      # 客户编辑
│       │   ├── CustomerDetail.vue    # 客户详情（Tab切换）
│       │   ├── ContactList.vue       # 联系人列表
│       │   ├── HighSeasList.vue      # 公海客户列表
│       │   ├── CustomerAnalysis.vue  # 客户分析
│       │   └── CustomerImport.vue    # 批量导入
│       ├── sales/                    # 销售域
│       │   ├── LeadList.vue          # 线索列表
│       │   ├── OpportunityList.vue   # 商机列表
│       │   ├── OpportunityCreate.vue # 商机创建
│       │   ├── OpportunityDetail.vue # 商机详情（含阶段推进）
│       │   ├── QuotationCreate.vue   # 报价创建
│       │   ├── OrderList.vue         # 订单列表
│       │   ├── OrderCreate.vue       # 订单创建
│       │   ├── OrderDetail.vue       # 订单详情（含状态流转）
│       │   ├── SalesFunnel.vue       # 销售漏斗
│       │   └── ContractList.vue      # 合同管理
│       ├── finance/                  # 财务域
│       │   ├── ReceivableList.vue    # 回款列表
│       │   ├── ReceivablePlan.vue    # 回款计划
│       │   ├── InvoiceList.vue       # 发票列表
│       │   ├── ReimbursementList.vue # 报销列表
│       │   ├── ReimbursementCreate.vue # 报销申请
│       │   ├── RefundList.vue        # 退款列表
│       │   └── ExpenseList.vue       # 费用列表
│       ├── workorder/                # 工单域
│       │   ├── WorkOrderList.vue     # 工单列表
│       │   ├── WorkOrderCreate.vue   # 工单创建
│       │   └── WorkOrderDetail.vue   # 工单详情（含SLA倒计时）
│       ├── marketing/                # 营销域
│       │   ├── CampaignList.vue      # 营销活动列表
│       │   ├── CampaignCreate.vue    # 营销活动创建
│       │   ├── BroadcastList.vue     # 群发任务列表
│       │   ├── BroadcastCreate.vue   # 创建群发任务
│       │   └── CareRuleList.vue      # 关怀规则管理
│       └── office/                   # 办公域
│           ├── WorkReportList.vue    # 工作报告列表
│           ├── TaskList.vue          # 任务列表（看板视图）
│           ├── ScheduleView.vue      # 日程管理（日历视图）
│           ├── DocumentIndex.vue     # 文档管理（目录树）
│           └── ApprovalCenter.vue    # 统一审批中心
├── api/
│   └── crm/                          # CRM API层
│       ├── customer.ts               # 客户域API
│       ├── sales.ts                  # 销售域API
│       ├── finance.ts                # 财务域API
│       ├── workorder.ts              # 工单域API
│       ├── marketing.ts              # 营销域API
│       └── office.ts                 # 办公域API
├── stores/
│   └── crm/                          # CRM状态管理（Pinia）
│       ├── customer.ts               # 客户域状态
│       └── sales.ts                  # 销售域状态
├── locales/
│   └── zh-CN/
│       └── crm.json                  # CRM中文语言包
│   └── en/
│       └── crm.json                  # CRM英文语言包
└── components/
    └── crm/                          # CRM公共组件
        ├── CustomerSelect.vue        # 客户选择器
        ├── OpportunitySelect.vue     # 商机选择器
        ├── OrderSelect.vue           # 订单选择器
        ├── StageStepper.vue          # 商机阶段进度条
        ├── StatusTag.vue             # 状态标签（统一颜色映射）
        ├── FollowTimeline.vue        # 跟进时间线
        ├── SalesFunnelChart.vue      # 销售漏斗图表
        └── ApprovalStatusBadge.vue   # 审批状态徽章
```

---

## 2. 路由设计

```typescript
// router/modules/crm.ts
export default [
  {
    path: '/crm',
    name: 'CRM',
    redirect: '/crm/customer',
    meta: { title: '客户管理', icon: 'user', permission: 'crm:customer:list' },
    children: [
      // ── 客户域 ──
      { path: 'customer', name: 'CustomerList', meta: { title: '客户列表' } },
      { path: 'customer/create', name: 'CustomerCreate', meta: { title: '创建客户', hidden: true } },
      { path: 'customer/:id', name: 'CustomerDetail', meta: { title: '客户详情', hidden: true } },
      { path: 'customer/:id/edit', name: 'CustomerEdit', meta: { title: '编辑客户', hidden: true } },
      { path: 'contact', name: 'ContactList', meta: { title: '联系人管理' } },
      { path: 'high-seas', name: 'HighSeasList', meta: { title: '公海客户', permission: 'crm:highseas:list' } },
      { path: 'customer/analysis', name: 'CustomerAnalysis', meta: { title: '客户分析' } },

      // ── 销售域 ──
      { path: 'lead', name: 'LeadList', meta: { title: '线索管理' } },
      { path: 'opportunity', name: 'OpportunityList', meta: { title: '商机管理' } },
      { path: 'opportunity/create', name: 'OpportunityCreate', meta: { title: '创建商机', hidden: true } },
      { path: 'opportunity/:id', name: 'OpportunityDetail', meta: { title: '商机详情', hidden: true } },
      { path: 'order', name: 'OrderList', meta: { title: '订单管理' } },
      { path: 'order/create', name: 'OrderCreate', meta: { title: '创建订单', hidden: true } },
      { path: 'order/:id', name: 'OrderDetail', meta: { title: '订单详情', hidden: true } },
      { path: 'sales-funnel', name: 'SalesFunnel', meta: { title: '销售漏斗' } },
      { path: 'contract', name: 'ContractList', meta: { title: '合同管理' } },

      // ── 财务域 ──
      { path: 'receivable', name: 'ReceivableList', meta: { title: '回款管理' } },
      { path: 'invoice', name: 'InvoiceList', meta: { title: '发票管理' } },
      { path: 'reimbursement', name: 'ReimbursementList', meta: { title: '报销管理' } },
      { path: 'refund', name: 'RefundList', meta: { title: '退款管理' } },
      { path: 'expense', name: 'ExpenseList', meta: { title: '费用管理' } },

      // ── 工单域 ──
      { path: 'workorder', name: 'WorkOrderList', meta: { title: '工单管理' } },
      { path: 'workorder/create', name: 'WorkOrderCreate', meta: { title: '创建工单', hidden: true } },
      { path: 'workorder/:id', name: 'WorkOrderDetail', meta: { title: '工单详情', hidden: true } },

      // ── 营销域 ──
      { path: 'campaign', name: 'CampaignList', meta: { title: '营销活动' } },
      { path: 'broadcast', name: 'BroadcastList', meta: { title: '群发管理' } },
      { path: 'care-rule', name: 'CareRuleList', meta: { title: '关怀规则' } },

      // ── 办公域 ──
      { path: 'work-report', name: 'WorkReportList', meta: { title: '工作报告' } },
      { path: 'task', name: 'TaskList', meta: { title: '任务管理' } },
      { path: 'schedule', name: 'ScheduleView', meta: { title: '日程管理' } },
      { path: 'document', name: 'DocumentIndex', meta: { title: '文档管理' } },
      { path: 'approval-center', name: 'ApprovalCenter', meta: { title: '审批中心' } },
    ]
  }
];
```

---

## 3. 核心页面组件设计

### 3.1 列表页模式（List Pattern）

所有CRM列表页遵循统一模式，以客户列表为例：

```vue
<template>
  <div class="crm-list-container">
    <!-- 搜索栏 -->
    <el-form :model="query" inline>
      <el-form-item label="客户名称"><el-input v-model="query.name" /></el-form-item>
      <el-form-item label="客户状态">
        <el-select v-model="query.status"><!-- 字典选项 --></el-select>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker v-model="query.dateRange" type="daterange" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="create">创建客户</el-button>
      <el-button @click="batchDelete">批量删除</el-button>
      <el-button @click="exportExcel">导出Excel</el-button>
    </div>

    <!-- 数据表格 -->
    <el-table :data="list" @selection-change="handleSelection">
      <el-table-column type="selection" />
      <el-table-column prop="name" label="客户名称" />
      <el-table-column prop="status" label="状态"><StatusTag :status="row.status" /></el-table-column>
      <el-table-column prop="primaryContact" label="首联系人" />
      <el-table-column prop="mobile" label="手机号码" />
      <el-table-column prop="industry" label="所属行业" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" fixed="right">
        <el-button link @click="view">详情</el-button>
        <el-button link @click="edit">编辑</el-button>
        <el-button link type="danger" @click="remove">删除</el-button>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="query.pageNo"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
    />
  </div>
</template>
```

### 3.2 表单页模式（Form Pattern）

```vue
<template>
  <div class="crm-form-container">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="客户名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入客户名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="客户状态" prop="status">
            <el-select v-model="form.status"><!-- ... --></el-select>
          </el-form-item>
        </el-col>
        <!-- 更多字段... -->
      </el-row>
      <el-form-item>
        <el-button type="primary" @click="submit">保存</el-button>
        <el-button @click="cancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
```

### 3.3 详情页模式（Detail Pattern）

```vue
<template>
  <div class="crm-detail-container">
    <el-page-header @back="goBack" :content="title" />

    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="客户名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="客户状态"><StatusTag :status="detail.status" /></el-descriptions-item>
          <!-- ... -->
        </el-descriptions>
      </el-tab-pane>
      <el-tab-pane label="联系人" name="contact">
        <ContactList :customer-id="detail.id" />
      </el-tab-pane>
      <el-tab-pane label="商机" name="opportunity">
        <OpportunityList :customer-id="detail.id" />
      </el-tab-pane>
      <el-tab-pane label="跟进记录" name="follow">
        <FollowTimeline :customer-id="detail.id" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
```

---

## 4. 核心业务组件

### 4.1 销售漏斗（SalesFunnelChart.vue）

基于ECharts漏斗图，展示商机各阶段的转化率：

```
初步接触 (100个)           ████████████████████████████████ 100%
需求分析 (75个)            ████████████████████████ 75%
方案报价 (45个)            ██████████████ 45%
商务谈判 (20个)            ██████ 20%
已成交   (12个)            ████ 12%
```

### 4.2 商机阶段推进（StageStepper.vue）

基于 `el-steps` 展示商机当前阶段和流转历史，支持向前推进操作：

```
初步接触 ──→ 需求分析 ──→ 方案报价 ──→ 商务谈判 ──→ 成交
  ✓            ✓           ✓          ●当前        ○
```

### 4.3 跟进时间线（FollowTimeline.vue）

基于 `el-timeline` 展示客户的所有跟进记录，按时间倒序排列。

### 4.4 统一状态标签（StatusTag.vue）

CRM各域状态统一颜色映射：

| 状态类型 | 颜色映射 |
|----------|---------|
| 待处理/待审批 | `warning` (橙) |
| 进行中/处理中 | `primary` (蓝) |
| 已完成/已通过 | `success` (绿) |
| 已驳回/已拒绝 | `danger` (红) |
| 已取消/已撤回 | `info` (灰) |

---

## 5. API层设计

```typescript
// api/crm/customer.ts
import request from '@/utils/request';

export const CustomerApi = {
  // GET /admin-api/crm/customer/page — 分页列表
  page: (params: CustomerQuery) => request.get('/crm/customer/page', { params }),

  // GET /admin-api/crm/customer/{id} — 客户详情
  getById: (id: number) => request.get(`/crm/customer/${id}`),

  // POST /admin-api/crm/customer — 创建客户
  create: (data: CustomerCreateDTO) => request.post('/crm/customer', data),

  // PUT /admin-api/crm/customer/{id} — 更新客户
  update: (id: number, data: CustomerUpdateDTO) => request.put(`/crm/customer/${id}`, data),

  // DELETE /admin-api/crm/customer/{id} — 删除客户
  delete: (id: number) => request.delete(`/crm/customer/${id}`),

  // POST /admin-api/crm/customer/batch-delete — 批量删除
  batchDelete: (ids: number[]) => request.post('/crm/customer/batch-delete', { ids }),

  // POST /admin-api/crm/customer/export — 导出Excel
  export: (params: CustomerQuery) => request.post('/crm/customer/export', params, { responseType: 'blob' }),

  // POST /admin-api/crm/customer/{id}/transfer — 客户转移
  transfer: (id: number, data: TransferDTO) => request.post(`/crm/customer/${id}/transfer`, data),
};
```

---

## 6. 状态管理（Pinia）

```typescript
// stores/crm/customer.ts
import { defineStore } from 'pinia';

export const useCustomerStore = defineStore('crm-customer', {
  state: () => ({
    list: [] as CustomerVO[],
    current: null as CustomerVO | null,
    total: 0,
    loading: false,
    query: {
      pageNo: 1,
      pageSize: 20,
      name: '',
      status: '',
      industry: '',
    } as CustomerQuery,
  }),
  actions: {
    async fetchPage() {
      this.loading = true;
      const { data } = await CustomerApi.page(this.query);
      this.list = data.records;
      this.total = data.total;
      this.loading = false;
    },
    async fetchById(id: number) {
      const { data } = await CustomerApi.getById(id);
      this.current = data;
    },
  },
});
```

---

## 7. 国际化（i18n）

```json
// locales/zh-CN/crm.json
{
  "crm.customer": "客户管理",
  "crm.customer.list": "客户列表",
  "crm.customer.create": "创建客户",
  "crm.customer.name": "客户名称",
  "crm.customer.status": "客户状态",
  "crm.customer.status.potential": "潜在客户",
  "crm.customer.status.intended": "意向客户",
  "crm.customer.status.deal": "成交客户",
  "crm.customer.status.lost": "流失客户",
  "crm.highseas": "公海管理",
  "crm.contact": "联系人管理",
  "crm.opportunity": "商机管理",
  "crm.opportunity.stage": "商机阶段",
  "crm.order": "订单管理",
  "crm.sales.funnel": "销售漏斗",
  "crm.finance.receivable": "回款管理",
  "crm.workorder": "工单管理",
  "crm.marketing.campaign": "营销活动",
  "crm.office.approval": "审批中心"
}
```

---

## 8. 菜单注册

CRM前端菜单通过后端 `system_menu` + `system_menu_i18n` 动态加载，前端路由的 `meta.permission` 对应后端菜单的 `permission` 字段：

| 一级菜单 | 二级菜单 | permission |
|----------|---------|------------|
| 客户管理 | 客户列表 | crm:customer:list |
| 客户管理 | 联系人管理 | crm:contact:list |
| 客户管理 | 公海客户 | crm:highseas:list |
| 客户管理 | 客户分析 | crm:customer:analysis |
| 销售管理 | 线索管理 | crm:lead:list |
| 销售管理 | 商机管理 | crm:opportunity:list |
| 销售管理 | 订单管理 | crm:order:list |
| 销售管理 | 销售漏斗 | crm:sales:funnel |
| 销售管理 | 合同管理 | crm:contract:list |
| 财务管理 | 回款管理 | crm:receivable:list |
| 财务管理 | 发票管理 | crm:invoice:list |
| 财务管理 | 报销管理 | crm:reimbursement:list |
| 财务管理 | 退款管理 | crm:refund:list |
| 财务管理 | 费用管理 | crm:expense:list |
| 工单管理 | 工单列表 | crm:workorder:list |
| 营销工具 | 营销活动 | crm:campaign:list |
| 营销工具 | 群发管理 | crm:broadcast:list |
| 营销工具 | 关怀规则 | crm:care:rule:list |
| 办公协作 | 工作报告 | crm:report:list |
| 办公协作 | 任务管理 | crm:task:list |
| 办公协作 | 日程管理 | crm:schedule:list |
| 办公协作 | 文档管理 | crm:document:list |
| 办公协作 | 审批中心 | crm:approval:center |

---

> **参考**: 前端架构请同时参考 `01-Existing-System-Analysis/07-Frontend-Architecture.md` 和 `Code/Web/` 目录下的现有代码规范。
