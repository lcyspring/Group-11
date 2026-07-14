# CRM-FE-BUG-001：新增商机分析分页控件不改变请求页码

## 现象

`BusinessSummary.vue` 维护了本地 `pageNo/pageSize` 并绑定分页组件，但调用
`getBusinessPageByDate` 时只传父级筛选参数。

## 影响

用户切换分页后仍请求默认页；分页控件发生变化但表格内容不随页码变化。

## 修复

请求参数合并本地 `pageNo/pageSize`；筛选条件刷新时将页码重置为 1。

## 验证

管理端 Web 已在 Ubuntu 26.04、Node 22.22.1、pnpm 11.3.0 下完成生产构建。
