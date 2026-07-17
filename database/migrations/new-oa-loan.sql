-- OA 借款、分次还款和菜单。幂等适配已有 Podman 数据卷。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bpm_oa_loan (
 id bigint NOT NULL AUTO_INCREMENT, user_id bigint NOT NULL, type varchar(30) NOT NULL,
 amount decimal(18,2) NOT NULL, reason varchar(1000) NOT NULL, trip_id bigint NULL,
 employee_level varchar(20) NOT NULL, approval_limit decimal(18,2) NOT NULL,
 escalated_approval bit(1) NOT NULL DEFAULT b'0', outstanding_amount decimal(18,2) NOT NULL DEFAULT 0,
 repayment_status tinyint NOT NULL DEFAULT 0, status tinyint NOT NULL, process_instance_id varchar(64) NULL,
 approval_time datetime NULL, repaid_time datetime NULL,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', tenant_id bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (id), KEY idx_bpm_oa_loan_user_outstanding (tenant_id,user_id,repayment_status,outstanding_amount,deleted),
 KEY idx_bpm_oa_loan_process (process_instance_id), KEY idx_bpm_oa_loan_trip (tenant_id,trip_id,deleted),
 CONSTRAINT chk_bpm_oa_loan_amount CHECK (amount > 0),
 CONSTRAINT chk_bpm_oa_loan_outstanding CHECK (outstanding_amount >= 0 AND outstanding_amount <= amount),
 CONSTRAINT chk_bpm_oa_loan_repayment CHECK (repayment_status IN (0,1,2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 借款申请';

CREATE TABLE IF NOT EXISTS bpm_oa_loan_repayment (
 id bigint NOT NULL AUTO_INCREMENT, loan_id bigint NOT NULL, user_id bigint NOT NULL,
 amount decimal(18,2) NOT NULL, repaid_at datetime NOT NULL, reference_no varchar(100) NULL, remark varchar(500) NULL,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', tenant_id bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (id), KEY idx_bpm_oa_loan_repayment (tenant_id,loan_id,repaid_at,deleted),
 CONSTRAINT chk_bpm_oa_loan_repayment_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 借款还款流水';

INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6210,'借款管理','',2,30,5,'loan','ep:money','bpm/oa/loan/index','BpmOALoan',0,b'1',b'1',b'1','oa-loan',NOW(),'oa-loan',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6210);
INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6211,'借款查询','bpm:oa-loan:query',3,1,6210,'','','',NULL,0,b'1',b'1',b'1','oa-loan',NOW(),'oa-loan',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6211);
INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6212,'借款申请','bpm:oa-loan:create',3,2,6210,'','','',NULL,0,b'1',b'1',b'1','oa-loan',NOW(),'oa-loan',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6212);
INSERT INTO system_menu (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 6213,'借款还款','bpm:oa-loan:update',3,3,6210,'','','',NULL,0,b'1',b'1',b'1','oa-loan',NOW(),'oa-loan',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=6213);

INSERT INTO system_menu_i18n (`menu_id`,`language`,`name`)
SELECT menu.id,lang.language,CASE menu.id
 WHEN 6210 THEN CASE lang.language WHEN 'zh-CN' THEN '借款管理' WHEN 'en' THEN 'Loan Management' ELSE 'إدارة السلف' END
 WHEN 6211 THEN CASE lang.language WHEN 'zh-CN' THEN '借款查询' WHEN 'en' THEN 'View Loans' ELSE 'عرض السلف' END
 WHEN 6212 THEN CASE lang.language WHEN 'zh-CN' THEN '借款申请' WHEN 'en' THEN 'Create Loan' ELSE 'إنشاء سلفة' END
 ELSE CASE lang.language WHEN 'zh-CN' THEN '借款还款' WHEN 'en' THEN 'Repay Loan' ELSE 'سداد السلفة' END END
FROM system_menu menu JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') lang
WHERE menu.id IN (6210,6211,6212,6213)
ON DUPLICATE KEY UPDATE name=VALUES(name),deleted=b'0';

INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id,menu.id,'oa-loan',NOW(),'oa-loan',NOW(),b'0',role.tenant_id
FROM system_role role JOIN system_menu menu ON menu.id IN (6210,6211,6212,6213)
WHERE role.code='super_admin' AND role.deleted=b'0' AND NOT EXISTS
(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=role.id AND rm.menu_id=menu.id AND rm.tenant_id=role.tenant_id AND rm.deleted=b'0');
