# CRM-FE-BUG-002：客户统计前端类型与后端响应字段漂移

## 现象

客户统计 API 类型声明使用 `customerType/customerSource` 和小写 `followup*` 字段，
客户转化组件又把合同汇总列表声明成客户日期汇总类型。这些名称与后端响应 VO 不一致。

## 影响

- TypeScript 无法真实描述运行时响应，字段重构或组件复用时缺少有效保护；
- 合同汇总组件可能在严格类型检查中出现错误提示；
- 跟进按员工响应的字段自动补全会指向不存在的属性。

## 根因

后端 VO 演进后，前端手写接口类型没有同步；组件初始开发时复用了相邻统计类型。

## 修复关键

- `customerType/customerSource` 改为后端实际字段 `industryId/source`；
- 补齐 `ownerUserId/creator`；
- `followupRecordCount/followupCustomerCount` 改为
  `followUpRecordCount/followUpCustomerCount`；
- `CustomerConversionStat.vue` 的列表改为
  `CrmStatisticsCustomerContractSummaryRespVO[]`。

## 验证证据

- Ubuntu 26.04、Node 22.22.1、pnpm 11.3.0；
- Vite production build 成功；
- CRM 82/82 与 Maven reactor 20/20 SUCCESS，确认响应 VO 未受影响。

## 清理与遗留风险

没有产生测试数据。全量 `vue-tsc` 仍受既有 `STAT-BUG-010` 内存问题影响，因此本次
验证以生产构建和字段逐项对照为主；后续应考虑从 OpenAPI 生成类型以避免再次漂移。
