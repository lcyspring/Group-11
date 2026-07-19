# CRM-VISIT-BUG-001：拜访列表字段与 MyBatis 类型处理器不匹配

日期：2026-07-17  
分支：`develop`

## 现象

客户拜访创建接口在运行时返回“系统异常”，MySQL 报 `Invalid JSON text: The document is empty`，
失败列为 `participant_user_ids`。

## 根因

`LongListTypeHandler` 和 `StringListTypeHandler` 明确把列表编码为逗号分隔字符串，对应数据库
`VARCHAR`。初版迁移把参与人和两类附件列定义成 JSON；空列表被类型处理器写成空字符串，不是合法
JSON，导致插入失败。单元测试使用 Mapper mock，无法替代真实 MySQL 类型契约验收。

## 修复

- 三个列表列统一改为 `VARCHAR`；
- 迁移包含幂等 `ALTER TABLE ... MODIFY COLUMN`，自动修复已运行过初版脚本的持久卷；
- 保留 Java 类型处理器，避免同一字段出现两套序列化协议。

## 验证要求

- 空参与人、空附件的拜访申请可创建；
- 审批完成后结果回填可写入空结果附件；
- 详情返回的列表仍按数组序列化；
- CRM 容器门禁和真实 MySQL API 链路同时通过。
