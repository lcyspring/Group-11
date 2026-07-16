-- CRM 已签署合同补充协议、独立审批和生效版本。MySQL 8.0，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_contract_amendment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `contract_id` bigint NOT NULL COMMENT '合同编号',
  `no` varchar(64) NOT NULL COMMENT '补充协议编号',
  `client_request_id` varchar(128) NOT NULL COMMENT '客户端幂等请求号',
  `request_hash` char(64) NOT NULL COMMENT '请求内容 SHA-256',
  `base_version` int NOT NULL COMMENT '基线合同版本',
  `target_version` int NOT NULL COMMENT '生效目标版本',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `reason` varchar(1000) NOT NULL COMMENT '变更原因',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '审批状态：0草稿、10审批中、20通过、30驳回、40取消',
  `process_instance_id` varchar(64) NULL COMMENT '流程实例编号',
  `before_contract_snapshot` json NOT NULL COMMENT '变更前合同快照',
  `before_product_snapshot` json NOT NULL COMMENT '变更前产品快照',
  `after_contract_snapshot` json NOT NULL COMMENT '变更后合同快照',
  `after_product_snapshot` json NOT NULL COMMENT '变更后产品快照',
  `amount_before` decimal(24,6) NOT NULL COMMENT '变更前金额',
  `amount_after` decimal(24,6) NOT NULL COMMENT '变更后金额',
  `amount_delta` decimal(24,6) NOT NULL COMMENT '金额变化',
  `submitter_user_id` bigint NULL COMMENT '提交人',
  `submit_time` datetime NULL COMMENT '提交时间',
  `effective_time` datetime NULL COMMENT '生效时间',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_contract_amendment_no` (`tenant_id`,`no`),
  UNIQUE KEY `uk_crm_contract_amendment_request` (`tenant_id`,`client_request_id`),
  UNIQUE KEY `uk_crm_contract_amendment_version` (`tenant_id`,`contract_id`,`target_version`),
  KEY `idx_crm_contract_amendment_status` (`tenant_id`,`contract_id`,`audit_status`,`deleted`),
  CONSTRAINT `chk_crm_contract_amendment_version` CHECK (`base_version` > 0 AND `target_version` = `base_version` + 1),
  CONSTRAINT `chk_crm_contract_amendment_status` CHECK (`audit_status` IN (0,10,20,30,40)),
  CONSTRAINT `chk_crm_contract_amendment_amount` CHECK (`amount_after` > 0 AND `amount_delta` = `amount_after` - `amount_before`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 合同补充协议';

SET @amendment_id_exists = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='crm_contract_attachment' AND column_name='amendment_id');
SET @amendment_id_sql = IF(@amendment_id_exists=0,
 'ALTER TABLE crm_contract_attachment ADD COLUMN amendment_id bigint NULL COMMENT ''补充协议编号'' AFTER contract_id', 'SELECT 1');
PREPARE amendment_id_stmt FROM @amendment_id_sql; EXECUTE amendment_id_stmt; DEALLOCATE PREPARE amendment_id_stmt;

SET @amendment_attachment_index_exists = (SELECT COUNT(*) FROM information_schema.statistics
 WHERE table_schema=DATABASE() AND table_name='crm_contract_attachment' AND index_name='idx_crm_contract_attachment_amendment');
SET @amendment_attachment_index_sql = IF(@amendment_attachment_index_exists=0,
 'ALTER TABLE crm_contract_attachment ADD KEY idx_crm_contract_attachment_amendment (tenant_id,amendment_id,deleted)', 'SELECT 1');
PREPARE amendment_attachment_index_stmt FROM @amendment_attachment_index_sql;
EXECUTE amendment_attachment_index_stmt; DEALLOCATE PREPARE amendment_attachment_index_stmt;

SET @change_action_check_exists = (SELECT COUNT(*) FROM information_schema.table_constraints
 WHERE constraint_schema=DATABASE() AND table_name='crm_contract_change_record'
 AND constraint_name='chk_crm_contract_change_action' AND constraint_type='CHECK');
SET @drop_change_action_check_sql = IF(@change_action_check_exists>0,
 'ALTER TABLE crm_contract_change_record DROP CHECK chk_crm_contract_change_action', 'SELECT 1');
PREPARE drop_change_action_check_stmt FROM @drop_change_action_check_sql;
EXECUTE drop_change_action_check_stmt; DEALLOCATE PREPARE drop_change_action_check_stmt;
ALTER TABLE crm_contract_change_record ADD CONSTRAINT `chk_crm_contract_change_action` CHECK (`action_type` BETWEEN 1 AND 14);
ALTER TABLE crm_contract_change_record MODIFY COLUMN `action_type` tinyint NOT NULL
 COMMENT '动作：1创建、2修改、3提交、4通过、5驳回、6取消、7签署、8签署作废、9-14补充协议生命周期';

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '合同补充协议','crm:contract:amendment',3,9,m.id,'','','','',0,b'1',b'1',b'1','crm-amendment',NOW(),'crm-amendment',NOW(),b'0'
FROM `system_menu` m WHERE m.`path`='contract' AND m.`deleted`=b'0'
AND NOT EXISTS (SELECT 1 FROM `system_menu` x WHERE x.`permission`='crm:contract:amendment' AND x.`deleted`=b'0');

INSERT INTO `system_menu_i18n` (`menu_id`,`language`,`name`)
SELECT m.id,l.language,CASE l.language WHEN 'zh-CN' THEN '合同补充协议' WHEN 'en' THEN 'Contract Amendments' ELSE 'ملاحق العقد' END
FROM `system_menu` m JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') l
WHERE m.permission='crm:contract:amendment' AND m.deleted=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`),`deleted`=b'0';

INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id,menu.id,'crm-amendment',NOW(),'crm-amendment',NOW(),b'0',role.tenant_id
FROM system_role role JOIN system_menu menu ON menu.permission='crm:contract:amendment'
WHERE role.code='crm_admin' AND role.deleted=b'0' AND menu.deleted=b'0'
AND NOT EXISTS (SELECT 1 FROM system_role_menu existing WHERE existing.role_id=role.id
 AND existing.menu_id=menu.id AND existing.tenant_id=role.tenant_id AND existing.deleted=b'0');
