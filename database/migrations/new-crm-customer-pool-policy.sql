-- CRM customer public-pool state, governed rule overrides and immutable event sources.
-- Idempotent for MySQL 8.0.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `crm_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `crm_add_column_if_missing`(
    IN table_name_value varchar(64),
    IN column_name_value varchar(64),
    IN column_definition_value text
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @column_ddl = CONCAT('ALTER TABLE `', table_name_value,
                                 '` ADD COLUMN ', column_definition_value);
        PREPARE column_statement FROM @column_ddl;
        EXECUTE column_statement;
        DEALLOCATE PREPARE column_statement;
    END IF;
END$$
DELIMITER ;

CALL `crm_add_column_if_missing`('crm_customer', 'pool_status',
    '`pool_status` tinyint NOT NULL DEFAULT 0 COMMENT ''池状态：0在管、1公海、2垃圾池'' AFTER `owner_time`');
CALL `crm_add_column_if_missing`('crm_customer', 'pool_entry_time',
    '`pool_entry_time` datetime NULL COMMENT ''本次进入公海时间'' AFTER `pool_status`');
CALL `crm_add_column_if_missing`('crm_customer', 'pool_previous_owner_user_id',
    '`pool_previous_owner_user_id` bigint NULL COMMENT ''本次入池前负责人'' AFTER `pool_entry_time`');
CALL `crm_add_column_if_missing`('crm_customer', 'pool_reason',
    '`pool_reason` varchar(40) NULL COMMENT ''本次入池原因编码'' AFTER `pool_previous_owner_user_id`');
CALL `crm_add_column_if_missing`('crm_customer', 'pool_cycle_count',
    '`pool_cycle_count` int NOT NULL DEFAULT 0 COMMENT ''累计进入公海次数'' AFTER `pool_reason`');
CALL `crm_add_column_if_missing`('crm_customer', 'garbage_time',
    '`garbage_time` datetime NULL COMMENT ''进入垃圾池时间'' AFTER `pool_cycle_count`');
CALL `crm_add_column_if_missing`('crm_customer', 'garbage_reason',
    '`garbage_reason` varchar(500) NULL COMMENT ''进入垃圾池原因'' AFTER `garbage_time`');

CALL `crm_add_column_if_missing`('crm_customer_pool_config', 'daily_claim_limit',
    '`daily_claim_limit` int NULL COMMENT ''每日自助领取上限'' AFTER `notify_days`');
CALL `crm_add_column_if_missing`('crm_customer_pool_config', 'repeat_claim_cooldown_days',
    '`repeat_claim_cooldown_days` int NULL COMMENT ''重复领取冷却天数'' AFTER `daily_claim_limit`');
CALL `crm_add_column_if_missing`('crm_customer_pool_config', 'high_value_level_threshold',
    '`high_value_level_threshold` int NULL COMMENT ''重点客户等级阈值'' AFTER `repeat_claim_cooldown_days`');
CALL `crm_add_column_if_missing`('crm_customer_pool_config', 'high_value_expire_multiplier',
    '`high_value_expire_multiplier` int NULL COMMENT ''重点客户保护期倍数'' AFTER `high_value_level_threshold`');
CALL `crm_add_column_if_missing`('crm_customer_pool_config', 'protect_active_business',
    '`protect_active_business` tinyint(1) NULL COMMENT ''保护活跃商机'' AFTER `high_value_expire_multiplier`');
CALL `crm_add_column_if_missing`('crm_customer_pool_config', 'protect_active_contract',
    '`protect_active_contract` tinyint(1) NULL COMMENT ''保护未完结销售单据'' AFTER `protect_active_business`');
CALL `crm_add_column_if_missing`('crm_customer_pool_config', 'auto_pool_batch_size',
    '`auto_pool_batch_size` int NULL COMMENT ''自动回收单批数量'' AFTER `protect_active_contract`');

CALL `crm_add_column_if_missing`('crm_customer_owner_record', 'source',
    '`source` varchar(40) NULL COMMENT ''归属事件来源编码'' AFTER `type`');
CALL `crm_add_column_if_missing`('crm_customer_owner_record', 'reason',
    '`reason` varchar(500) NULL COMMENT ''归属事件原因'' AFTER `source`');

DROP PROCEDURE `crm_add_column_if_missing`;

CREATE TABLE IF NOT EXISTS `crm_customer_pool_claim_counter` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '领取用户',
  `claim_date` date NOT NULL COMMENT '领取业务日期',
  `claim_count` int NOT NULL DEFAULT 0 COMMENT '当日已领取数量',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_customer_pool_claim_counter` (`tenant_id`,`user_id`,`claim_date`),
  CONSTRAINT `chk_crm_customer_pool_claim_counter` CHECK (`claim_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 公海每日领取并发计数';

-- Align the legacy A/B/C values with the prototype's ordered 1..5 customer-star model.
-- The legacy marker is evaluated before dictionary rows are changed, so repeated runs never remap data twice.
SET @legacy_customer_level_model = (
    SELECT COUNT(*) > 0
    FROM `system_dict_data`
    WHERE `dict_type` = 'crm_customer_level' AND `value` = '1'
      AND `label` LIKE 'A%' AND `deleted` = b'0'
);

UPDATE `crm_customer`
SET `level` = CASE `level` WHEN 1 THEN 5 WHEN 2 THEN 3 WHEN 3 THEN 1 ELSE `level` END
WHERE @legacy_customer_level_model = 1 AND `level` IN (1, 2, 3);

UPDATE `crm_clue`
SET `level` = CASE `level` WHEN 1 THEN 5 WHEN 2 THEN 3 WHEN 3 THEN 1 ELSE `level` END
WHERE @legacy_customer_level_model = 1 AND `level` IN (1, 2, 3);

UPDATE `system_dict_data`
SET `sort` = CASE `value` WHEN '1' THEN 5 WHEN '2' THEN 3 WHEN '3' THEN 1 END,
    `label` = CASE `value` WHEN '1' THEN 'VIP 客户' WHEN '2' THEN '银牌客户'
                         WHEN '3' THEN '普通客户' END,
    `color_type` = CASE `value` WHEN '1' THEN 'danger' WHEN '2' THEN 'success'
                              WHEN '3' THEN 'default' END,
    `value` = CASE `value` WHEN '1' THEN '5' WHEN '2' THEN '3' WHEN '3' THEN '1' END,
    `updater` = 'pool-policy-migration', `update_time` = NOW()
WHERE @legacy_customer_level_model = 1 AND `dict_type` = 'crm_customer_level'
  AND `value` IN ('1', '2', '3') AND `deleted` = b'0';

INSERT INTO `system_dict_data`
  (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '铜牌客户', '2', 'crm_customer_level', 0, 'info', '', 'CRM 五级客户模型',
       'pool-policy-migration', NOW(), 'pool-policy-migration', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data`
                  WHERE `dict_type` = 'crm_customer_level' AND `value` = '2' AND `deleted` = b'0');

INSERT INTO `system_dict_data`
  (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 4, '金牌客户', '4', 'crm_customer_level', 0, 'warning', '', 'CRM 五级客户模型',
       'pool-policy-migration', NOW(), 'pool-policy-migration', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data`
                  WHERE `dict_type` = 'crm_customer_level' AND `value` = '4' AND `deleted` = b'0');

INSERT INTO `crm_customer_pool_claim_counter`
  (`user_id`, `claim_date`, `claim_count`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.`new_owner_user_id`, DATE(r.`create_time`), COUNT(*), 'pool-policy-migration', NOW(),
       'pool-policy-migration', NOW(), b'0', r.`tenant_id`
FROM `crm_customer_owner_record` r
WHERE r.`deleted` = b'0' AND r.`type` = 2 AND r.`source` = 'SELF_CLAIM'
GROUP BY r.`tenant_id`, r.`new_owner_user_id`, DATE(r.`create_time`)
ON DUPLICATE KEY UPDATE `claim_count` = GREATEST(`claim_count`, VALUES(`claim_count`));

-- Reconstruct explicit pool state from the compatible owner representation.
UPDATE `crm_customer`
SET `pool_status` = CASE WHEN `owner_user_id` IS NULL THEN 1 ELSE 0 END
WHERE (`owner_user_id` IS NULL AND `pool_status` = 0)
   OR (`owner_user_id` IS NOT NULL AND `pool_status` <> 0);

UPDATE `crm_customer` c
LEFT JOIN (
    SELECT r.`customer_id`, r.`create_time`,
           COALESCE(r.`previous_owner_user_id`, r.`owner_user_id`) AS previous_owner_user_id
    FROM `crm_customer_owner_record` r
    JOIN (
        SELECT `tenant_id`, `customer_id`, MAX(`id`) AS id
        FROM `crm_customer_owner_record`
        WHERE `deleted` = b'0' AND `type` = 1
        GROUP BY `tenant_id`, `customer_id`
    ) latest ON latest.`id` = r.`id` AND latest.`tenant_id` = r.`tenant_id`
) latest_pool ON latest_pool.`customer_id` = c.`id`
SET c.`pool_entry_time` = COALESCE(c.`pool_entry_time`, latest_pool.`create_time`, c.`update_time`),
    c.`pool_previous_owner_user_id` = COALESCE(c.`pool_previous_owner_user_id`,
                                               latest_pool.`previous_owner_user_id`),
    c.`pool_reason` = COALESCE(c.`pool_reason`, 'LEGACY_PUBLIC')
WHERE c.`deleted` = b'0' AND c.`pool_status` = 1;

UPDATE `crm_customer` c
LEFT JOIN (
    SELECT `tenant_id`, `customer_id`, COUNT(*) AS cycle_count
    FROM `crm_customer_owner_record`
    WHERE `deleted` = b'0' AND `type` = 1
    GROUP BY `tenant_id`, `customer_id`
) cycles ON cycles.`customer_id` = c.`id` AND cycles.`tenant_id` = c.`tenant_id`
SET c.`pool_cycle_count` = GREATEST(c.`pool_cycle_count`, COALESCE(cycles.`cycle_count`, 0));

SET @pool_status_check_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'crm_customer'
      AND constraint_name = 'chk_crm_customer_pool_status'
);
SET @pool_status_check_sql = IF(@pool_status_check_exists = 0,
    'ALTER TABLE crm_customer ADD CONSTRAINT chk_crm_customer_pool_status CHECK (pool_status IN (0,1,2))',
    'SELECT 1');
PREPARE pool_status_check_statement FROM @pool_status_check_sql;
EXECUTE pool_status_check_statement;
DEALLOCATE PREPARE pool_status_check_statement;

SET @pool_owner_check_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'crm_customer'
      AND constraint_name = 'chk_crm_customer_pool_owner'
);
SET @pool_owner_check_sql = IF(@pool_owner_check_exists = 0,
    'ALTER TABLE crm_customer ADD CONSTRAINT chk_crm_customer_pool_owner CHECK ((pool_status = 0 AND owner_user_id IS NOT NULL) OR (pool_status IN (1,2) AND owner_user_id IS NULL))',
    'SELECT 1');
PREPARE pool_owner_check_statement FROM @pool_owner_check_sql;
EXECUTE pool_owner_check_statement;
DEALLOCATE PREPARE pool_owner_check_statement;

SET @pool_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'crm_customer'
      AND index_name = 'idx_crm_customer_pool_state'
);
SET @pool_index_sql = IF(@pool_index_exists = 0,
    'ALTER TABLE crm_customer ADD INDEX idx_crm_customer_pool_state (tenant_id,pool_status,pool_entry_time,id)',
    'SELECT 1');
PREPARE pool_index_statement FROM @pool_index_sql;
EXECUTE pool_index_statement;
DEALLOCATE PREPARE pool_index_statement;

SET @owner_source_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'crm_customer_owner_record'
      AND index_name = 'idx_crm_customer_owner_source'
);
SET @owner_source_index_sql = IF(@owner_source_index_exists = 0,
    'ALTER TABLE crm_customer_owner_record ADD INDEX idx_crm_customer_owner_source (tenant_id,type,source,new_owner_user_id,create_time,customer_id)',
    'SELECT 1');
PREPARE owner_source_index_statement FROM @owner_source_index_sql;
EXECUTE owner_source_index_statement;
DEALLOCATE PREPARE owner_source_index_statement;

INSERT INTO `system_notify_template`
  (`name`, `code`, `nickname`, `content`, `type`, `params`, `status`, `remark`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '客户进入公海', 'crm-customer-put-pool', 'CRM',
       '客户 {customerName} 已进入公海，原因：{reason}。', 1,
       '["customerName","reason"]', 0, 'ADR-014 / CUS-012 / CUS-014',
       '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_notify_template`
    WHERE `code` = 'crm-customer-put-pool' AND `deleted` = b'0'
);
