-- 仅约束有效联系人；历史空手机号保留，由应用层要求新建和更新时必填。
-- 生成列让逻辑删除后的手机号可以重新使用，也允许保留多条历史删除记录。
SET @active_mobile_key_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_contact'
      AND column_name = 'active_mobile_key'
);
SET @active_mobile_key_column_sql = IF(
    @active_mobile_key_column_exists = 0,
    'ALTER TABLE crm_contact ADD COLUMN active_mobile_key varchar(64) GENERATED ALWAYS AS (CASE WHEN deleted = b''0'' AND customer_id IS NOT NULL AND mobile IS NOT NULL AND trim(mobile) <> '''' THEN concat(coalesce(tenant_id, 0), ''#'', customer_id, ''#'', mobile) ELSE NULL END) STORED COMMENT ''有效联系人手机号唯一键'' AFTER mobile',
    'SELECT 1'
);
PREPARE active_mobile_key_column_statement FROM @active_mobile_key_column_sql;
EXECUTE active_mobile_key_column_statement;
DEALLOCATE PREPARE active_mobile_key_column_statement;

SET @active_mobile_key_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_contact'
      AND index_name = 'uk_crm_contact_active_mobile'
);
SET @active_mobile_key_index_sql = IF(
    @active_mobile_key_index_exists = 0,
    'CREATE UNIQUE INDEX uk_crm_contact_active_mobile ON crm_contact (active_mobile_key)',
    'SELECT 1'
);
PREPARE active_mobile_key_index_statement FROM @active_mobile_key_index_sql;
EXECUTE active_mobile_key_index_statement;
DEALLOCATE PREPARE active_mobile_key_index_statement;

SET @customer_mobile_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_contact'
      AND index_name = 'idx_crm_contact_customer_mobile'
);
SET @customer_mobile_index_sql = IF(
    @customer_mobile_index_exists = 0,
    'CREATE INDEX idx_crm_contact_customer_mobile ON crm_contact (tenant_id, customer_id, mobile, deleted)',
    'SELECT 1'
);
PREPARE customer_mobile_index_statement FROM @customer_mobile_index_sql;
EXECUTE customer_mobile_index_statement;
DEALLOCATE PREPARE customer_mobile_index_statement;
