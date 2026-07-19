-- CRM task, call and SMS activity truth sources plus traceable clue conversion migration.
-- Idempotent for MySQL 8.0.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `biz_type` int NOT NULL COMMENT '关联 CRM 对象类型：1线索、2客户',
  `biz_id` bigint NOT NULL COMMENT '关联 CRM 对象编号',
  `source_clue_id` bigint NULL COMMENT '由线索转换迁移时的原线索编号',
  `type` tinyint NOT NULL COMMENT '任务类型：1普通、2跟进',
  `title` varchar(256) NOT NULL COMMENT '任务标题',
  `description` text NULL COMMENT '任务描述',
  `priority` tinyint NOT NULL DEFAULT 2 COMMENT '优先级：1低、2中、3高',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0未开始、10进行中、20完成、30未完成、40取消、50超时',
  `assignee_user_id` bigint NOT NULL COMMENT '负责人',
  `due_time` datetime NOT NULL COMMENT '截止时间',
  `remind_time` datetime NULL COMMENT '提醒时间',
  `notify_system` bit(1) NOT NULL DEFAULT b'1' COMMENT '站内提醒',
  `notify_email` bit(1) NOT NULL DEFAULT b'0' COMMENT '邮件提醒',
  `notify_sms` bit(1) NOT NULL DEFAULT b'0' COMMENT '短信提醒',
  `start_time` datetime NULL COMMENT '开始时间',
  `finish_time` datetime NULL COMMENT '结束时间',
  `result` varchar(1000) NULL COMMENT '完成、未完成或取消说明',
  `version` int NOT NULL DEFAULT 0 COMMENT '并发版本',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_task_biz` (`tenant_id`,`biz_type`,`biz_id`,`status`,`due_time`,`id`),
  KEY `idx_crm_task_assignee` (`tenant_id`,`assignee_user_id`,`status`,`due_time`,`id`),
  KEY `idx_crm_task_source_clue` (`tenant_id`,`source_clue_id`,`id`),
  CONSTRAINT `chk_crm_task_biz_type` CHECK (`biz_type` IN (1,2)),
  CONSTRAINT `chk_crm_task_type` CHECK (`type` IN (1,2)),
  CONSTRAINT `chk_crm_task_priority` CHECK (`priority` IN (1,2,3)),
  CONSTRAINT `chk_crm_task_status` CHECK (`status` IN (0,10,20,30,40,50))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 业务任务';

CREATE TABLE IF NOT EXISTS `crm_task_action_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `task_id` bigint NOT NULL COMMENT '任务编号',
  `action_type` tinyint NOT NULL COMMENT '动作：1创建、2修改、3开始、4完成、5未完成、6取消、7超时、8迁移',
  `from_status` tinyint NULL COMMENT '原状态',
  `to_status` tinyint NOT NULL COMMENT '目标状态',
  `operator_user_id` bigint NULL COMMENT '操作人；系统任务为空',
  `remark` varchar(1000) NULL COMMENT '说明',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_task_action_task` (`tenant_id`,`task_id`,`create_time`,`id`),
  CONSTRAINT `chk_crm_task_action_type` CHECK (`action_type` IN (1,2,3,4,5,6,7,8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 任务不可变动作轨迹';

CREATE TABLE IF NOT EXISTS `crm_call_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `biz_type` int NOT NULL COMMENT '关联 CRM 对象类型：1线索、2客户',
  `biz_id` bigint NOT NULL COMMENT '关联 CRM 对象编号',
  `source_clue_id` bigint NULL COMMENT '由线索转换迁移时的原线索编号',
  `contact_id` bigint NULL COMMENT '联系人编号',
  `direction` tinyint NOT NULL COMMENT '方向：1呼出、2呼入',
  `status` tinyint NOT NULL COMMENT '状态：10接通、20未接、30失败',
  `phone` varchar(32) NOT NULL COMMENT '电话号码快照',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NULL COMMENT '结束时间',
  `duration_seconds` int NOT NULL DEFAULT 0 COMMENT '通话秒数',
  `recording_url` varchar(1024) NULL COMMENT '受保护录音地址',
  `summary` varchar(2000) NULL COMMENT '通话摘要',
  `operator_user_id` bigint NOT NULL COMMENT '操作人员',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_call_biz` (`tenant_id`,`biz_type`,`biz_id`,`start_time`,`id`),
  KEY `idx_crm_call_contact` (`tenant_id`,`contact_id`,`start_time`,`id`),
  KEY `idx_crm_call_source_clue` (`tenant_id`,`source_clue_id`,`id`),
  CONSTRAINT `chk_crm_call_biz_type` CHECK (`biz_type` IN (1,2)),
  CONSTRAINT `chk_crm_call_direction` CHECK (`direction` IN (1,2)),
  CONSTRAINT `chk_crm_call_status` CHECK (`status` IN (10,20,30)),
  CONSTRAINT `chk_crm_call_duration` CHECK (`duration_seconds` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 通话记录';

CREATE TABLE IF NOT EXISTS `crm_sms_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `biz_type` int NOT NULL COMMENT '关联 CRM 对象类型：1线索、2客户',
  `biz_id` bigint NOT NULL COMMENT '关联 CRM 对象编号',
  `source_clue_id` bigint NULL COMMENT '由线索转换迁移时的原线索编号',
  `contact_id` bigint NULL COMMENT '联系人编号',
  `direction` tinyint NOT NULL COMMENT '方向：1发送、2接收',
  `status` tinyint NOT NULL COMMENT '状态：0待发送、10已发送、20已送达、30失败、40已接收',
  `mobile` varchar(32) NOT NULL COMMENT '手机号码快照',
  `content` varchar(2000) NOT NULL COMMENT '短信内容快照',
  `system_sms_log_id` bigint NULL COMMENT '系统短信发送日志编号',
  `external_message_id` varchar(128) NULL COMMENT '渠道消息编号',
  `failure_reason` varchar(1000) NULL COMMENT '失败原因',
  `occurred_time` datetime NOT NULL COMMENT '发送或接收时间',
  `operator_user_id` bigint NOT NULL COMMENT '操作人员',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_sms_system_log` (`tenant_id`,`system_sms_log_id`),
  KEY `idx_crm_sms_biz` (`tenant_id`,`biz_type`,`biz_id`,`occurred_time`,`id`),
  KEY `idx_crm_sms_contact` (`tenant_id`,`contact_id`,`occurred_time`,`id`),
  KEY `idx_crm_sms_source_clue` (`tenant_id`,`source_clue_id`,`id`),
  CONSTRAINT `chk_crm_sms_biz_type` CHECK (`biz_type` IN (1,2)),
  CONSTRAINT `chk_crm_sms_direction` CHECK (`direction` IN (1,2)),
  CONSTRAINT `chk_crm_sms_status` CHECK (`status` IN (0,10,20,30,40))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 短信沟通记录';

CREATE TABLE IF NOT EXISTS `crm_clue_conversion_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `clue_id` bigint NOT NULL COMMENT '原线索编号',
  `customer_id` bigint NOT NULL COMMENT '目标客户编号',
  `primary_contact_id` bigint NULL COMMENT '转换创建的首联系人编号',
  `follow_up_count` int NOT NULL DEFAULT 0 COMMENT '迁移跟进数',
  `task_count` int NOT NULL DEFAULT 0 COMMENT '迁移任务数',
  `call_count` int NOT NULL DEFAULT 0 COMMENT '迁移通话数',
  `sms_count` int NOT NULL DEFAULT 0 COMMENT '迁移短信数',
  `operator_user_id` bigint NULL COMMENT '转换操作人',
  `converted_at` datetime NOT NULL COMMENT '转换时间',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_clue_conversion_clue` (`tenant_id`,`clue_id`),
  UNIQUE KEY `uk_crm_clue_conversion_customer` (`tenant_id`,`customer_id`),
  CONSTRAINT `chk_crm_clue_conversion_counts` CHECK (`follow_up_count` >= 0 AND `task_count` >= 0 AND `call_count` >= 0 AND `sms_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 线索转客户迁移审计';

-- Backfill the source/target link for historical successful conversions. Historical activity tables did not exist,
-- so only the existing follow-up source count can be reconstructed without inventing data.
INSERT INTO `crm_clue_conversion_record`
  (`clue_id`,`customer_id`,`primary_contact_id`,`follow_up_count`,`task_count`,`call_count`,`sms_count`,
   `operator_user_id`,`converted_at`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT clue.id, clue.customer_id,
       (SELECT contact.id FROM crm_contact contact
        WHERE contact.tenant_id=clue.tenant_id AND contact.customer_id=clue.customer_id
          AND contact.primary_contact=b'1' AND contact.deleted=b'0'
        ORDER BY contact.id LIMIT 1),
       (SELECT COUNT(*) FROM crm_follow_up_record follow_up
        WHERE follow_up.tenant_id=clue.tenant_id AND follow_up.biz_type=1
          AND follow_up.biz_id=clue.id AND follow_up.deleted=b'0'),
       0, 0, 0, NULL, COALESCE(clue.update_time, clue.create_time),
       'clue-activity-migration', NOW(), 'clue-activity-migration', NOW(), b'0', clue.tenant_id
FROM crm_clue clue
WHERE clue.transform_status=b'1' AND clue.customer_id IS NOT NULL AND clue.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM crm_clue_conversion_record existing
                  WHERE existing.tenant_id=clue.tenant_id AND existing.clue_id=clue.id);

-- Activity capability is embedded in clue/customer details, so only action permissions are added.
INSERT INTO `system_menu`
  (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,
   `keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT permission_name, permission_code, 3, permission_sort, crm.id, '', '', '', '', 0, b'1', b'1', b'1',
       'clue-activity-migration', NOW(), 'clue-activity-migration', NOW(), b'0'
FROM system_menu crm
JOIN (
  SELECT 'CRM活动查询' permission_name, 'crm:activity:query' permission_code, 41 permission_sort
  UNION ALL SELECT 'CRM活动创建', 'crm:activity:create', 42
  UNION ALL SELECT 'CRM任务处理', 'crm:activity:update', 43
) permission_defs
WHERE crm.path='/crm' AND crm.parent_id=0 AND crm.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu existing
                  WHERE existing.permission=permission_defs.permission_code AND existing.deleted=b'0');

INSERT INTO `system_role_menu`
  (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT DISTINCT source.role_id, target.id, 'clue-activity-migration', NOW(),
       'clue-activity-migration', NOW(), b'0', source.tenant_id
FROM system_role_menu source
JOIN system_menu source_menu ON source_menu.id=source.menu_id
  AND source_menu.permission IN ('crm:clue:query','crm:customer:query') AND source_menu.deleted=b'0'
JOIN system_menu target ON target.permission='crm:activity:query' AND target.deleted=b'0'
WHERE source.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
                  WHERE existing.role_id=source.role_id AND existing.menu_id=target.id
                    AND existing.tenant_id=source.tenant_id AND existing.deleted=b'0');

INSERT INTO `system_role_menu`
  (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT DISTINCT source.role_id, target.id, 'clue-activity-migration', NOW(),
       'clue-activity-migration', NOW(), b'0', source.tenant_id
FROM system_role_menu source
JOIN system_menu source_menu ON source_menu.id=source.menu_id
  AND source_menu.permission IN ('crm:clue:update','crm:customer:update') AND source_menu.deleted=b'0'
JOIN system_menu target ON target.permission IN ('crm:activity:create','crm:activity:update') AND target.deleted=b'0'
WHERE source.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
                  WHERE existing.role_id=source.role_id AND existing.menu_id=target.id
                    AND existing.tenant_id=source.tenant_id AND existing.deleted=b'0');

INSERT INTO `system_notify_template`
  (`name`,`code`,`nickname`,`content`,`type`,`params`,`status`,`remark`,
   `creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 'CRM任务指派', 'crm-task-assigned', 'CRM',
       '您收到 CRM 任务 {title}，截止时间：{dueTime}。', 1, '["title","dueTime"]', 0,
       'CRM 任务状态机', 'clue-activity-migration', NOW(), 'clue-activity-migration', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_notify_template
                  WHERE code='crm-task-assigned' AND deleted=b'0');

INSERT INTO `system_notify_template`
  (`name`,`code`,`nickname`,`content`,`type`,`params`,`status`,`remark`,
   `creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 'CRM任务处理完成', 'crm-task-finished', 'CRM',
       'CRM 任务 {title} 已处理，结果：{result}。', 1, '["title","result"]', 0,
       'CRM 任务状态机', 'clue-activity-migration', NOW(), 'clue-activity-migration', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_notify_template
                  WHERE code='crm-task-finished' AND deleted=b'0');
