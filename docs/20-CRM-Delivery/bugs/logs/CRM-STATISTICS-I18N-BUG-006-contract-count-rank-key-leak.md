# CRM-STATISTICS-I18N-BUG-006 签约合同排行数量键泄漏

## 现象

签约合同排行表格列和图表横轴显示 `crm.statistics.rank.contractCount2`。

## 根因

`contractCount2` 只定义在业绩分析 `performance` 作用域，排行组件读取的是 `rank` 作用域。

## 修复

在中、英、阿三份 `statistics.rank` 中补齐合同数量文案，并增加作用域专项测试。
