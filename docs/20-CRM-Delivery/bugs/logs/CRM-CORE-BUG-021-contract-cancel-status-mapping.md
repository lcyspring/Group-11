# CRM-CORE-BUG-021：合同取消状态写入错误枚举值

- 日期：2026-07-14
- 级别：P0
- 关联：GAP-APR-002、ADR-006
- 状态：已关闭

## 现象与根因

`CrmAuditStatusUtils` 在 BPM 取消结果为 `4` 时直接返回了 `BpmTaskStatusEnum.CANCEL(4)`，
而 CRM 字典和 `CrmAuditStatusEnum.CANCEL` 的值是 `40`。取消后的合同会落入 CRM 未定义
状态，页面无正确标签，后续状态动作也无法匹配。

## 修复关键

按流程实例状态枚举识别 BPM 终态，并明确映射到 `CrmAuditStatusEnum.CANCEL(40)`，不再
复用 BPM 数值作为 CRM 业务状态。

## 验证

`CrmAuditStatusUtilsTest` 覆盖通过、驳回、取消和非终态拒绝 4/4；合同 Service 额外断言
取消回调的条件更新目标为 `40`；CRM 全量 124/124。
