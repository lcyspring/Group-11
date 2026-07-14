# STAT-BUG-022：小时统计分桶继续执行日分桶

## 现象

统计请求允许 `interval=0`（HOUR）。公共日期范围工具生成全部小时桶后，由于
`switch` 分支缺少 `break`，继续执行 DAY 分支并追加日桶。

## 影响

同一查询结果包含小时和日两种粒度，调用方按区间聚合时可能重复计算数据。

## 修复

在 HOUR 分支结束后显式 `break`。

## 验证

新增 `CrmStatisticsDateRangeTest`，断言一天只生成 24 个小时桶，且所有区间均
小于一小时；测试已在 Ubuntu 26.04 CRM reactor 中实际执行。
