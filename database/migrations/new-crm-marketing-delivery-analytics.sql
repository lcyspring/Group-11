-- CRM 群发提供商结果与邮件打开分析。可重复执行。
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_marketing_broadcast_recipient' AND column_name='delivery_status');
SET @ddl = IF(@column_exists=0,
  'ALTER TABLE crm_marketing_broadcast_recipient ADD COLUMN delivery_status tinyint NOT NULL DEFAULT 0 COMMENT ''提供商结果：0未知、10处理中、20短信送达、30失败、40邮件接受'' AFTER last_attempt_at',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_marketing_broadcast_recipient' AND column_name='delivered_at');
SET @ddl = IF(@column_exists=0,
  'ALTER TABLE crm_marketing_broadcast_recipient ADD COLUMN delivered_at datetime NULL COMMENT ''短信送达或邮件接受时间'' AFTER delivery_status',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_marketing_broadcast_recipient' AND column_name='opened_at');
SET @ddl = IF(@column_exists=0,
  'ALTER TABLE crm_marketing_broadcast_recipient ADD COLUMN opened_at datetime NULL COMMENT ''邮件首次打开时间'' AFTER delivered_at',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_marketing_broadcast_recipient' AND column_name='tracking_token');
SET @ddl = IF(@column_exists=0,
  'ALTER TABLE crm_marketing_broadcast_recipient ADD COLUMN tracking_token char(32) NULL COMMENT ''不可猜测的邮件打开追踪令牌'' AFTER opened_at',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='crm_marketing_broadcast_recipient' AND index_name='uk_crm_marketing_tracking_token');
SET @ddl = IF(@index_exists=0,
  'ALTER TABLE crm_marketing_broadcast_recipient ADD UNIQUE KEY uk_crm_marketing_tracking_token (tracking_token)',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='crm_marketing_broadcast_recipient' AND index_name='idx_crm_marketing_delivery_status');
SET @ddl = IF(@index_exists=0,
  'ALTER TABLE crm_marketing_broadcast_recipient ADD KEY idx_crm_marketing_delivery_status (tenant_id,broadcast_id,delivery_status,deleted)',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
