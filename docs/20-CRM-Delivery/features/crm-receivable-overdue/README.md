# 回款逾期检测与提醒

## 功能说明

系统按租户定时扫描已到期且未审批通过回款的回款计划，为每个计划和业务日期创建唯一提醒事实，
向计划负责人发送 CRM 站内信。提醒事实记录待发送、已发送、失败和尝试次数；失败记录按照 YAML
配置的最大重试次数处理。审批通过或计划不再逾期时，待发送事实会被安全关闭，不产生过期通知。

## 配置

服务端 `mitedtsm.crm.activity.receivable-overdue` 配置：

| 字段 | 作用 |
|---|---|
| `enabled` | 是否启用定时检测 |
| `cron` / `zone` | 扫描时间和时区 |
| `lock-key` / `lock-lease-seconds` | 多实例分布式锁及租约 |
| `batch-size` / `max-batch-size` / `max-batches` | 单次扫描的批量与安全上限 |
| `max-retries` | 单条提醒最大失败重试次数 |

默认值在 `Server/mitedtsm-server/src/main/resources/application.yaml`；运行差异由部署 KDL 显式注入
Spring 环境，不要求操作者在命令行或 Host 手工导出变量。

## 数据与边界

提醒事实位于 `crm_receivable_overdue_reminder`，唯一键为租户、回款计划和业务日期；该功能只负责
逾期识别和站内提醒，不伪造银行到账或会计核销。
