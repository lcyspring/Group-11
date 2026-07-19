# CRM 跟进统计正确性修复

更新日期：2026-07-14

## 来源

- `GAP-RPT-001`：现有 CRM 统计可复用，但必须逐指标映射并明确口径。
- 员工客户分析页面中的跟进次数、跟进客户数和跟进方式统计。

## 发现

1. Service 从 Mapper 分别取得跟进记录数和去重客户数后，在按日期、按员工两个响应中写入了相反字段。
2. 五条员工客户分析 SQL 没有过滤 `crm_follow_up_record.deleted = 0`，逻辑删除记录仍进入指标；排行榜的相同来源已经过滤，两个页面口径不一致。

## 实现

- 修正 `getFollowUpSummaryByDate` 与 `getFollowUpSummaryByUser` 的响应字段映射。
- 按日期记录数、按日期客户数、按员工记录数、按员工客户数、按跟进方式五条 SQL 统一排除逻辑删除。
- 增加 Service 数值哨兵测试，使用 7 条记录/3 个客户防止同值掩盖字段互换。
- 增加 Mapper XML 回归，锁定五条 SQL 的逻辑删除条件。

## 边界

- 本次只修复既有指标的计算正确性，不改变时间字段、人员范围或数据权限算法。
- 真正的 MySQL/API 双角色数据集仍属于 `STAT-DATA-001`、`STAT-SEC-001` 后续对账范围。

测试证据见 `docs/20-CRM-Delivery/testing/follow-up-statistics-correctness/`，Bug 详情见 `STAT-BUG-019`、`STAT-BUG-020` 独立日志。
