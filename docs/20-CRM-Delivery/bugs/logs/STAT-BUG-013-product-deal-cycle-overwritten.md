# STAT-BUG-013：产品成交周期被成交客户数覆盖

## 基本信息

- 发现日期：2026-07-13
- 级别：P1
- 模块：CRM / 数据统计 / 客户成交周期
- 状态：已关闭
- 影响页面：客户成交周期 / 按产品统计
- 影响接口：`GET /admin-api/crm/statistics-customer/get-customer-deal-cycle-by-product`

## 现象与影响

后端同时返回产品平均成交周期和成交客户数，但前端映射时把 `customerDealCycle` 赋值为 `customerDealCount`。图表纵轴展示的不是成交天数，而是客户数量，导致两个业务指标表面上完全相同。

## 根因

`CustomerDealCycleByProduct.vue` 对接口结果二次转换时使用了错误字段：

```text
customerDealCycle: s.customerDealCount
```

后端 SQL 和响应 VO 已有真实 `customerDealCycle`，数据在展示层被覆盖。

## 修复关键

- 新增 `normalizeProductDealCycles`，只为空产品名提供展示兜底，完整保留后端周期和数量字段。
- 组件改为调用统一转换函数，避免再次手写错误映射。
- 新增 Node 原生测试，专门断言成交周期 `12.5` 不会被客户数 `3` 覆盖。
- 将该测试接入 `pnpm test:crm-statistics`。

代码位置：

- `Web/src/views/crm/statistics/customer/components/CustomerDealCycleByProduct.vue`
- `Web/src/views/crm/statistics/customer/dealCycle.ts`
- `Web/src/views/crm/statistics/customer/dealCycle.test.mjs`
- `Web/package.json`

## 验证证据

1. `pnpm test:crm-statistics`：2/2 测试文件通过。
2. 相关 Vue/TS ESLint：通过。
3. Web 生产构建：通过，并已重建 rootless Podman Web 镜像。
4. Podman 临时数据设置为客户创建于 2026-07-01、合同下单于 2026-07-13，并关联一个产品。
5. 产品成交周期接口返回：
   - `productName=商品002`
   - `customerDealCycle=12.0`
   - `customerDealCount=1`
6. 临时客户、合同和合同产品记录已物理清理，三张表残留数均为 0。

## 遗留风险

成交周期当前使用 MySQL `TIMESTAMPDIFF(DAY, customer.create_time, contract.order_date)`，按自然日截断后再平均。是否应按小时折算、工作日或首次审批通过时间计算，需要产品口径确认并另立需求或 Bug 日志。
