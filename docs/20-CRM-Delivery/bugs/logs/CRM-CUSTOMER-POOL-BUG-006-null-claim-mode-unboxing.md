# CRM-CUSTOMER-POOL-BUG-006：领取模式空值在日志分支发生拆箱异常

- 模块：CRM / 客户公海 / 领取与分配
- 状态：已修复

业务已经使用 `Boolean.TRUE.equals(isReceive)` 得到安全的 `selfClaim`，但后续主管分配日志仍用
`if (!isReceive)` 对可空 Boolean 拆箱。内部调用或兼容客户端传入 null 时会在状态写入完成前
抛出空指针并回滚。

日志分支统一使用已归一化的 `selfClaim`，null 明确按主管分配处理，不再重复拆箱原始输入。
CRM 全量回归和真实主管分配均通过。
