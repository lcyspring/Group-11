# CRM 工单统计（GAP-WO-STAT-001）

## 目标

把客服工单纳入 CRM 数据统计，覆盖原型要求的状态、类型、处理人和时间趋势。

## 接口

| 接口 | 口径 |
| --- | --- |
| `/admin-api/crm/statistics-work-order/summary` | 按创建时间筛选的总量和状态汇总 |
| `/admin-api/crm/statistics-work-order/by-status` | 按创建时间、状态分组 |
| `/admin-api/crm/statistics-work-order/by-type` | 按创建时间、类型分组 |
| `/admin-api/crm/statistics-work-order/by-handler` | 按创建时间、处理人分组 |
| `/admin-api/crm/statistics-work-order/trend` | 创建趋势使用 `create_time`，完结趋势使用 `complete_time` |

## 权限与隔离

- 接口权限：`crm:statistics-work-order:query`；
- 默认范围：当前用户创建或处理的工单；
- 额外拥有 `crm:work-order:query-all` 时可查看当前租户全部工单；
- SQL 显式包含 `tenant_id`、`deleted = 0`，且不连接操作轨迹表，避免一单多轨迹重复计数。

## 前端

新增 `crm/statistics/workorder/index` 页面，提供时间范围/间隔筛选、汇总卡片、创建与完结趋势、状态/类型/处理人明细。
