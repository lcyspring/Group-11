# CRM-WO-TEST-BUG-001：Mockito 轨迹断言命中 Mapper insert 重载歧义

- 发现/关闭日期：2026-07-14
- 级别：P2 / 测试代码

## 现象与根因

新增分派轨迹测试使用无类型 `argThat` 验证 `recordMapper.insert(...)`。当前 MyBatis Mapper
同时暴露单实体与集合 `insert`，Java 编译器无法仅凭 Lambda 推断目标重载，Ubuntu 26.04
测试编译失败；生产代码编译已通过。

## 修复与验证

将匹配器显式限定为 `CrmWorkOrderRecordDO`，消除重载歧义。修复后 Ubuntu 26.04 CRM
全量 170/170 通过。
