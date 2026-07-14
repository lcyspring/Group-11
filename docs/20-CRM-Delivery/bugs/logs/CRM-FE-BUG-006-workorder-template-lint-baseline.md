# CRM-FE-BUG-006：工单统计模板未满足零警告规则

- 发现/关闭日期：2026-07-14
- 级别：P2 / 可维护性
- 状态：已关闭

## 现象与根因

统计血缘专项首次把工单统计入口纳入零警告 ESLint 后，`el-date-picker` 的首个属性与标签位于
同一行，触发 `vue/first-attribute-linebreak`。原页面此前未进入独立统计专项检查，因此该告警
没有在构建前暴露。

## 修复关键

按仓库 Vue 模板规范拆分首属性，并把工单页持续纳入 `lint:crm-statistics-lineage`；不使用
禁用规则或提高 warning 阈值掩盖问题。

## 验证

Ubuntu 26.04 容器专项 ESLint 警告 0，Web production build 通过。
