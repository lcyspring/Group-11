-- 商机赢单转合同幂等来源。
-- 历史合同可能允许一个商机关联多份合同，因此不回填 source_business_id；
-- 新的显式转换才写入该字段，并通过有效记录生成键防止并发重复创建。
SET @source_business_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_contract'
      AND column_name = 'source_business_id'
);
SET @source_business_column_sql = IF(
    @source_business_column_exists = 0,
    'ALTER TABLE crm_contract ADD COLUMN source_business_id bigint NULL COMMENT ''转换来源商机编号'' AFTER business_id',
    'SELECT 1'
);
PREPARE source_business_column_statement FROM @source_business_column_sql;
EXECUTE source_business_column_statement;
DEALLOCATE PREPARE source_business_column_statement;

SET @active_business_conversion_key_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_contract'
      AND column_name = 'active_business_conversion_key'
);
SET @active_business_conversion_key_column_sql = IF(
    @active_business_conversion_key_column_exists = 0,
    'ALTER TABLE crm_contract ADD COLUMN active_business_conversion_key varchar(96) GENERATED ALWAYS AS (CASE WHEN deleted = b''0'' AND source_business_id IS NOT NULL THEN concat(tenant_id, ''#'', source_business_id) ELSE NULL END) STORED COMMENT ''有效商机转换唯一键'' AFTER source_business_id',
    'SELECT 1'
);
PREPARE active_business_conversion_key_column_statement FROM @active_business_conversion_key_column_sql;
EXECUTE active_business_conversion_key_column_statement;
DEALLOCATE PREPARE active_business_conversion_key_column_statement;

SET @active_business_conversion_key_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_contract'
      AND index_name = 'uk_crm_contract_active_business_conversion'
);
SET @active_business_conversion_key_index_sql = IF(
    @active_business_conversion_key_index_exists = 0,
    'CREATE UNIQUE INDEX uk_crm_contract_active_business_conversion ON crm_contract (active_business_conversion_key)',
    'SELECT 1'
);
PREPARE active_business_conversion_key_index_statement FROM @active_business_conversion_key_index_sql;
EXECUTE active_business_conversion_key_index_statement;
DEALLOCATE PREPARE active_business_conversion_key_index_statement;

SET @source_business_query_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_contract'
      AND index_name = 'idx_crm_contract_source_business'
);
SET @source_business_query_index_sql = IF(
    @source_business_query_index_exists = 0,
    'CREATE INDEX idx_crm_contract_source_business ON crm_contract (tenant_id, source_business_id, deleted)',
    'SELECT 1'
);
PREPARE source_business_query_index_statement FROM @source_business_query_index_sql;
EXECUTE source_business_query_index_statement;
DEALLOCATE PREPARE source_business_query_index_statement;
