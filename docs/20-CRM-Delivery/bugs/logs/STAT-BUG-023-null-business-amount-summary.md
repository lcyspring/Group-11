# STAT-BUG-023：商机金额全为空时统计接口返回空值

## 现象

`crm_business.total_price` 允许为空。按结束状态或日期执行 `SUM(total_price)`
时，如果分组内金额全为空，MySQL 返回 `NULL`。

## 影响

前端金额图表得到空值；服务层在跨区间累加时也可能发生空指针异常。

## 修复

两条漏斗金额聚合 SQL 改为 `COALESCE(SUM(total_price), 0)`。

## 验证

新增 `CrmStatisticsFunnelMapperTest` 锁定两条 SQL 的空值语义；CRM 65/65 通过。
