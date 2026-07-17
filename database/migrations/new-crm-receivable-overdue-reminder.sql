CREATE TABLE IF NOT EXISTS `crm_receivable_overdue_reminder` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '提醒事实编号',
  `receivable_plan_id` bigint NOT NULL COMMENT '回款计划编号',
  `recipient_user_id` bigint NOT NULL COMMENT '接收管理员编号',
  `reminder_date` date NOT NULL COMMENT '提醒业务日期',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待发送 1已发送 2失败',
  `attempts` int NOT NULL DEFAULT 0 COMMENT '发送尝试次数',
  `sent_time` datetime DEFAULT NULL COMMENT '发送成功时间',
  `last_error` varchar(1000) DEFAULT NULL COMMENT '最近失败原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_reminder_date_tenant` (`receivable_plan_id`,`reminder_date`,`tenant_id`),
  KEY `idx_retry` (`tenant_id`,`status`,`attempts`,`id`),
  KEY `idx_recipient_date` (`tenant_id`,`recipient_user_id`,`reminder_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 回款逾期提醒事实表';

INSERT INTO `system_notify_template`
 (`name`,`code`,`nickname`,`content`,`type`,`params`,`status`,`remark`,
  `creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 'CRM回款计划逾期提醒','crm-receivable-overdue','CRM',
       '回款计划 #{planId}（第 {period} 期）已逾期，计划日期：{returnTime}，计划金额：{price} 元，请及时跟进。',
       1,'["planId","period","returnTime","price"]',0,'CRM 回款逾期检测与提醒',
       'crm-receivable-overdue',NOW(),'crm-receivable-overdue',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template`
                  WHERE `code`='crm-receivable-overdue' AND `deleted`=b'0');
