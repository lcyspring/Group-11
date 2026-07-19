# OA 日程提醒

日程支持按 `reminderMinutes` 提前提醒负责人。Infra Job `bpmOAEventReminderJob`
每分钟扫描到期日程，使用 `reminder_status` 条件更新抢占任务：同一租户的并发执行最多只有一个发送者。

发送成功记录 `reminder_sent_time`；发送失败记录最近错误并释放状态，下一轮可重试。取消日程和已经开始的日程不会发送提醒。

数据库迁移为 `database/migrations/new-oa-event-reminder.sql`，已加入 MySQL bootstrap 与 compatibility manifest。
