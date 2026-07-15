# CRM-IAM-BUG-003：只读协作者可导出 CRM 对象

- 发现/关闭日期：2026-07-15
- 级别：P0 / 数据安全
- 关联：GAP-IAM-003、GAP-SEC-003

## 现象与根因

八类 CRM 列表按 READ 权限允许查看，但导出接口直接复用列表结果写 Excel。只读协作者因而能批量取得完整
离线副本，页面查询权限与高风险导出动作没有分层。

## 修复关键

- 新增统一 `validateExportPermission`，按对象批量检查 OWNER/WRITE；
- CRM 管理员保留导出能力，下属 OWNER/WRITE 纳入管理范围；
- READ-only、无权限或混合未授权 ID 整批返回 `1020007010`；
- 八个导出 Controller 在生成响应前统一调用门禁。

## 验证

管理员、WRITE、下属 OWNER 和 READ-only/无范围负向均由 Service 测试覆盖；CRM 全量 231/231 通过。
