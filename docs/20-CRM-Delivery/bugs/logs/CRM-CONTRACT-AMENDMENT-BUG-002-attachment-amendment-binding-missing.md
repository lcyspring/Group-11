# CRM-CONTRACT-AMENDMENT-BUG-002：生命周期附件响应缺少补充协议归属

发现日期：2026-07-16。状态：Fixed。

## 现象

补充协议审批和附件锁定在数据库中均已成功，但真实验收无法从生命周期 API 判断附件属于哪个
补充协议，因而把已完成业务误判为失败。

## 根因

响应 VO 的字段位置错误：`amendmentId` 被放入 `Signing`，实际的 `Attachment` 对象没有该字段。
持久化模型和查询结果虽然正确，序列化契约却丢失了附件归属。

## 修复关键

从 `CrmContractLifecycleRespVO.Signing` 移除 `amendmentId`，加入 `Attachment`，保持数据库模型、
Mapper、Service 和前端 API 类型一致。

## 验证

- Controller 回归测试断言附件包含 `amendmentId` 且签署对象不包含该字段；
- CRM 全量 426/426；
- 第二轮真实验收识别依据附件 ID 6，并确认审批生效后 `immutable=true`；
- 跨合同和跨租户负向读取保持拒绝。

未修改历史附件数据，仅修正响应契约。
