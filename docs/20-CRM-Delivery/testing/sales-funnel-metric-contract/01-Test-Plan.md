# 测试计划

## 后端

- 4 个商机、1 个赢单返回 25.00%；
- 3 个商机、2 个赢单返回 66.67%；
- 空日期桶返回计数 0、转化率 0.00；
- 小时时间间隔只生成小时桶，不继续生成日桶；
- 两条金额聚合 SQL 均对 `SUM(total_price)` 使用 `COALESCE`；
- 全量 CRM 单元测试及 JaCoCo。

## 前端

- TypeScript/API 新字段编译；
- 漏斗转化率图读取 `businessWinRate`；
- 新增商机明细请求携带 `pageNo/pageSize`；
- 筛选刷新重置到第一页；
- Ubuntu 26.04 中执行生产构建。
