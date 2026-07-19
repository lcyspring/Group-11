-- OA leave calendar, balance reservation and attachment governance.
-- Idempotent for existing Podman volumes.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @attachment_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
 AND table_name='bpm_oa_leave' AND column_name='attachment_urls');
SET @attachment_sql = IF(@attachment_column=0,
 'ALTER TABLE bpm_oa_leave ADD COLUMN attachment_urls json NULL COMMENT ''附件 URL 列表'' AFTER reason', 'SELECT 1');
PREPARE attachment_stmt FROM @attachment_sql; EXECUTE attachment_stmt; DEALLOCATE PREPARE attachment_stmt;

SET @reserved_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
 AND table_name='bpm_oa_leave' AND column_name='balance_reserved');
SET @reserved_sql = IF(@reserved_column=0,
 'ALTER TABLE bpm_oa_leave ADD COLUMN balance_reserved bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否已预占余额'' AFTER process_instance_id', 'SELECT 1');
PREPARE reserved_stmt FROM @reserved_sql; EXECUTE reserved_stmt; DEALLOCATE PREPARE reserved_stmt;

SET @deducted_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
 AND table_name='bpm_oa_leave' AND column_name='balance_deducted');
SET @deducted_sql = IF(@deducted_column=0,
 'ALTER TABLE bpm_oa_leave ADD COLUMN balance_deducted bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否已结算为已用余额'' AFTER balance_reserved', 'SELECT 1');
PREPARE deducted_stmt FROM @deducted_sql; EXECUTE deducted_stmt; DEALLOCATE PREPARE deducted_stmt;

CREATE TABLE IF NOT EXISTS bpm_oa_leave_calendar (
  id bigint NOT NULL AUTO_INCREMENT, calendar_date date NOT NULL, workday bit(1) NOT NULL,
  name varchar(100) NOT NULL, creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0', tenant_id bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (id), UNIQUE KEY uk_bpm_oa_leave_calendar (tenant_id,calendar_date,deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 工作日与节假日覆盖日历';

CREATE TABLE IF NOT EXISTS bpm_oa_leave_balance (
  id bigint NOT NULL AUTO_INCREMENT, user_id bigint NOT NULL, leave_type tinyint NOT NULL,
  balance_year int NOT NULL, total_days bigint NOT NULL DEFAULT 0, reserved_days bigint NOT NULL DEFAULT 0,
  used_days bigint NOT NULL DEFAULT 0, creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0', tenant_id bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (id), UNIQUE KEY uk_bpm_oa_leave_balance (tenant_id,user_id,leave_type,balance_year,deleted),
  CONSTRAINT chk_bpm_oa_leave_balance_nonnegative CHECK (total_days >= 0 AND reserved_days >= 0 AND used_days >= 0),
  CONSTRAINT chk_bpm_oa_leave_balance_capacity CHECK (reserved_days + used_days <= total_days)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 年度假期余额';

INSERT INTO system_dict_data (`sort`,`label`,`value`,`dict_type`,`status`,`color_type`,`css_class`,`remark`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 3,'年假','4','bpm_oa_leave_type',0,'success','',NULL,'oa-leave',NOW(),'oa-leave',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='bpm_oa_leave_type' AND value='4' AND deleted=b'0');

INSERT INTO system_dict_data (`sort`,`label`,`value`,`dict_type`,`status`,`color_type`,`css_class`,`remark`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 4,'调休','5','bpm_oa_leave_type',0,'warning','',NULL,'oa-leave',NOW(),'oa-leave',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='bpm_oa_leave_type' AND value='5' AND deleted=b'0');
