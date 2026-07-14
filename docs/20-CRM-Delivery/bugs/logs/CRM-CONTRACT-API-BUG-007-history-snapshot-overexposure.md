# CRM-CONTRACT-API-BUG-007：轨迹列表过度返回完整历史快照

发现日期：2026-07-14。状态：Fixed。

真实签署后查询 `/crm/contract-lifecycle/get` 时，初版响应直接复用了
`CrmContractChangeRecordDO`，使 `contractSnapshot`、`productSnapshot` 以及通用持久化字段全部进入
响应。结果会放大合同详情流量，并让普通轨迹视图获得不需要的完整历史数据。

修复新增专用 `Signing`、`Attachment`、`ChangeRecord` 响应摘要 VO。轨迹只返回序号、版本、动作、
操作人、原因和时间；不可变 JSON 快照继续保存在数据库，未来如需查看，必须通过独立权限接口按需加载。
禁止再让 Controller 直接返回生命周期 DO。
