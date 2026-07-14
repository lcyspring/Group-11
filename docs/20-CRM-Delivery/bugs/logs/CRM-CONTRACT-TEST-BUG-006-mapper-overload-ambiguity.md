# CRM-CONTRACT-TEST-BUG-006：合同生命周期 Mapper 断言重载歧义

发现日期：2026-07-14。状态：Fixed。

MyBatis-Plus 当前版本同时暴露单实体和集合形式的 `insert`、`updateById`。Mockito 无类型
`argThat` 无法在测试编译期确定重载，导致合同生命周期测试无法编译；生产代码不受影响。

修复为三个断言分别显式声明 `CrmContractSigningDO`、`CrmContractAttachmentDO` 和
`CrmContractChangeRecordDO` 泛型。该问题与 `CRM-INV-TEST-BUG-001` 根因一致，但本日志保留
本功能包的独立发现、修复和验证证据。
