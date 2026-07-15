# CRM-FIN-BUG-010：报销更新触发 MyBatis 乐观锁参数异常

- 发现日期：2026-07-15
- 分支：`develop`
- 状态：Closed

## 现象

报销草稿创建成功，但真实更新接口返回 `500 系统异常`。日志显示：

`Parameter 'MP_OPTLOCK_VERSION_ORIGINAL' not found. Available parameters are [param1, et]`

事务回滚，所以附件和明细仍保持更新前状态。单元测试只 Mock 了 Mapper，未执行 MyBatis 生成 SQL，未能提前发现。

## 根因

`CrmReimbursementDO.version` 使用 MyBatis Plus `@Version`，当前项目的 Mapper 扩展同时提供单对象和集合重载，乐观锁插件生成了版本参数，但实际 Mapper 参数映射没有对应键。编译和纯 Mockito 测试均无法覆盖该框架组合问题。

## 修复

移除隐式 `@Version` 插件路径，在 Mapper 中显式实现：

- 内容更新：`id + expectedVersion` 条件更新并原子递增版本；
- 提交审批：`id + expectedVersion + DRAFT` 条件更新并原子递增版本；
- 影响行数为 0 时返回业务并发错误，不暴露框架异常。

## 回归要求

重新执行 CRM 全量测试，并在 Podman MySQL 上真实更新带受保护附件的草稿，确认接口 `code=0`、版本递增、金额与附件均正确保存后关闭。

## 回归结果

- CRM：264/264；
- 真实更新：`code=0`；
- 版本：0 原子递增为 1；
- 总额：`124.000000`；
- 明细：2 条；
- 受保护附件：1 个，详情可见。
