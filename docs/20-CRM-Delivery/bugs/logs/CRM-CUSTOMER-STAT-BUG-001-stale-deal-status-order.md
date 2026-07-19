# CRM-CUSTOMER-STAT-BUG-001：四态画像残留二态排序列

发现日期：2026-07-14。状态：Fixed。分支：`develop`。

四态画像 SQL 已改为按 `lifecycle_status` 分组，但 XML 末尾残留旧的 `ORDER BY deal_status`。
真实 MySQL 8.0 开启 `ONLY_FULL_GROUP_BY` 时，接口返回 `code=500`，因为排序列不在新的分组中。
仅检查分组字段的静态测试未能发现这个残留语句。

修复为只按 `lifecycle_status` 稳定排序，并增加负向断言，明确 SQL 不得再包含旧排序列。该问题由
更新后的 8080 真实接口发现，修复后必须重新执行 CRM 全量测试、Server 镜像更新和画像接口验收。
