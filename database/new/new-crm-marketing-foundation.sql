-- CRM marketing campaign and competitor foundation. Idempotent for existing Podman volumes.

CREATE TABLE IF NOT EXISTS `crm_marketing_campaign` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号', `code` varchar(64) NOT NULL COMMENT '活动编码',
  `name` varchar(200) NOT NULL COMMENT '活动名称', `status` tinyint NOT NULL DEFAULT 10 COMMENT '10草稿、20进行中、30锁定、40终止、50完成',
  `owner_user_id` bigint NOT NULL COMMENT '负责人', `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间', `budget_amount` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '预算',
  `actual_cost_amount` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '实际成本', `target_lead_count` int NULL COMMENT '目标线索数',
  `target_customer_count` int NULL COMMENT '目标客户数', `description` varchar(2000) NULL COMMENT '说明',
  `summary` varchar(2000) NULL COMMENT '总结', `locked_time` datetime NULL COMMENT '锁定时间',
  `terminated_time` datetime NULL COMMENT '终止时间', `completed_time` datetime NULL COMMENT '完成时间',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_crm_marketing_campaign_code` (`tenant_id`,`code`,`deleted`),
  KEY `idx_crm_marketing_campaign_owner` (`tenant_id`,`owner_user_id`,`status`,`deleted`),
  KEY `idx_crm_marketing_campaign_time` (`tenant_id`,`start_time`,`end_time`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 营销活动';

SET @birthday_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
 AND table_name='crm_contact' AND column_name='birthday');
SET @birthday_sql = IF(@birthday_column=0, 'ALTER TABLE crm_contact ADD COLUMN birthday date NULL COMMENT ''生日'' AFTER sex', 'SELECT 1');
PREPARE birthday_stmt FROM @birthday_sql; EXECUTE birthday_stmt; DEALLOCATE PREPARE birthday_stmt;

CREATE TABLE IF NOT EXISTS `crm_marketing_campaign_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号', `campaign_id` bigint NOT NULL COMMENT '活动编号',
  `biz_type` tinyint NOT NULL COMMENT '1线索、2客户、3商机、4任务', `biz_id` bigint NOT NULL COMMENT '业务编号',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_crm_marketing_campaign_relation` (`tenant_id`,`campaign_id`,`biz_type`,`biz_id`,`deleted`),
  KEY `idx_crm_marketing_relation_biz` (`tenant_id`,`biz_type`,`biz_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 营销活动关联对象';

CREATE TABLE IF NOT EXISTS `crm_competitor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号', `name` varchar(200) NOT NULL COMMENT '名称',
  `website` varchar(500) NULL COMMENT '网站', `strengths` varchar(2000) NULL COMMENT '优势',
  `weaknesses` varchar(2000) NULL COMMENT '劣势', `strategy` varchar(2000) NULL COMMENT '应对策略',
  `owner_user_id` bigint NOT NULL COMMENT '负责人', `status` tinyint NOT NULL DEFAULT 0 COMMENT '0启用、1停用',
  `remark` varchar(1000) NULL COMMENT '备注', `creator` varchar(64) NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `updater` varchar(64) NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), KEY `idx_crm_competitor_owner` (`tenant_id`,`owner_user_id`,`status`,`deleted`),
  KEY `idx_crm_competitor_name` (`tenant_id`,`name`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 竞争对手资料';

CREATE TABLE IF NOT EXISTS `crm_marketing_consent` (
  `id` bigint NOT NULL AUTO_INCREMENT, `customer_id` bigint NOT NULL, `contact_id` bigint NULL,
  `channel` tinyint NOT NULL COMMENT '1短信、2邮件', `status` tinyint NOT NULL COMMENT '1同意、2退订',
  `source` varchar(100) NOT NULL, `occurred_at` datetime NOT NULL,
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_marketing_consent_target` (`tenant_id`,`customer_id`,`contact_id`,`channel`,`deleted`),
  KEY `idx_crm_marketing_consent_lookup` (`tenant_id`,`customer_id`,`contact_id`,`channel`,`status`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 营销同意与退订';

CREATE TABLE IF NOT EXISTS `crm_marketing_broadcast` (
  `id` bigint NOT NULL AUTO_INCREMENT, `campaign_id` bigint NULL, `name` varchar(200) NOT NULL,
  `channel` tinyint NOT NULL COMMENT '1短信、2邮件、3双渠道', `sms_template_code` varchar(100) NULL,
  `mail_template_code` varchar(100) NULL, `template_params` text NULL, `status` tinyint NOT NULL DEFAULT 10,
  `total_count` int NOT NULL DEFAULT 0, `valid_count` int NOT NULL DEFAULT 0, `suppressed_count` int NOT NULL DEFAULT 0,
  `sent_count` int NOT NULL DEFAULT 0, `failed_count` int NOT NULL DEFAULT 0, `reviewer_user_id` bigint NULL,
  `reviewed_at` datetime NULL, `review_comment` varchar(1000) NULL, `scheduled_at` datetime NULL, `sent_at` datetime NULL,
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`),
  KEY `idx_crm_marketing_broadcast_status` (`tenant_id`,`status`,`scheduled_at`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 营销群发任务';

CREATE TABLE IF NOT EXISTS `crm_marketing_broadcast_recipient` (
  `id` bigint NOT NULL AUTO_INCREMENT, `broadcast_id` bigint NOT NULL, `customer_id` bigint NOT NULL, `contact_id` bigint NULL,
  `channel` tinyint NOT NULL, `mobile` varchar(32) NULL, `email` varchar(255) NULL, `status` tinyint NOT NULL DEFAULT 10,
  `suppressed_reason` varchar(500) NULL, `provider_log_id` bigint NULL, `failure_reason` varchar(1000) NULL,
  `attempt_count` int NOT NULL DEFAULT 0, `sent_at` datetime NULL, `last_attempt_at` datetime NULL,
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_marketing_broadcast_recipient` (`tenant_id`,`broadcast_id`,`customer_id`,`contact_id`,`channel`,`deleted`),
  KEY `idx_crm_marketing_recipient_status` (`tenant_id`,`broadcast_id`,`status`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 群发收件人结果';

CREATE TABLE IF NOT EXISTS `crm_customer_care_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT, `code` varchar(64) NOT NULL, `name` varchar(200) NOT NULL,
  `rule_type` tinyint NOT NULL COMMENT '1生日、2节假日、3成交后定期回访', `event_month_day` varchar(5) NULL COMMENT '节假日日期 MM-dd',
  `follow_up_days` int NULL COMMENT '成交后第 N 天触达，仅定期回访使用',
  `channel` tinyint NOT NULL, `sms_template_code` varchar(100) NULL, `mail_template_code` varchar(100) NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'0', `target_scope` varchar(50) NOT NULL DEFAULT 'READABLE_CUSTOMERS',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_customer_care_plan_code` (`tenant_id`,`code`,`deleted`), KEY `idx_crm_customer_care_plan_enabled` (`tenant_id`,`enabled`,`event_month_day`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户关怀计划';

CREATE TABLE IF NOT EXISTS `crm_customer_care_record` (
  `id` bigint NOT NULL AUTO_INCREMENT, `plan_id` bigint NOT NULL, `customer_id` bigint NOT NULL, `contact_id` bigint NULL,
  `event_date` date NOT NULL, `channel` tinyint NOT NULL, `status` tinyint NOT NULL DEFAULT 10,
  `failure_reason` varchar(1000) NULL, `provider_log_id` bigint NULL, `sent_at` datetime NULL,
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_customer_care_record` (`tenant_id`,`plan_id`,`customer_id`,`contact_id`,`event_date`,`channel`,`deleted`),
  KEY `idx_crm_customer_care_record_status` (`tenant_id`,`status`,`event_date`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户关怀触达记录';

INSERT INTO system_menu (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '营销活动','',2,80,parent.id,'marketing-campaign','ep:promotion','crm/marketing/campaign/index','CrmMarketingCampaign',0,b'1',b'1',b'1','crm-marketing',NOW(),'crm-marketing',NOW(),b'0'
FROM system_menu parent WHERE parent.path='/crm' AND parent.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.path='marketing-campaign' AND m.parent_id=parent.id AND m.deleted=b'0');

INSERT INTO system_menu (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '竞争对手','',2,81,parent.id,'competitor','ep:aim','crm/marketing/competitor/index','CrmCompetitor',0,b'1',b'1',b'1','crm-marketing',NOW(),'crm-marketing',NOW(),b'0'
FROM system_menu parent WHERE parent.path='/crm' AND parent.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.path='competitor' AND m.parent_id=parent.id AND m.deleted=b'0');

INSERT INTO system_menu (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT defs.name,defs.permission,3,defs.sort,page.id,'','','','',0,b'1',b'1',b'1','crm-marketing',NOW(),'crm-marketing',NOW(),b'0'
FROM system_menu page JOIN (SELECT '营销活动查询' name,'crm:marketing-campaign:query' permission,1 sort
 UNION ALL SELECT '营销活动维护','crm:marketing-campaign:update',2
 UNION ALL SELECT '营销活动删除','crm:marketing-campaign:delete',3) defs
WHERE page.path='marketing-campaign' AND page.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.permission=defs.permission AND existing.deleted=b'0');

INSERT INTO system_menu (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT defs.name,defs.permission,3,defs.sort,page.id,'','','','',0,b'1',b'1',b'1','crm-marketing',NOW(),'crm-marketing',NOW(),b'0'
FROM system_menu page JOIN (SELECT '竞争对手查询' name,'crm:competitor:query' permission,1 sort
 UNION ALL SELECT '竞争对手维护','crm:competitor:update',2 UNION ALL SELECT '竞争对手删除','crm:competitor:delete',3) defs
WHERE page.path='competitor' AND page.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.permission=defs.permission AND existing.deleted=b'0');

INSERT INTO system_menu (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '短信邮件群发','',2,82,parent.id,'marketing-outreach','ep:message','crm/marketing/outreach/index','CrmMarketingOutreach',0,b'1',b'1',b'1','crm-marketing',NOW(),'crm-marketing',NOW(),b'0'
FROM system_menu parent WHERE parent.path='/crm' AND parent.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.path='marketing-outreach' AND m.parent_id=parent.id AND m.deleted=b'0');

INSERT INTO system_menu (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '客户关怀','',2,83,parent.id,'customer-care','ep:present','crm/marketing/care/index','CrmCustomerCare',0,b'1',b'1',b'1','crm-marketing',NOW(),'crm-marketing',NOW(),b'0'
FROM system_menu parent WHERE parent.path='/crm' AND parent.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.path='customer-care' AND m.parent_id=parent.id AND m.deleted=b'0');

INSERT INTO system_menu (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT defs.name,defs.permission,3,defs.sort,page.id,'','','','',0,b'1',b'1',b'1','crm-marketing',NOW(),'crm-marketing',NOW(),b'0'
FROM system_menu page JOIN (SELECT '群发查询' name,'crm:marketing-outreach:query' permission,1 sort UNION ALL SELECT '群发维护','crm:marketing-outreach:update',2 UNION ALL SELECT '群发审核','crm:marketing-outreach:review',3 UNION ALL SELECT '群发发送','crm:marketing-outreach:send',4 UNION ALL SELECT '营销同意管理','crm:marketing-outreach:consent',5) defs
WHERE page.path='marketing-outreach' AND page.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.permission=defs.permission AND existing.deleted=b'0');

INSERT INTO system_menu (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT defs.name,defs.permission,3,defs.sort,page.id,'','','','',0,b'1',b'1',b'1','crm-marketing',NOW(),'crm-marketing',NOW(),b'0'
FROM system_menu page JOIN (SELECT '关怀查询' name,'crm:customer-care:query' permission,1 sort UNION ALL SELECT '关怀维护','crm:customer-care:update',2 UNION ALL SELECT '关怀删除','crm:customer-care:delete',3) defs
WHERE page.path='customer-care' AND page.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.permission=defs.permission AND existing.deleted=b'0');

INSERT INTO system_menu_i18n (`menu_id`,`language`,`name`)
SELECT menu.id,lang.language,
 CASE menu.path
  WHEN 'marketing-campaign' THEN CASE lang.language WHEN 'zh-CN' THEN '营销活动' WHEN 'en' THEN 'Marketing Campaigns' ELSE 'الحملات التسويقية' END
  WHEN 'competitor' THEN CASE lang.language WHEN 'zh-CN' THEN '竞争对手' WHEN 'en' THEN 'Competitors' ELSE 'المنافسون' END
  WHEN 'marketing-outreach' THEN CASE lang.language WHEN 'zh-CN' THEN '短信邮件群发' WHEN 'en' THEN 'Marketing Outreach' ELSE 'التواصل التسويقي' END
  ELSE CASE lang.language WHEN 'zh-CN' THEN '客户关怀' WHEN 'en' THEN 'Customer Care' ELSE 'رعاية العملاء' END END
FROM system_menu menu JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') lang
WHERE menu.path IN ('marketing-campaign','competitor','marketing-outreach','customer-care') AND menu.deleted=b'0'
ON DUPLICATE KEY UPDATE name=VALUES(name),deleted=b'0';

INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id,menu.id,'crm-marketing',NOW(),'crm-marketing',NOW(),b'0',role.tenant_id
FROM system_role role JOIN system_menu menu ON (menu.path IN ('marketing-campaign','competitor','marketing-outreach','customer-care') OR menu.permission IN
 ('crm:marketing-campaign:query','crm:marketing-campaign:update','crm:marketing-campaign:delete','crm:competitor:query','crm:competitor:update','crm:competitor:delete','crm:marketing-outreach:query','crm:marketing-outreach:update','crm:marketing-outreach:review','crm:marketing-outreach:send','crm:marketing-outreach:consent','crm:customer-care:query','crm:customer-care:update','crm:customer-care:delete'))
WHERE role.code='crm_admin' AND role.deleted=b'0' AND menu.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_role_menu existing WHERE existing.role_id=role.id AND existing.menu_id=menu.id
 AND existing.tenant_id=role.tenant_id AND existing.deleted=b'0');
