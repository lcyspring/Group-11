# CRM-WORKORDER-UI-BUG-008：工单统计有数据但趋势图不更新

## 现象

工单统计汇总卡片、状态表、类型表均有数据，趋势接口也返回创建和完结序列，但折线图保持空白或旧状态。

## 根因

页面使用 `shallowRef` 保存 ECharts options，接口返回后只原地修改 `xAxis.data` 和两组 `series.data`。
嵌套对象不是深层响应式对象，传给 `Echart` 子组件的 options 引用也没有变化，子组件无法观察这次更新。

## 修复

提取 `buildWorkOrderTrendOptions` 纯函数；每次加载完成后从返回行重新构造完整 options，并替换
`trendOptions.value`。空数据和有数据都走相同构造路径，语言标签也随当前语言重新生成。

## 验证

- 单元测试验证新旧 options 引用不同，横轴及创建/完结序列完整；
- 专项覆盖率：行 94.19%、分支 100%、函数 100%；
- ESLint 通过，Ubuntu 26.04 生产构建和语言包校验通过；
- 新 Web 镜像已替换，HTTP 200。
