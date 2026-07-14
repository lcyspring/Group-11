# STAT-BUG-024：未回款金额列重复显示回款率

更新日期：2026-07-14

## 现象

客户总量分析的“未回款金额”和“回款完成率”两列都调用
`erpCalculatePercentage(receivablePrice, contractPrice)`，显示完全相同的百分比。

## 根因

前端表格没有独立的未回款金额字段，复制回款率模板后未改变公式和格式。

## 修复关键

- 后端响应增加 `unreceivablePrice = contractPrice - receivablePrice`；
- 回款率由后端统一计算并处理零分母；
- 前端未回款列使用金额格式，回款率列消费百分比字段。

## 验证

合同 100、回款 40 返回未回款 60、回款率 40.00；CRM 80/80，Ubuntu Web 构建成功。

## 状态

已关闭。期间金额差与合同级真实应收余额的边界仍需保留。
