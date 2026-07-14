-- ===================================================================
-- CRM 营销域 初始化 SQL
-- 包含：营销活动、营销短信模板、营销邮件模板
-- ===================================================================

-- ========== 营销活动表 ==========
CREATE TABLE IF NOT EXISTS `crm_marketing_campaign` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name` varchar(128) NOT NULL COMMENT '活动名称',
    `type` int NOT NULL DEFAULT 1 COMMENT '活动类型：1=短信营销，2=邮件营销',
    `status` int NOT NULL DEFAULT 0 COMMENT '活动状态：0=草稿，1=进行中，2=已结束，3=已取消',
    `start_time` datetime DEFAULT NULL COMMENT '活动开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '活动结束时间',
    `target_customer_ids` json DEFAULT NULL COMMENT '目标客户编号列表',
    `description` varchar(512) DEFAULT NULL COMMENT '活动描述',
    `budget` decimal(12,2) DEFAULT NULL COMMENT '活动预算，单位：元',
    `actual_cost` decimal(12,2) DEFAULT NULL COMMENT '实际花费，单位：元',
    `template_id` bigint DEFAULT NULL COMMENT '关联的模板编号（短信或邮件模板）',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 营销活动';

-- ========== 营销短信模板表 ==========
CREATE TABLE IF NOT EXISTS `crm_marketing_sms_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name` varchar(64) NOT NULL COMMENT '模板名称',
    `code` varchar(64) NOT NULL COMMENT '模板编码，保证唯一',
    `content` varchar(512) NOT NULL COMMENT '模板内容，参数使用 {} 包裹',
    `params` json DEFAULT NULL COMMENT '参数数组（自动根据内容生成）',
    `status` int NOT NULL DEFAULT 0 COMMENT '启用状态：0=启用，1=禁用',
    `campaign_id` bigint DEFAULT NULL COMMENT '关联的营销活动编号',
    `channel_id` bigint DEFAULT NULL COMMENT '短信渠道编号，关联 system_sms_channel',
    `channel_code` varchar(32) DEFAULT NULL COMMENT '短信渠道编码，冗余字段',
    `api_template_id` varchar(64) DEFAULT NULL COMMENT '短信 API 的模板编号',
    `remark` varchar(256) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_code` (`code`),
    KEY `idx_campaign_id` (`campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 营销短信模板';

-- ========== 营销邮件模板表 ==========
CREATE TABLE IF NOT EXISTS `crm_marketing_mail_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name` varchar(64) NOT NULL COMMENT '模板名称',
    `code` varchar(64) NOT NULL COMMENT '模板编码，保证唯一',
    `title` varchar(256) NOT NULL COMMENT '邮件标题，参数使用 {} 包裹',
    `content` text NOT NULL COMMENT '邮件内容，参数使用 {} 包裹',
    `params` json DEFAULT NULL COMMENT '参数数组（自动根据内容生成）',
    `status` int NOT NULL DEFAULT 0 COMMENT '启用状态：0=启用，1=禁用',
    `campaign_id` bigint DEFAULT NULL COMMENT '关联的营销活动编号',
    `account_id` bigint DEFAULT NULL COMMENT '发送邮箱账号编号，关联 system_mail_account',
    `nickname` varchar(64) DEFAULT NULL COMMENT '发送人名称',
    `remark` varchar(256) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_code` (`code`),
    KEY `idx_campaign_id` (`campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 营销邮件模板';
