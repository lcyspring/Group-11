-- OA 工作报告（日/周/月报）CRUD、接收人可见性和提交锁定。对已有数据卷幂等执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS crm_work_report (
  id bigint NOT NULL AUTO_INCREMENT,
  author_user_id bigint NOT NULL,
  report_type tinyint NOT NULL COMMENT '1 日报 2 周报 3 月报',
  report_date date NOT NULL,
  period_start date NOT NULL,
  period_end date NOT NULL,
  title varchar(200) NOT NULL,
  completed_content text NOT NULL,
  pending_content text NULL,
  next_plan text NOT NULL,
  issues text NULL,
  receiver_user_ids varchar(2000) NOT NULL,
  attachment_urls varchar(4000) NULL,
  status tinyint NOT NULL DEFAULT 0 COMMENT '0 草稿 1 已提交',
  submit_time datetime NULL,
  creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0', tenant_id bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_crm_work_report_period (tenant_id,author_user_id,report_type,period_start,deleted),
  KEY idx_crm_work_report_author (tenant_id,author_user_id,status,report_date,deleted),
  KEY idx_crm_work_report_date (tenant_id,report_date,report_type,deleted),
  CONSTRAINT chk_crm_work_report_type CHECK (report_type IN (1,2,3)),
  CONSTRAINT chk_crm_work_report_status CHECK (status IN (0,1)),
  CONSTRAINT chk_crm_work_report_period CHECK (period_end >= period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 工作报告';

INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6230,'工作报告','',2,50,5,'work-report','ep:document','crm/workReport/index','CrmWorkReport',0,b'1',b'1',b'1','oa-report',NOW(),'oa-report',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6230);
INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6231,'报告查询','crm:work-report:query',3,1,6230,'','','',NULL,0,b'1',b'1',b'1','oa-report',NOW(),'oa-report',NOW(),b'0' WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6231);
INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6232,'报告创建','crm:work-report:create',3,2,6230,'','','',NULL,0,b'1',b'1',b'1','oa-report',NOW(),'oa-report',NOW(),b'0' WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6232);
INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6233,'报告更新','crm:work-report:update',3,3,6230,'','','',NULL,0,b'1',b'1',b'1','oa-report',NOW(),'oa-report',NOW(),b'0' WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6233);
INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6234,'报告删除','crm:work-report:delete',3,4,6230,'','','',NULL,0,b'1',b'1',b'1','oa-report',NOW(),'oa-report',NOW(),b'0' WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6234);

INSERT INTO system_menu_i18n (`menu_id`,`language`,`name`)
SELECT menu.id,lang.language,CASE menu.id
 WHEN 6230 THEN CASE lang.language WHEN 'zh-CN' THEN '工作报告' WHEN 'en' THEN 'Work Reports' ELSE 'تقارير العمل' END
 WHEN 6231 THEN CASE lang.language WHEN 'zh-CN' THEN '报告查询' WHEN 'en' THEN 'View Reports' ELSE 'عرض التقارير' END
 WHEN 6232 THEN CASE lang.language WHEN 'zh-CN' THEN '报告创建' WHEN 'en' THEN 'Create Report' ELSE 'إنشاء تقرير' END
 WHEN 6233 THEN CASE lang.language WHEN 'zh-CN' THEN '报告更新' WHEN 'en' THEN 'Update Report' ELSE 'تحديث التقرير' END
 ELSE CASE lang.language WHEN 'zh-CN' THEN '报告删除' WHEN 'en' THEN 'Delete Report' ELSE 'حذف التقرير' END END
FROM system_menu menu JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') lang
WHERE menu.id IN (6230,6231,6232,6233,6234)
ON DUPLICATE KEY UPDATE name=VALUES(name),deleted=b'0';

INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id,menu.id,'oa-report',NOW(),'oa-report',NOW(),b'0',role.tenant_id FROM system_role role
JOIN system_menu menu ON menu.id IN (6230,6231,6232,6233,6234)
WHERE role.code='super_admin' AND role.deleted=b'0' AND NOT EXISTS
(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=role.id AND rm.menu_id=menu.id AND rm.tenant_id=role.tenant_id AND rm.deleted=b'0');
