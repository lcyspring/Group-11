# CRM-CORE-BUG-018：商机结束时可能清空最后销售阶段

更新日期：2026-07-14

## 现象

条件更新初版直接把请求中的空 `statusId` 写入数据库。输单、赢单和无效请求只
携带 `endStatus`，因此会把商机结束前的最后阶段清空。

## 根因

旧 `updateById` 的非空字段策略会忽略空 `statusId`；改为显式 Wrapper `set` 后，
空值语义发生变化但没有同步保留原阶段。

## 修复关键

结束请求将已读取并参与 compare-and-set 的旧 `statusId` 作为新阶段值，只更新
`endStatus/endRemark`；活动阶段请求仍写入新 `statusId`。

## 验证

输单和赢单 Service 用例均断言 Mapper 收到原阶段 ID；Ubuntu CRM 76/76。

## 状态

已关闭，回归在提交前拦截。
