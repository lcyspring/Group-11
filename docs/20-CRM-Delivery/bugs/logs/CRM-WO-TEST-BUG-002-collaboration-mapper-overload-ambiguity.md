# CRM-WO-TEST-BUG-002：协作测试命中 MyBatis Mapper 重载歧义

- 发现/关闭日期：2026-07-16
- 级别：P2 / 测试构建
- 分支：`develop`

## 现象与根因

协作测试使用无类型 Mockito 匹配器调用 `insert` 和 `selectCount`。当前 Mapper 同时提供单实体/集合
`insert`，以及字符串字段/函数字段 `selectCount`，Java 17 无法稳定推断目标重载，测试编译失败。

## 修复

对实体匹配器显式声明 `CrmWorkOrderDO`、`CrmWorkOrderRecordDO`，对函数字段匹配器显式声明
`SFunction<CrmWorkOrderDO, ?>`。同时保留严格 Mockito 校验，不使用宽松全局规则掩盖无效桩。

## 验证

Ubuntu 26.04 Java 17 测试编译通过，工单服务专项 20/20 通过。
