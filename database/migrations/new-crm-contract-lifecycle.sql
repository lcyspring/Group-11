-- CRM 合同签署、规范化附件与不可变版本轨迹（ADR-015）。脚本可重复执行。
CREATE TABLE IF NOT EXISTS `crm_contract_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `contract_id` bigint NOT NULL COMMENT '合同编号',
  `contract_version` int NOT NULL DEFAULT 1 COMMENT '所属合同版本',
  `category` tinyint NOT NULL COMMENT '类别：1普通附件、2签署副本、3变更依据',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_url` varchar(1024) NOT NULL COMMENT '文件地址',
  `content_type` varchar(128) NULL COMMENT '内容类型',
  `file_size` bigint NULL COMMENT '文件字节数',
  `sha256` char(64) NULL COMMENT '内容 SHA-256',
  `immutable` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否已锁定不可变',
  `uploader_user_id` bigint NOT NULL COMMENT '上传人',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_contract_attachment_contract` (`tenant_id`,`contract_id`,`contract_version`,`deleted`),
  CONSTRAINT `chk_crm_contract_attachment_category` CHECK (`category` IN (1,2,3)),
  CONSTRAINT `chk_crm_contract_attachment_size` CHECK (`file_size` IS NULL OR `file_size` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 合同规范化附件';

CREATE TABLE IF NOT EXISTS `crm_contract_signing` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `contract_id` bigint NOT NULL COMMENT '合同编号',
  `contract_version` int NOT NULL COMMENT '签署合同版本',
  `status` tinyint NOT NULL COMMENT '状态：10已签署、20已作废',
  `method` tinyint NOT NULL COMMENT '方式：1线下、2电子签',
  `signed_time` datetime NOT NULL COMMENT '实际签署时间',
  `signed_attachment_id` bigint NOT NULL COMMENT '签署副本附件编号',
  `handler_user_id` bigint NOT NULL COMMENT '签署经办人',
  `provider_code` varchar(64) NOT NULL COMMENT '签署适配器编码',
  `provider_request_id` varchar(200) NOT NULL COMMENT '稳定幂等请求号',
  `external_signing_id` varchar(200) NULL COMMENT '外部签署编号',
  `void_reason` varchar(500) NULL COMMENT '作废原因',
  `void_time` datetime NULL COMMENT '作废时间',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_contract_signing_contract` (`tenant_id`,`contract_id`),
  UNIQUE KEY `uk_crm_contract_signing_request` (`tenant_id`,`provider_request_id`),
  CONSTRAINT `chk_crm_contract_signing_status` CHECK (`status` IN (10,20)),
  CONSTRAINT `chk_crm_contract_signing_method` CHECK (`method` IN (1,2)),
  CONSTRAINT `chk_crm_contract_signing_void` CHECK ((`status`=10 AND `void_time` IS NULL) OR (`status`=20 AND `void_time` IS NOT NULL AND `void_reason` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 合同实际签署事实';

CREATE TABLE IF NOT EXISTS `crm_contract_change_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `contract_id` bigint NOT NULL COMMENT '合同编号',
  `sequence_no` int NOT NULL COMMENT '合同内事件序号',
  `contract_version` int NOT NULL COMMENT '合同业务版本',
  `action_type` tinyint NOT NULL COMMENT '动作：1创建、2修改、3提交、4通过、5驳回、6取消、7签署、8签署作废',
  `operator_user_id` bigint NULL COMMENT '操作人，系统回调可空',
  `reason` varchar(1000) NULL COMMENT '原因或摘要',
  `contract_snapshot` json NOT NULL COMMENT '合同字段快照',
  `product_snapshot` json NOT NULL COMMENT '合同产品快照',
  `action_time` datetime NOT NULL COMMENT '动作时间',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_contract_change_sequence` (`tenant_id`,`contract_id`,`sequence_no`),
  KEY `idx_crm_contract_change_version` (`tenant_id`,`contract_id`,`contract_version`,`id`),
  CONSTRAINT `chk_crm_contract_change_action` CHECK (`action_type` BETWEEN 1 AND 8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 合同不可变版本与生命周期轨迹';

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT p.name,p.permission,3,p.sort,m.id,'','','','',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM `system_menu` m JOIN (
  SELECT '合同附件维护' name,'crm:contract:attachment' permission,6 sort
  UNION ALL SELECT '合同签署','crm:contract:sign',7
  UNION ALL SELECT '合同签署作废','crm:contract:sign-void',8
) p
WHERE m.`path`='contract' AND m.`deleted`=b'0'
AND NOT EXISTS (SELECT 1 FROM `system_menu` x WHERE x.`permission`=p.permission AND x.`deleted`=b'0');

INSERT INTO `system_menu_i18n` (`menu_id`,`language`,`name`)
SELECT m.id,l.language,CASE m.permission
 WHEN 'crm:contract:attachment' THEN CASE l.language WHEN 'zh-CN' THEN '合同附件维护' WHEN 'en' THEN 'Manage Contract Attachments' ELSE 'إدارة مرفقات العقد' END
 WHEN 'crm:contract:sign' THEN CASE l.language WHEN 'zh-CN' THEN '合同签署' WHEN 'en' THEN 'Sign Contract' ELSE 'توقيع العقد' END
 ELSE CASE l.language WHEN 'zh-CN' THEN '合同签署作废' WHEN 'en' THEN 'Void Contract Signature' ELSE 'إبطال توقيع العقد' END END
FROM `system_menu` m JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') l
WHERE m.permission IN ('crm:contract:attachment','crm:contract:sign','crm:contract:sign-void') AND m.deleted=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`),`deleted`=b'0';
