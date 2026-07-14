# CRM-CORE-BUG-015：商机结束原因未进入状态闭环

更新日期：2026-07-14

## 现象

数据库和 DO 已有 `end_remark`，但状态 Request VO、Service 更新、前端弹窗和详情
均未使用。商机标记输单或无效后无法知道原因。

## 根因

状态接口只传 `statusId/endStatus`，已有数据字段没有接入应用层和展示层；原型
`US-OPP-005` 的必填及最小 10 字规则也未实现。

## 修复关键

- Request VO 增加最大 500 字和条件最小 10 字校验；
- Service 接口增加 `@Valid`，原因 trim 后与状态原子保存；
- 赢单不保存原因；
- 前端输单/无效弹窗采集原因，详情页回显。

## 验证

请求约束 6/6、Service 行为 3/3、Ubuntu CRM 76/76、Web production build 成功。

## 状态

已关闭。
