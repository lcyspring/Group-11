# BPM-TENANT-BUG-002：流程定义和模型按 ID 读取可绕过租户查询

日期：2026-07-14

分支：`develop`

状态：已修复

## 现象与影响

Flowable 的 `getProcessDefinition`、`getModel` 和部分 Deployment 查询按全局 ID 直接读取，未
显式限定当前租户。知道其他租户 ID 时可能读取流程元数据或将后续操作指向错误对象。

## 修复与验证

- 流程定义、Deployment 和模型改为 Query API，并附带当前租户；
- 批量查询、BPMN 模型读取、状态更新均复用租户安全查询；
- 新增流程定义、Deployment、模型正向租户条件测试，全部通过。
