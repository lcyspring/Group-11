-- CRM 客户四态生命周期与不可变变更历史。脚本必须可重复执行。
SET @lifecycle_status_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'crm_customer' AND column_name = 'lifecycle_status'
);
SET @lifecycle_status_column_sql = IF(
    @lifecycle_status_column_exists = 0,
    'ALTER TABLE crm_customer ADD COLUMN lifecycle_status tinyint NOT NULL DEFAULT 10 COMMENT ''生命周期：10潜在、20意向、30成交、40流失'' AFTER deal_status',
    'SELECT 1'
);
PREPARE lifecycle_status_column_statement FROM @lifecycle_status_column_sql;
EXECUTE lifecycle_status_column_statement;
DEALLOCATE PREPARE lifecycle_status_column_statement;

SET @lifecycle_change_time_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'crm_customer' AND column_name = 'lifecycle_status_change_time'
);
SET @lifecycle_change_time_column_sql = IF(
    @lifecycle_change_time_column_exists = 0,
    'ALTER TABLE crm_customer ADD COLUMN lifecycle_status_change_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''生命周期状态最后变更时间'' AFTER lifecycle_status',
    'SELECT 1'
);
PREPARE lifecycle_change_time_column_statement FROM @lifecycle_change_time_column_sql;
EXECUTE lifecycle_change_time_column_statement;
DEALLOCATE PREPARE lifecycle_change_time_column_statement;

SET @lifecycle_lost_reason_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'crm_customer' AND column_name = 'lifecycle_lost_reason'
);
SET @lifecycle_lost_reason_column_sql = IF(
    @lifecycle_lost_reason_column_exists = 0,
    'ALTER TABLE crm_customer ADD COLUMN lifecycle_lost_reason varchar(500) NULL COMMENT ''当前流失原因'' AFTER lifecycle_status_change_time',
    'SELECT 1'
);
PREPARE lifecycle_lost_reason_column_statement FROM @lifecycle_lost_reason_column_sql;
EXECUTE lifecycle_lost_reason_column_statement;
DEALLOCATE PREPARE lifecycle_lost_reason_column_statement;

-- 旧数据只存在成交布尔值：已成交映射成交，其余映射潜在，不臆造意向或流失。
UPDATE crm_customer
SET lifecycle_status = CASE WHEN deal_status = b'1' THEN 30 ELSE 10 END
WHERE lifecycle_status IS NULL
   OR (lifecycle_status = 10 AND deal_status = b'1');

-- 在数据库层锁定四态值、成交兼容字段和当前流失原因的一致性。
SET @lifecycle_status_check_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'crm_customer'
      AND constraint_name = 'chk_crm_customer_lifecycle_status'
);
SET @lifecycle_status_check_sql = IF(
    @lifecycle_status_check_exists = 0,
    'ALTER TABLE crm_customer ADD CONSTRAINT chk_crm_customer_lifecycle_status CHECK (lifecycle_status IN (10,20,30,40))',
    'SELECT 1'
);
PREPARE lifecycle_status_check_statement FROM @lifecycle_status_check_sql;
EXECUTE lifecycle_status_check_statement;
DEALLOCATE PREPARE lifecycle_status_check_statement;

SET @lifecycle_deal_sync_check_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'crm_customer'
      AND constraint_name = 'chk_crm_customer_lifecycle_deal_sync'
);
SET @lifecycle_deal_sync_check_sql = IF(
    @lifecycle_deal_sync_check_exists = 0,
    'ALTER TABLE crm_customer ADD CONSTRAINT chk_crm_customer_lifecycle_deal_sync CHECK ((lifecycle_status = 30 AND deal_status = b''1'') OR (lifecycle_status <> 30 AND deal_status = b''0''))',
    'SELECT 1'
);
PREPARE lifecycle_deal_sync_check_statement FROM @lifecycle_deal_sync_check_sql;
EXECUTE lifecycle_deal_sync_check_statement;
DEALLOCATE PREPARE lifecycle_deal_sync_check_statement;

SET @lifecycle_lost_reason_check_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'crm_customer'
      AND constraint_name = 'chk_crm_customer_current_lost_reason'
);
SET @lifecycle_lost_reason_check_sql = IF(
    @lifecycle_lost_reason_check_exists = 0,
    'ALTER TABLE crm_customer ADD CONSTRAINT chk_crm_customer_current_lost_reason CHECK ((lifecycle_status = 40 AND lifecycle_lost_reason IS NOT NULL AND CHAR_LENGTH(TRIM(lifecycle_lost_reason)) > 0) OR (lifecycle_status <> 40 AND lifecycle_lost_reason IS NULL))',
    'SELECT 1'
);
PREPARE lifecycle_lost_reason_check_statement FROM @lifecycle_lost_reason_check_sql;
EXECUTE lifecycle_lost_reason_check_statement;
DEALLOCATE PREPARE lifecycle_lost_reason_check_statement;

SET @lifecycle_status_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'crm_customer'
      AND index_name = 'idx_crm_customer_lifecycle_status'
);
SET @lifecycle_status_index_sql = IF(
    @lifecycle_status_index_exists = 0,
    'ALTER TABLE crm_customer ADD INDEX idx_crm_customer_lifecycle_status (tenant_id, lifecycle_status, owner_user_id, create_time)',
    'SELECT 1'
);
PREPARE lifecycle_status_index_statement FROM @lifecycle_status_index_sql;
EXECUTE lifecycle_status_index_statement;
DEALLOCATE PREPARE lifecycle_status_index_statement;

CREATE TABLE IF NOT EXISTS `crm_customer_lifecycle_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `customer_id` bigint NOT NULL COMMENT '客户编号',
  `from_status` tinyint NOT NULL COMMENT '变更前状态',
  `to_status` tinyint NOT NULL COMMENT '变更后状态',
  `reason` varchar(500) NULL COMMENT '变更原因',
  `operator_user_id` bigint NULL COMMENT '操作人编号；兼容系统迁移可空',
  `change_time` datetime NOT NULL COMMENT '业务变更时间',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_customer_lifecycle_record` (`tenant_id`,`customer_id`,`change_time`,`id`),
  CONSTRAINT `chk_crm_customer_lifecycle_from_status` CHECK (`from_status` IN (10,20,30,40)),
  CONSTRAINT `chk_crm_customer_lifecycle_to_status` CHECK (`to_status` IN (10,20,30,40)),
  CONSTRAINT `chk_crm_customer_lifecycle_lost_reason` CHECK (`to_status` <> 40 OR (`reason` IS NOT NULL AND CHAR_LENGTH(TRIM(`reason`)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户生命周期变更历史';
