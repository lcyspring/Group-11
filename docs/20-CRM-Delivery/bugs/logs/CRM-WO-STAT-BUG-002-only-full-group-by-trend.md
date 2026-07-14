# CRM-WO-STAT-BUG-002：工单趋势 SQL 不兼容 only_full_group_by

## 现象

工单统计汇总、状态、类型、处理人接口均正常，但 `/trend` 返回 500。MySQL 报：

`Expression #1 of SELECT list is not in GROUP BY clause ... only_full_group_by`

## 根因

趋势子查询选择 `DATE_FORMAT(create_time, ...)`，却只按 `DATE(create_time)` 分组。
MySQL 8 的严格 SQL 模式不接受这种非同一表达式分组。

## 修复

创建和完结子查询的 SELECT 与 GROUP BY 均使用完全相同的
`DATE_FORMAT(..., '%Y-%m-%dT00:00:00')` 表达式，保持日桶语义并兼容严格模式。

## 验证

修复后重新执行 CRM 单测和 Server Ubuntu 26.04 构建，并对 `/trend` 做真实 HTTP 回归；
要求返回 `code=0`，空日期桶由服务层补齐。
