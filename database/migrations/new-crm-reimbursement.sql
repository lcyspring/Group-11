-- CRM 财务域报销与费用分类（ADR-017）。脚本可重复执行。
CREATE TABLE IF NOT EXISTS `crm_expense_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `code` varchar(40) NOT NULL COMMENT '分类编码',
  `name` varchar(100) NOT NULL COMMENT '分类名称',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0启用、1停用',
  `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `description` varchar(500) NULL COMMENT '说明',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_expense_category_code` (`tenant_id`,`code`,`deleted`),
  UNIQUE KEY `uk_crm_expense_category_name` (`tenant_id`,`name`,`deleted`),
  KEY `idx_crm_expense_category_status` (`tenant_id`,`status`,`sort`,`deleted`),
  CONSTRAINT `chk_crm_expense_category_status` CHECK (`status` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 费用分类';

CREATE TABLE IF NOT EXISTS `crm_reimbursement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(50) NOT NULL COMMENT '报销编号',
  `applicant_user_id` bigint NOT NULL COMMENT '申请人快照',
  `owner_user_id` bigint NOT NULL COMMENT '负责人',
  `department_id` bigint NULL COMMENT '申请人部门快照',
  `customer_id` bigint NULL COMMENT '关联客户',
  `contract_id` bigint NULL COMMENT '关联合同',
  `currency` char(3) NOT NULL COMMENT 'ISO 4217 币种',
  `total_amount` decimal(24,6) NOT NULL COMMENT '服务端汇总金额',
  `expense_start_date` date NOT NULL COMMENT '费用开始日期',
  `expense_end_date` date NOT NULL COMMENT '费用结束日期',
  `reason` varchar(500) NOT NULL COMMENT '报销事由',
  `remark` varchar(1000) NULL COMMENT '备注',
  `process_instance_id` varchar(64) NULL COMMENT 'BPM 流程实例编号',
  `audit_status` tinyint NOT NULL COMMENT '审批状态：0草稿、10审批中、20通过、30驳回、40取消',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_reimbursement_no` (`tenant_id`,`no`),
  KEY `idx_crm_reimbursement_applicant` (`tenant_id`,`applicant_user_id`,`audit_status`,`deleted`),
  KEY `idx_crm_reimbursement_owner` (`tenant_id`,`owner_user_id`,`audit_status`,`deleted`),
  KEY `idx_crm_reimbursement_customer` (`tenant_id`,`customer_id`,`expense_start_date`,`deleted`),
  KEY `idx_crm_reimbursement_contract` (`tenant_id`,`contract_id`,`expense_start_date`,`deleted`),
  CONSTRAINT `chk_crm_reimbursement_amount` CHECK (`total_amount` > 0),
  CONSTRAINT `chk_crm_reimbursement_dates` CHECK (`expense_start_date` <= `expense_end_date`),
  CONSTRAINT `chk_crm_reimbursement_audit` CHECK (`audit_status` IN (0,10,20,30,40)),
  CONSTRAINT `chk_crm_reimbursement_currency` CHECK (CHAR_LENGTH(`currency`) = 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 报销单';

CREATE TABLE IF NOT EXISTS `crm_reimbursement_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `reimbursement_id` bigint NOT NULL COMMENT '报销单编号',
  `category_id` bigint NOT NULL COMMENT '费用分类编号',
  `occurred_date` date NOT NULL COMMENT '费用发生日期',
  `amount` decimal(24,6) NOT NULL COMMENT '明细金额',
  `description` varchar(500) NOT NULL COMMENT '费用说明',
  `invoice_no` varchar(100) NULL COMMENT '票据号码',
  `attachment_urls` varchar(6000) NULL COMMENT '受管附件 URL JSON 数组',
  `sort` int NOT NULL DEFAULT 0 COMMENT '明细顺序',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_reimbursement_item_parent` (`tenant_id`,`reimbursement_id`,`sort`,`deleted`),
  KEY `idx_crm_reimbursement_item_category` (`tenant_id`,`category_id`,`occurred_date`,`deleted`),
  CONSTRAINT `chk_crm_reimbursement_item_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 报销费用明细';

-- 兼容早期开发环境已创建的 2000 字符列；10 个受管 URL 的 JSON 上限需要更大空间。
ALTER TABLE `crm_reimbursement_item`
  MODIFY COLUMN `attachment_urls` varchar(6000) NULL COMMENT '受管附件 URL JSON 数组';

CREATE TABLE IF NOT EXISTS `crm_reimbursement_action_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `reimbursement_id` bigint NOT NULL COMMENT '报销单编号',
  `action_type` tinyint NOT NULL COMMENT '动作：1创建、2修改、3提交、4通过、5驳回、6取消、7删除',
  `from_status` tinyint NULL COMMENT '原审批状态',
  `to_status` tinyint NULL COMMENT '目标审批状态',
  `amount_snapshot` decimal(24,6) NOT NULL COMMENT '操作时金额快照',
  `operator_user_id` bigint NULL COMMENT '操作人，流程回调可空',
  `action_time` datetime NOT NULL COMMENT '动作时间',
  `process_instance_id` varchar(64) NULL COMMENT '流程实例编号',
  `remark` varchar(1000) NULL COMMENT '动作说明',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_reimbursement_action` (`tenant_id`,`reimbursement_id`,`id`),
  KEY `idx_crm_reimbursement_action_process` (`tenant_id`,`process_instance_id`),
  CONSTRAINT `chk_crm_reimbursement_action` CHECK (`action_type` BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 报销不可变动作轨迹';

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '报销管理','',2,64,root.id,'reimbursement','ep:wallet','crm/reimbursement/index','CrmReimbursement',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM `system_menu` root
WHERE root.`path`='/crm' AND root.`parent_id`=0 AND root.`deleted`=b'0'
AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='reimbursement' AND `parent_id`=root.id AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT p.name,p.permission,3,p.sort,m.id,'','','','',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM `system_menu` m JOIN (
  SELECT '报销查询' name,'crm:reimbursement:query' permission,1 sort
  UNION ALL SELECT '报销创建','crm:reimbursement:create',2
  UNION ALL SELECT '报销修改','crm:reimbursement:update',3
  UNION ALL SELECT '报销删除','crm:reimbursement:delete',4
  UNION ALL SELECT '费用分类维护','crm:expense-category:write',5
) p
WHERE m.`path`='reimbursement' AND m.`deleted`=b'0'
AND NOT EXISTS (SELECT 1 FROM `system_menu` x WHERE x.`permission`=p.permission AND x.`deleted`=b'0');

INSERT INTO `system_menu_i18n` (`menu_id`,`language`,`name`)
SELECT m.id,l.language,CASE l.language WHEN 'zh-CN' THEN '报销管理' WHEN 'en' THEN 'Reimbursements' ELSE 'إدارة المصروفات' END
FROM `system_menu` m JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') l
WHERE m.`path`='reimbursement' AND m.`deleted`=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`),`deleted`=b'0';
