# CRM-CUSTOMER-MIGRATION-BUG-001：主表与历史表 CHECK 名称冲突

发现日期：2026-07-14。状态：Fixed。分支：`develop`。

为客户主表补充当前流失原因一致性约束时，最初沿用了历史表约束名
`chk_crm_customer_lifecycle_lost_reason`。MySQL 要求同一 schema 内 CHECK 名称唯一，即使约束属于
不同表也不能重名，因此真实迁移返回错误 3822；此前的状态值和成交同步约束已正常生效。

主表约束改名为 `chk_crm_customer_current_lost_reason`，名称同时表达“当前状态快照”与历史事件原因
的区别。修复后迁移需连续执行两次，并核对客户主表三项 CHECK 和历史表三项 CHECK 均存在。
