-- CRM 营销链接与逐收件人点击事实。可重复执行。
CREATE TABLE IF NOT EXISTS `crm_marketing_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '链接编号',
  `broadcast_id` bigint NOT NULL COMMENT '群发任务编号',
  `code` varchar(32) NOT NULL COMMENT '模板参数编码',
  `name` varchar(100) NOT NULL COMMENT '链接名称',
  `target_url` varchar(2000) NOT NULL COMMENT '服务端固化的目标 URL',
  `creator` varchar(64) NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_marketing_link_code` (`tenant_id`,`broadcast_id`,`code`,`deleted`),
  KEY `idx_crm_marketing_link_broadcast` (`tenant_id`,`broadcast_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 营销跟踪链接';

CREATE TABLE IF NOT EXISTS `crm_marketing_link_recipient` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点击事实编号',
  `link_id` bigint NOT NULL COMMENT '链接编号',
  `recipient_id` bigint NOT NULL COMMENT '群发收件人编号',
  `tracking_token` char(48) NOT NULL COMMENT '不可猜测的匿名跳转令牌',
  `first_clicked_at` datetime NULL COMMENT '首次点击时间',
  `last_clicked_at` datetime NULL COMMENT '最近点击时间',
  `click_count` int NOT NULL DEFAULT 0 COMMENT '累计点击次数',
  `creator` varchar(64) NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_marketing_click_token` (`tracking_token`),
  UNIQUE KEY `uk_crm_marketing_link_recipient` (`tenant_id`,`link_id`,`recipient_id`,`deleted`),
  KEY `idx_crm_marketing_click_summary` (`tenant_id`,`link_id`,`first_clicked_at`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 营销链接逐收件人点击事实';
