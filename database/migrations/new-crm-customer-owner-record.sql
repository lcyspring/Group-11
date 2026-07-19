-- CRM 客户归属变更历史。脚本可重复执行，并保留 owner_user_id 兼容原公海统计 SQL。
CREATE TABLE IF NOT EXISTS `crm_customer_owner_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `customer_id` bigint NOT NULL COMMENT '客户编号',
  `owner_user_id` bigint NOT NULL COMMENT '放入前或领取后的负责人编号',
  `previous_owner_user_id` bigint NULL COMMENT '变更前负责人编号',
  `new_owner_user_id` bigint NULL COMMENT '变更后负责人编号',
  `type` tinyint NOT NULL COMMENT '类型：1 进入公海，2 领取或分配，3 初始分配，4 转移',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_tenant_owner_type_time` (`tenant_id`, `owner_user_id`, `type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户归属变更记录表';

SET @previous_owner_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_customer_owner_record'
      AND column_name = 'previous_owner_user_id'
);
SET @previous_owner_column_sql = IF(
    @previous_owner_column_exists = 0,
    'ALTER TABLE crm_customer_owner_record ADD COLUMN previous_owner_user_id bigint NULL COMMENT ''变更前负责人编号'' AFTER owner_user_id',
    'SELECT 1'
);
PREPARE previous_owner_column_statement FROM @previous_owner_column_sql;
EXECUTE previous_owner_column_statement;
DEALLOCATE PREPARE previous_owner_column_statement;

SET @new_owner_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_customer_owner_record'
      AND column_name = 'new_owner_user_id'
);
SET @new_owner_column_sql = IF(
    @new_owner_column_exists = 0,
    'ALTER TABLE crm_customer_owner_record ADD COLUMN new_owner_user_id bigint NULL COMMENT ''变更后负责人编号'' AFTER previous_owner_user_id',
    'SELECT 1'
);
PREPARE new_owner_column_statement FROM @new_owner_column_sql;
EXECUTE new_owner_column_statement;
DEALLOCATE PREPARE new_owner_column_statement;

-- 仅回填可确定语义的旧公海记录，不伪造历史操作人。
UPDATE crm_customer_owner_record
SET previous_owner_user_id = owner_user_id
WHERE type = 1 AND previous_owner_user_id IS NULL;

UPDATE crm_customer_owner_record
SET new_owner_user_id = owner_user_id
WHERE type = 2 AND new_owner_user_id IS NULL;
