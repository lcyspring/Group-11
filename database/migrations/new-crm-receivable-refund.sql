-- CRM 回款退款/冲销业务反向记录（ADR-007/008）。脚本可重复执行。
CREATE TABLE IF NOT EXISTS `crm_receivable_refund` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(50) NOT NULL COMMENT '退款/冲销业务编号',
  `receivable_id` bigint NOT NULL COMMENT '原审批通过回款编号',
  `customer_id` bigint NOT NULL COMMENT '客户编号快照',
  `contract_id` bigint NOT NULL COMMENT '合同编号快照',
  `owner_user_id` bigint NOT NULL COMMENT '负责人快照',
  `type` tinyint NOT NULL COMMENT '类型：1客户退款、2业务冲销',
  `refund_time` datetime NOT NULL COMMENT '退款/冲销业务日期',
  `amount` decimal(24,6) NOT NULL COMMENT '退款/冲销金额，始终为正数',
  `reason` varchar(500) NOT NULL COMMENT '业务原因',
  `remark` varchar(1000) NULL COMMENT '备注',
  `process_instance_id` varchar(64) NULL COMMENT 'BPM 流程实例编号',
  `audit_status` tinyint NOT NULL COMMENT '审批状态：0草稿、10审批中、20通过、30驳回、40取消',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_receivable_refund_no_tenant` (`tenant_id`, `no`),
  KEY `idx_crm_receivable_refund_source` (`tenant_id`, `receivable_id`, `audit_status`, `deleted`),
  KEY `idx_crm_receivable_refund_contract` (`tenant_id`, `contract_id`, `refund_time`, `deleted`),
  KEY `idx_crm_receivable_refund_customer` (`tenant_id`, `customer_id`, `refund_time`, `deleted`),
  KEY `idx_crm_receivable_refund_owner` (`tenant_id`, `owner_user_id`, `deleted`),
  CONSTRAINT `chk_crm_receivable_refund_type` CHECK (`type` IN (1,2)),
  CONSTRAINT `chk_crm_receivable_refund_amount` CHECK (`amount` > 0),
  CONSTRAINT `chk_crm_receivable_refund_audit` CHECK (`audit_status` IN (0,10,20,30,40)),
  CONSTRAINT `chk_crm_receivable_refund_reason` CHECK (CHAR_LENGTH(`reason`) BETWEEN 10 AND 500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 回款退款与业务冲销记录';

CREATE TABLE IF NOT EXISTS `crm_receivable_refund_action_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `refund_id` bigint NOT NULL COMMENT '退款/冲销编号',
  `action_type` tinyint NOT NULL COMMENT '动作：1创建、2修改、3提交、4通过、5驳回、6取消、7删除',
  `from_status` tinyint NULL COMMENT '原审批状态',
  `to_status` tinyint NULL COMMENT '目标审批状态',
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
  KEY `idx_crm_receivable_refund_action` (`tenant_id`, `refund_id`, `id`),
  KEY `idx_crm_receivable_refund_process` (`tenant_id`, `process_instance_id`),
  CONSTRAINT `chk_crm_receivable_refund_action` CHECK (`action_type` BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 退款/冲销不可变动作轨迹';

INSERT INTO `system_dict_type` (`name`,`type`,`status`,`remark`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 'CRM 退款/冲销类型','crm_receivable_refund_type',0,'客户退款与业务冲销','1',NOW(),'1',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type`='crm_receivable_refund_type' AND `deleted`=b'0');

INSERT INTO `system_dict_data` (`sort`,`label`,`value`,`dict_type`,`status`,`color_type`,`css_class`,`remark`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT d.sort,d.label,d.value,'crm_receivable_refund_type',0,d.color_type,'','','1',NOW(),'1',NOW(),b'0'
FROM (SELECT 1 sort,'客户退款' label,'1' value,'warning' color_type
      UNION ALL SELECT 2,'业务冲销','2','danger') d
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` x
  WHERE x.`dict_type`='crm_receivable_refund_type' AND x.`value`=d.value AND x.`deleted`=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '退款/冲销管理','',2,63,root.id,'receivable-refund','ep:remove-filled','crm/refund/index','CrmReceivableRefund',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM `system_menu` root
WHERE root.`path`='/crm' AND root.`parent_id`=0 AND root.`deleted`=b'0'
AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='receivable-refund' AND `parent_id`=root.id AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT p.name,p.permission,3,p.sort,m.id,'','','','',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM `system_menu` m JOIN (
  SELECT '退款/冲销查询' name,'crm:receivable-refund:query' permission,1 sort
  UNION ALL SELECT '退款/冲销创建','crm:receivable-refund:create',2
  UNION ALL SELECT '退款/冲销修改','crm:receivable-refund:update',3
  UNION ALL SELECT '退款/冲销删除','crm:receivable-refund:delete',4
) p
WHERE m.`path`='receivable-refund' AND m.`deleted`=b'0'
AND NOT EXISTS (SELECT 1 FROM `system_menu` x WHERE x.`permission`=p.permission AND x.`deleted`=b'0');

INSERT INTO `system_menu_i18n` (`menu_id`,`language`,`name`)
SELECT m.id,l.language,CASE l.language WHEN 'zh-CN' THEN '退款/冲销管理'
 WHEN 'en' THEN 'Refund / Reversal' ELSE 'الاسترداد / العكس' END
FROM `system_menu` m JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') l
WHERE m.`path`='receivable-refund' AND m.`deleted`=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`),`deleted`=b'0';

INSERT INTO `system_menu_i18n` (`menu_id`,`language`,`name`)
SELECT m.id,l.language,CASE m.permission
 WHEN 'crm:receivable-refund:query' THEN CASE l.language WHEN 'zh-CN' THEN '退款/冲销查询' WHEN 'en' THEN 'Query Refunds' ELSE 'استعلام الاسترداد' END
 WHEN 'crm:receivable-refund:create' THEN CASE l.language WHEN 'zh-CN' THEN '退款/冲销创建' WHEN 'en' THEN 'Create Refund' ELSE 'إنشاء استرداد' END
 WHEN 'crm:receivable-refund:update' THEN CASE l.language WHEN 'zh-CN' THEN '退款/冲销修改' WHEN 'en' THEN 'Update Refund' ELSE 'تعديل الاسترداد' END
 ELSE CASE l.language WHEN 'zh-CN' THEN '退款/冲销删除' WHEN 'en' THEN 'Delete Refund' ELSE 'حذف الاسترداد' END END
FROM `system_menu` m JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') l
WHERE m.`permission` LIKE 'crm:receivable-refund:%' AND m.`deleted`=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`),`deleted`=b'0';
