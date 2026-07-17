-- CRM 客户拜访申请、审批、结果回填和跟进联动。对已有数据卷幂等执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS crm_customer_visit (
  id bigint NOT NULL AUTO_INCREMENT,
  applicant_user_id bigint NOT NULL,
  customer_id bigint NOT NULL,
  contact_id bigint NULL,
  planned_start_time datetime NOT NULL,
  planned_end_time datetime NOT NULL,
  location varchar(300) NOT NULL,
  purpose varchar(1000) NOT NULL,
  participant_user_ids varchar(2000) NULL,
  attachment_urls varchar(4000) NULL,
  audit_status tinyint NOT NULL,
  process_instance_id varchar(64) NULL,
  approval_time datetime NULL,
  result_status tinyint NOT NULL DEFAULT 0,
  actual_start_time datetime NULL,
  actual_end_time datetime NULL,
  result_content varchar(2000) NULL,
  next_contact_time datetime NULL,
  result_attachment_urls varchar(4000) NULL,
  follow_up_record_id bigint NULL,
  creator varchar(64) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  tenant_id bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_crm_customer_visit_applicant (tenant_id,applicant_user_id,audit_status,deleted),
  KEY idx_crm_customer_visit_customer (tenant_id,customer_id,planned_start_time,deleted),
  KEY idx_crm_customer_visit_process (process_instance_id),
  UNIQUE KEY uk_crm_customer_visit_followup (tenant_id,follow_up_record_id,deleted),
  CONSTRAINT chk_crm_customer_visit_plan_time CHECK (planned_end_time > planned_start_time),
  CONSTRAINT chk_crm_customer_visit_actual_time CHECK (actual_end_time IS NULL OR actual_start_time IS NULL OR actual_end_time >= actual_start_time),
  CONSTRAINT chk_crm_customer_visit_result_status CHECK (result_status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户拜访';

-- LongListTypeHandler/StringListTypeHandler 的持久化契约为逗号分隔 VARCHAR；同时修复已执行过早期脚本的卷。
ALTER TABLE crm_customer_visit
  MODIFY COLUMN participant_user_ids varchar(2000) NULL,
  MODIFY COLUMN attachment_urls varchar(4000) NULL,
  MODIFY COLUMN result_attachment_urls varchar(4000) NULL;

INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6220,'客户拜访','',2,40,5,'customer-visit','ep:location','crm/customerVisit/index','CrmCustomerVisit',0,b'1',b'1',b'1','crm-visit',NOW(),'crm-visit',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6220);
INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6221,'拜访查询','crm:customer-visit:query',3,1,6220,'','','',NULL,0,b'1',b'1',b'1','crm-visit',NOW(),'crm-visit',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6221);
INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6222,'拜访申请','crm:customer-visit:create',3,2,6220,'','','',NULL,0,b'1',b'1',b'1','crm-visit',NOW(),'crm-visit',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6222);
INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6223,'拜访结果','crm:customer-visit:update',3,3,6220,'','','',NULL,0,b'1',b'1',b'1','crm-visit',NOW(),'crm-visit',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6223);

INSERT INTO system_menu_i18n (`menu_id`,`language`,`name`)
SELECT menu.id,lang.language,CASE menu.id
 WHEN 6220 THEN CASE lang.language WHEN 'zh-CN' THEN '客户拜访' WHEN 'en' THEN 'Customer Visits' ELSE 'زيارات العملاء' END
 WHEN 6221 THEN CASE lang.language WHEN 'zh-CN' THEN '拜访查询' WHEN 'en' THEN 'View Visits' ELSE 'عرض الزيارات' END
 WHEN 6222 THEN CASE lang.language WHEN 'zh-CN' THEN '拜访申请' WHEN 'en' THEN 'Create Visit' ELSE 'إنشاء زيارة' END
 ELSE CASE lang.language WHEN 'zh-CN' THEN '拜访结果' WHEN 'en' THEN 'Record Visit Result' ELSE 'تسجيل نتيجة الزيارة' END END
FROM system_menu menu JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') lang
WHERE menu.id IN (6220,6221,6222,6223)
ON DUPLICATE KEY UPDATE name=VALUES(name),deleted=b'0';

INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id,menu.id,'crm-visit',NOW(),'crm-visit',NOW(),b'0',role.tenant_id
FROM system_role role JOIN system_menu menu ON menu.id IN (6220,6221,6222,6223)
WHERE role.code='super_admin' AND role.deleted=b'0' AND NOT EXISTS
(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=role.id AND rm.menu_id=menu.id AND rm.tenant_id=role.tenant_id AND rm.deleted=b'0');
