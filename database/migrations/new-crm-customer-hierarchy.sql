-- CRM 客户上下级关系。脚本可重复执行，历史客户默认都是根客户。
SET @parent_customer_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_customer'
      AND column_name = 'parent_customer_id'
);
SET @parent_customer_column_sql = IF(
    @parent_customer_column_exists = 0,
    'ALTER TABLE crm_customer ADD COLUMN parent_customer_id bigint NULL COMMENT ''上级客户编号'' AFTER name',
    'SELECT 1'
);
PREPARE parent_customer_column_statement FROM @parent_customer_column_sql;
EXECUTE parent_customer_column_statement;
DEALLOCATE PREPARE parent_customer_column_statement;

SET @parent_customer_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_customer'
      AND index_name = 'idx_crm_customer_parent'
);
SET @parent_customer_index_sql = IF(
    @parent_customer_index_exists = 0,
    'CREATE INDEX idx_crm_customer_parent ON crm_customer (tenant_id, parent_customer_id, deleted)',
    'SELECT 1'
);
PREPARE parent_customer_index_statement FROM @parent_customer_index_sql;
EXECUTE parent_customer_index_statement;
DEALLOCATE PREPARE parent_customer_index_statement;
