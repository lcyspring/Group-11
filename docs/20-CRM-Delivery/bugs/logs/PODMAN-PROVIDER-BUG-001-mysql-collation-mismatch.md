# PODMAN-PROVIDER-BUG-001：Provider provision 稳定键比较排序规则冲突

日期：2026-07-18。分支：`develop`。级别：P1。状态：已关闭。

## 现象与根因

隔离库首次执行短信/邮件 Provider provision 时，MySQL 返回 `Illegal mix of collations`。System 的短信
渠道、短信模板、邮件账号和邮件模板字段使用 `utf8mb4_unicode_ci`，官方 MySQL 8 连接默认值及
`FROM_BASE64` 解码表达式却使用 `utf8mb4_0900_ai_ci`，稳定键等值比较因此在写入前失败。

## 修复与门禁

YAML 字符串仍通过 Base64 和 stdin 安全传输，但解码后显式转换为 `utf8mb4_unicode_ci`，与目标字段
保持一致。隔离数据库测试覆盖首次创建、重复创建、受管更新、禁用保护和模板外键关联，测试结束自动
删除隔离库，不接触当前业务数据。
