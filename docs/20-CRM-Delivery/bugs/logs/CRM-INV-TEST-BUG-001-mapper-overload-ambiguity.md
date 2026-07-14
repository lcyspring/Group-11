# CRM-INV-TEST-BUG-001：无类型 Mockito matcher 命中 Mapper 单条/批量重载歧义

- 发现/关闭日期：2026-07-14
- 级别：P2 / 测试基础设施

## 现象

发票测试使用无类型 `argThat` 验证 `insert`、`updateById`。当前 MyBatis-Plus 同时提供实体与集合
重载，Ubuntu 26.04 的 javac 无法推导 Lambda 目标类型，测试编译失败；生产代码编译正常。

## 修复

所有相关 matcher 显式限定为 `CrmInvoiceDO` 或 `CrmInvoiceActionRecordDO`。修复后测试进入真实
业务执行阶段并最终全量通过。后续 Mapper 断言禁止使用无法确定目标实体类型的 matcher。
