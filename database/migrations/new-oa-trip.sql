-- OA 出差申请、审批状态回写与独立菜单。
-- 对已有 Podman 数据卷幂等执行。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bpm_oa_trip (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  start_time datetime NOT NULL,
  end_time datetime NOT NULL,
  days decimal(8,2) NOT NULL,
  destination varchar(200) NOT NULL,
  reason varchar(1000) NOT NULL,
  estimated_expense decimal(18,2) NOT NULL DEFAULT 0.00,
  companion_user_ids json NULL,
  attachment_urls json NULL,
  status tinyint NOT NULL,
  process_instance_id varchar(64) DEFAULT NULL,
  approval_time datetime DEFAULT NULL,
  creator varchar(64) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  tenant_id bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_bpm_oa_trip_user_status (tenant_id,user_id,status,deleted),
  KEY idx_bpm_oa_trip_process (process_instance_id),
  CONSTRAINT chk_bpm_oa_trip_time CHECK (end_time > start_time),
  CONSTRAINT chk_bpm_oa_trip_days CHECK (days > 0),
  CONSTRAINT chk_bpm_oa_trip_expense CHECK (estimated_expense >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 出差申请';

SET @trip_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
 AND table_name='crm_reimbursement' AND column_name='trip_id');
SET @trip_sql = IF(@trip_column=0,
 'ALTER TABLE crm_reimbursement ADD COLUMN trip_id bigint NULL COMMENT ''关联 OA 出差编号'' AFTER contract_id, ADD KEY idx_crm_reimbursement_trip (tenant_id,trip_id,deleted)',
 'SELECT 1');
PREPARE trip_stmt FROM @trip_sql; EXECUTE trip_stmt; DEALLOCATE PREPARE trip_stmt;

INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6200,'出差管理','',2,20,5,'trip','ep:suitcase','bpm/oa/trip/index','BpmOATrip',0,b'1',b'1',b'1','oa-trip',NOW(),'oa-trip',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6200);

INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6201,'出差查询','bpm:oa-trip:query',3,1,6200,'','','',NULL,0,b'1',b'1',b'1','oa-trip',NOW(),'oa-trip',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6201);

INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6202,'出差申请','bpm:oa-trip:create',3,2,6200,'','','',NULL,0,b'1',b'1',b'1','oa-trip',NOW(),'oa-trip',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6202);

UPDATE system_menu SET name='出差管理', parent_id=5, path='trip', component='bpm/oa/trip/index',
 component_name='BpmOATrip', icon='ep:suitcase', sort=20, deleted=b'0', updater='oa-trip', update_time=NOW()
WHERE id=6200;

INSERT INTO system_menu_i18n (`menu_id`,`language`,`name`)
SELECT menu.id, lang.language,
  CASE menu.id
    WHEN 6200 THEN CASE lang.language WHEN 'zh-CN' THEN '出差管理' WHEN 'en' THEN 'Business Trips' ELSE 'رحلات العمل' END
    WHEN 6201 THEN CASE lang.language WHEN 'zh-CN' THEN '出差查询' WHEN 'en' THEN 'View Trips' ELSE 'عرض الرحلات' END
    ELSE CASE lang.language WHEN 'zh-CN' THEN '出差申请' WHEN 'en' THEN 'Create Trip' ELSE 'إنشاء رحلة' END
  END
FROM system_menu menu
JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') lang
WHERE menu.id IN (6200,6201,6202)
ON DUPLICATE KEY UPDATE name=VALUES(name), deleted=b'0';

INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id, menu.id, 'oa-trip', NOW(), 'oa-trip', NOW(), b'0', role.tenant_id
FROM system_role role JOIN system_menu menu ON menu.id IN (6200,6201,6202)
WHERE role.code='super_admin' AND role.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id=role.id AND rm.menu_id=menu.id
 AND rm.tenant_id=role.tenant_id AND rm.deleted=b'0');
