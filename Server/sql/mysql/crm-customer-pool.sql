-- ============================================================
-- CRM 客户公海模块 SQL 脚本
-- ============================================================

-- ----------------------------
-- 表结构: crm_customer_pool_receive (公海领取记录)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `crm_customer_pool_receive` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `customer_id` bigint NOT NULL COMMENT '客户编号',
    `receive_user_id` bigint NOT NULL COMMENT '领取人编号',
    `receive_time` datetime NOT NULL COMMENT '领取时间',
    `freeze_end_time` datetime NULL COMMENT '冻结结束时间（领取后不能放入公海的截止时间）',
    `source_type` tinyint NOT NULL COMMENT '来源类型：1-手动领取 2-自动分配 3-管理员分配',
    `remark` varchar(500) NULL COMMENT '备注',
    `creator` varchar(64) NOT NULL COMMENT '创建者',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `updater` varchar(64) NULL COMMENT '更新者',
    `update_time` datetime NULL COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_receive_user_id` (`receive_user_id`),
    KEY `idx_receive_time` (`receive_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公海领取记录';

-- ----------------------------
-- 表结构: crm_customer_pool_rule (公海规则)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `crm_customer_pool_rule` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name` varchar(100) NOT NULL COMMENT '规则名称',
    `rule_type` tinyint NOT NULL COMMENT '规则类型：1-回收规则 2-领取规则 3-分配规则',
    `execute_type` tinyint NOT NULL COMMENT '执行类型：1-定时执行 2-触发执行',
    `cron_expression` varchar(100) NULL COMMENT '定时表达式（cron）',
    `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
    `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
    `remark` varchar(500) NULL COMMENT '备注',
    -- 规则配置 JSON
    `config` text NULL COMMENT '规则配置（JSON格式）',
    `creator` varchar(64) NOT NULL COMMENT '创建者',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `updater` varchar(64) NULL COMMENT '更新者',
    `update_time` datetime NULL COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_rule_type` (`rule_type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公海规则';

-- ----------------------------
-- 表结构: crm_customer_pool_receive_limit (公海领取限制)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `crm_customer_pool_receive_limit` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `user_id` bigint NOT NULL COMMENT '用户编号',
    `limit_type` tinyint NOT NULL COMMENT '限制类型：1-每日限制 2-每周限制 3-每月限制',
    `max_count` int NOT NULL DEFAULT '10' COMMENT '最大领取数量',
    `used_count` int NOT NULL DEFAULT '0' COMMENT '已使用数量',
    `period_start_time` datetime NOT NULL COMMENT '周期开始时间',
    `period_end_time` datetime NOT NULL COMMENT '周期结束时间',
    `creator` varchar(64) NOT NULL COMMENT '创建者',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `updater` varchar(64) NULL COMMENT '更新者',
    `update_time` datetime NULL COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_limit_type` (`user_id`, `limit_type`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公海领取限制';

-- ----------------------------
-- 表结构: crm_customer_pool_log (公海操作日志)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `crm_customer_pool_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `customer_id` bigint NOT NULL COMMENT '客户编号',
    `customer_name` varchar(200) NOT NULL COMMENT '客户名称',
    `operation_type` tinyint NOT NULL COMMENT '操作类型：1-放入公海 2-领取公海 3-自动回收 4-分配公海',
    `operation_user_id` bigint NULL COMMENT '操作人编号',
    `operation_user_name` varchar(100) NULL COMMENT '操作人名称',
    `before_owner_user_id` bigint NULL COMMENT '操作前负责人编号',
    `before_owner_user_name` varchar(100) NULL COMMENT '操作前负责人名称',
    `after_owner_user_id` bigint NULL COMMENT '操作后负责人编号',
    `after_owner_user_name` varchar(100) NULL COMMENT '操作后负责人名称',
    `reason` varchar(500) NULL COMMENT '操作原因',
    `rule_id` bigint NULL COMMENT '触发规则编号',
    `creator` varchar(64) NOT NULL COMMENT '创建者',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公海操作日志';

-- ----------------------------
-- 修改 crm_customer 表，增加公海相关字段
-- ----------------------------
ALTER TABLE `crm_customer` 
ADD COLUMN `pool_status` tinyint NOT NULL DEFAULT '0' COMMENT '公海状态：0-非公海 1-公海',
ADD COLUMN `pool_time` datetime NULL COMMENT '进入公海时间',
ADD COLUMN `last_receive_time` datetime NULL COMMENT '最后领取时间',
ADD COLUMN `receive_freeze_end_time` datetime NULL COMMENT '领取冻结截止时间',
ADD COLUMN `pool_reason` varchar(500) NULL COMMENT '进入公海原因',
ADD COLUMN `pool_rule_id` bigint NULL COMMENT '触发规则编号';

-- ----------------------------
-- 初始化公海规则数据
-- ----------------------------
INSERT INTO `crm_customer_pool_rule` (`name`, `rule_type`, `execute_type`, `cron_expression`, `enabled`, `sort`, `config`) VALUES
('默认回收规则-未跟进', 1, 1, '0 0 2 * * ?', 1, 1, '{"contactExpireDays":30,"dealExpireDays":90,"notifyEnabled":true,"notifyDays":3}'),
('默认领取规则', 2, 2, NULL, 1, 1, '{"dailyLimit":10,"weeklyLimit":30,"monthlyLimit":100,"freezeDays":3}'),
('默认分配规则', 3, 1, '0 0 9 * * ?', 1, 1, '{"autoDistribute":false,"maxPerUser":5}');

-- ----------------------------
-- 更新现有客户的公海状态
-- ----------------------------
UPDATE `crm_customer` SET `pool_status` = CASE WHEN `owner_user_id` IS NULL THEN 1 ELSE 0 END;