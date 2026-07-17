-- OA 日程站内提醒：抢占状态保证并发任务不重复发送。
ALTER TABLE `bpm_oa_event`
  ADD COLUMN `reminder_status` tinyint NOT NULL DEFAULT 0 COMMENT '提醒状态：0待发送 1发送中 2已发送' AFTER `status`,
  ADD COLUMN `reminder_sent_time` datetime DEFAULT NULL COMMENT '提醒发送时间' AFTER `reminder_status`,
  ADD COLUMN `reminder_last_error` varchar(1000) DEFAULT NULL COMMENT '提醒最近错误' AFTER `reminder_sent_time`,
  ADD KEY `idx_reminder_due` (`tenant_id`,`status`,`reminder_status`,`start_time`);

INSERT INTO `system_notify_template`
 (`name`,`code`,`nickname`,`content`,`type`,`params`,`status`,`remark`,
  `creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 'OA日程提醒','bpm-oa-event-reminder','OA',
       '日程“{title}”将于 {startTime} 开始，地点：{location}。',
       1,'["title","startTime","location"]',0,'OA 日程提前提醒',
       'oa-event-reminder',NOW(),'oa-event-reminder',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template`
                  WHERE `code`='bpm-oa-event-reminder' AND `deleted`=b'0');

INSERT INTO `infra_job`
 (`name`,`status`,`handler_name`,`handler_param`,`cron_expression`,`retry_count`,`retry_interval`,
  `monitor_timeout`,`creator`,`create_time`,`updater`,`update_time`)
SELECT 'OA日程提醒任务',1,'bpmOAEventReminderJob','100','0 */1 * * * ?',3,1000,0,
       'oa-event-reminder',NOW(),'oa-event-reminder',NOW()
WHERE NOT EXISTS (SELECT 1 FROM `infra_job` WHERE `handler_name`='bpmOAEventReminderJob' AND `deleted`=b'0');
