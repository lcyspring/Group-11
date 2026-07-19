CREATE TABLE IF NOT EXISTS `bpm_oa_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `description` varchar(4000) DEFAULT NULL,
  `creator_user_id` bigint NOT NULL,
  `assignee_user_id` bigint NOT NULL,
  `participant_user_ids` varchar(2000) DEFAULT NULL COMMENT '参与人 JSON 数组',
  `priority` tinyint NOT NULL DEFAULT 1,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待开始 1进行中 2已完成',
  `due_time` datetime NOT NULL,
  `reminder_minutes` int DEFAULT NULL,
  `reminder_status` tinyint NOT NULL DEFAULT 0 COMMENT '0待发送 1发送中 2已发送',
  `reminder_sent_time` datetime DEFAULT NULL,
  `reminder_last_error` varchar(1000) DEFAULT NULL,
  `completed_time` datetime DEFAULT NULL,
  `business_type` varchar(50) DEFAULT NULL,
  `business_id` bigint DEFAULT NULL,
  `result` varchar(2000) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), KEY `idx_oa_task_owner` (`tenant_id`,`assignee_user_id`,`status`,`due_time`),
  KEY `idx_oa_task_creator` (`tenant_id`,`creator_user_id`,`status`,`due_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA任务';

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '任务工作台', 'bpm:oa-task:menu', 2, 36, parent.id, 'task', 'ep:list', 'bpm/oa/task/index', 'OaTask',
       0, b'1', b'1', b'1', 'oa-task', NOW(), 'oa-task', NOW(), b'0'
FROM system_menu parent WHERE parent.path='collaboration' AND parent.type=1 AND parent.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.permission='bpm:oa-task:menu' AND m.deleted=b'0');
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT x.name, x.permission, 3, x.sort, m.id, '', '', '', NULL, 0, b'1', b'1', b'1', 'oa-task', NOW(), 'oa-task', NOW(), b'0'
FROM (SELECT '任务查询' name,'bpm:oa-task:query' permission,1 sort UNION ALL
      SELECT '任务创建','bpm:oa-task:create',2 UNION ALL SELECT '任务更新','bpm:oa-task:update',3 UNION ALL
      SELECT '任务删除','bpm:oa-task:delete',4) x
JOIN system_menu m ON m.permission='bpm:oa-task:menu' AND m.deleted=b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu e WHERE e.permission=x.permission AND e.deleted=b'0');

INSERT INTO `system_notify_template` (`name`,`code`,`nickname`,`content`,`type`,`params`,`status`,`remark`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 'OA任务提醒','bpm-oa-task-reminder','OA','任务“{title}”将于 {dueTime} 到期，请及时处理。',1,'["title","dueTime"]',0,'OA 任务提前提醒','oa-task',NOW(),'oa-task',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code`='bpm-oa-task-reminder' AND `deleted`=b'0');
INSERT INTO `infra_job` (`name`,`status`,`handler_name`,`handler_param`,`cron_expression`,`retry_count`,`retry_interval`,`monitor_timeout`,`creator`,`create_time`,`updater`,`update_time`)
SELECT 'OA任务提醒任务',1,'bpmOATaskReminderJob','100','0 */1 * * * ?',3,1000,0,'oa-task',NOW(),'oa-task',NOW()
WHERE NOT EXISTS (SELECT 1 FROM `infra_job` WHERE `handler_name`='bpmOATaskReminderJob' AND `deleted`=b'0');
