# CRM-REC-BUG-001：持久卷缺少合同来源字段导致回款待办异常

## 现象

- 管理端“待办事项 / 待审核回款”提示“系统异常”；
- “回款提醒”也会失败；
- Server 日志记录 `Unknown column 'source_business_id' in 'field list'`。

## 根因

商机转合同功能已在 `CrmContractDO` 中增加 `sourceBusinessId`，新建数据库会由
`new-crm-business-contract-conversion.sql` 创建字段和索引；但已有 Podman MySQL
持久卷不会再次触发 `/docker-entrypoint-initdb.d`，因此运行库结构落后于 Java 模型。

## 修复

1. 对当前运行库执行既有幂等迁移；
2. `up.sh` 在启动 Server 前执行 YAML 指定的
   `mysql.compatibility_migration_file`；
3. 配置指向商机转合同的幂等迁移文件，重复执行不会重复创建字段或索引。

## 验证

- `source_business_id`、`active_business_conversion_key` 均存在；
- 唯一索引与查询索引均存在；
- `/crm/receivable/page?...auditStatus=10` 返回 `code=0`、1 条数据；
- `/crm/receivable-plan/page?...remindType=1` 返回 `code=0`、3 条数据；
- `up.sh` check 模式通过。

## 影响范围

受影响的不只是待审核回款：所有读取合同完整字段的回款、回款计划、合同相关页面
都可能触发相同异常。该问题属于运行库升级遗漏，不是 Flowable 审批状态错误。
