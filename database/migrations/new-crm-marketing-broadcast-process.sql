SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'crm_marketing_broadcast'
    AND column_name = 'process_instance_id'
);
SET @sql := IF(@column_exists = 0,
  'ALTER TABLE crm_marketing_broadcast ADD COLUMN process_instance_id varchar(64) NULL COMMENT ''BPM 审批流程实例编号'' AFTER review_comment',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'crm_marketing_broadcast'
    AND index_name = 'idx_crm_marketing_broadcast_process'
);
SET @sql := IF(@index_exists = 0,
  'ALTER TABLE crm_marketing_broadcast ADD KEY idx_crm_marketing_broadcast_process (tenant_id, process_instance_id, deleted)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
