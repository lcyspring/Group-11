SET @business_end_time_column = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='crm_business' AND column_name='end_time');
SET @business_end_time_sql = IF(@business_end_time_column=0,
 'ALTER TABLE crm_business ADD COLUMN end_time datetime NULL COMMENT ''实际结单时间'' AFTER end_remark', 'SELECT 1');
PREPARE business_end_time_stmt FROM @business_end_time_sql;
EXECUTE business_end_time_stmt;
DEALLOCATE PREPARE business_end_time_stmt;

UPDATE crm_business
SET end_time=update_time
WHERE end_status IS NOT NULL AND end_time IS NULL;
