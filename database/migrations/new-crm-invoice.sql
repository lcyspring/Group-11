-- CRM 发票完整生命周期台账（ADR-009）。脚本可重复执行。
CREATE TABLE IF NOT EXISTS `crm_invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(50) NOT NULL COMMENT '内部发票申请号',
  `contract_id` bigint NOT NULL COMMENT '合同编号',
  `customer_id` bigint NOT NULL COMMENT '客户编号快照',
  `owner_user_id` bigint NOT NULL COMMENT '负责人快照',
  `handler_user_id` bigint NOT NULL COMMENT '经手人',
  `direction` tinyint NOT NULL COMMENT '方向：1蓝票、-1红票',
  `original_invoice_id` bigint NULL COMMENT '原蓝票编号，红票必填',
  `status` tinyint NOT NULL COMMENT '状态：0草稿、10已开具、20部分红冲、30全部红冲、40已作废',
  `type` tinyint NOT NULL COMMENT '类型：1增值税普通发票、2增值税专用发票',
  `amount` decimal(24,6) NOT NULL COMMENT '票面金额，始终为正数',
  `red_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '蓝票累计有效红冲金额',
  `invoice_no` varchar(50) NULL COMMENT '税务发票号码',
  `invoice_date` datetime NULL COMMENT '开票日期',
  `title` varchar(200) NOT NULL COMMENT '购方抬头快照',
  `tax_no` varchar(64) NULL COMMENT '购方税号快照',
  `registered_address` varchar(255) NULL COMMENT '购方注册地址快照',
  `registered_phone` varchar(32) NULL COMMENT '购方注册电话快照',
  `bank_name` varchar(128) NULL COMMENT '购方开户行快照',
  `bank_account` varchar(64) NULL COMMENT '购方银行账号快照',
  `email` varchar(128) NULL COMMENT '接收邮箱',
  `content` varchar(500) NOT NULL COMMENT '开票内容快照',
  `external_provider` varchar(64) NULL COMMENT '外部开票适配器编码',
  `external_request_id` varchar(200) NULL COMMENT '外部幂等请求编号',
  `external_invoice_id` varchar(200) NULL COMMENT '外部平台票据编号',
  `issue_remark` varchar(500) NULL COMMENT '开具、作废或红冲说明',
  `remark` varchar(500) NULL COMMENT '草稿备注',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_invoice_no_tenant` (`tenant_id`, `no`),
  UNIQUE KEY `uk_crm_invoice_fiscal_no_tenant` (`tenant_id`, `invoice_no`),
  KEY `idx_crm_invoice_contract_status` (`tenant_id`, `contract_id`, `status`, `deleted`),
  KEY `idx_crm_invoice_customer_date` (`tenant_id`, `customer_id`, `invoice_date`, `deleted`),
  KEY `idx_crm_invoice_owner` (`tenant_id`, `owner_user_id`, `deleted`),
  KEY `idx_crm_invoice_original` (`tenant_id`, `original_invoice_id`, `status`, `deleted`),
  CONSTRAINT `chk_crm_invoice_direction` CHECK (`direction` IN (-1, 1)),
  CONSTRAINT `chk_crm_invoice_status` CHECK (`status` IN (0, 10, 20, 30, 40)),
  CONSTRAINT `chk_crm_invoice_type` CHECK (`type` IN (1, 2)),
  CONSTRAINT `chk_crm_invoice_amount` CHECK (`amount` > 0 AND `red_amount` >= 0 AND `red_amount` <= `amount`),
  CONSTRAINT `chk_crm_invoice_original` CHECK ((`direction` = 1 AND `original_invoice_id` IS NULL)
    OR (`direction` = -1 AND `original_invoice_id` IS NOT NULL AND `red_amount` = 0)),
  CONSTRAINT `chk_crm_invoice_fiscal_fields` CHECK (`status` = 0
    OR (`invoice_no` IS NOT NULL AND `invoice_date` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 合同发票生命周期台账';

CREATE TABLE IF NOT EXISTS `crm_invoice_action_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `invoice_id` bigint NOT NULL COMMENT '发票编号',
  `action_type` tinyint NOT NULL COMMENT '动作：1创建、2修改、3开具、4作废、5红冲、6作废红票、7删除',
  `from_status` tinyint NULL COMMENT '原状态',
  `to_status` tinyint NULL COMMENT '目标状态，删除动作为空',
  `operator_user_id` bigint NOT NULL COMMENT '操作人',
  `action_time` datetime NOT NULL COMMENT '业务动作时间',
  `provider_request_id` varchar(200) NULL COMMENT '外部适配器幂等请求编号',
  `remark` varchar(1000) NULL COMMENT '动作说明',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_invoice_action_invoice` (`tenant_id`, `invoice_id`, `id`),
  KEY `idx_crm_invoice_action_request` (`tenant_id`, `provider_request_id`),
  CONSTRAINT `chk_crm_invoice_action_type` CHECK (`action_type` BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 发票不可变动作轨迹';

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'CRM 发票状态', 'crm_invoice_status', 0, 'CRM 发票生命周期状态', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type`='crm_invoice_status' AND `deleted`=b'0');
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'CRM 发票类型', 'crm_invoice_type', 0, 'CRM 发票类型', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type`='crm_invoice_type' AND `deleted`=b'0');
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'CRM 发票方向', 'crm_invoice_direction', 0, '蓝票与红票方向', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type`='crm_invoice_direction' AND `deleted`=b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT d.sort, d.label, d.value, d.dict_type, 0, d.color_type, '', '', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT 0 sort, '草稿' label, '0' value, 'crm_invoice_status' dict_type, 'info' color_type
  UNION ALL SELECT 10, '已开具', '10', 'crm_invoice_status', 'success'
  UNION ALL SELECT 20, '部分红冲', '20', 'crm_invoice_status', 'warning'
  UNION ALL SELECT 30, '已全部红冲', '30', 'crm_invoice_status', 'danger'
  UNION ALL SELECT 40, '已作废', '40', 'crm_invoice_status', 'info'
  UNION ALL SELECT 1, '增值税普通发票', '1', 'crm_invoice_type', 'primary'
  UNION ALL SELECT 2, '增值税专用发票', '2', 'crm_invoice_type', 'warning'
  UNION ALL SELECT 1, '蓝票', '1', 'crm_invoice_direction', 'primary'
  UNION ALL SELECT 2, '红票', '-1', 'crm_invoice_direction', 'danger'
) d
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` x
  WHERE x.`dict_type`=d.dict_type AND x.`value`=d.value AND x.`deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '发票管理', '', 2, 62, root.id, 'invoice', 'ep:tickets', 'crm/invoice/index', 'CrmInvoice', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM `system_menu` root
WHERE root.`path`='/crm' AND root.`parent_id`=0 AND root.`deleted`=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='invoice' AND `parent_id`=root.id AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT p.name, p.permission, 3, p.sort, m.id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM `system_menu` m
JOIN (
  SELECT '发票查询' name, 'crm:invoice:query' permission, 1 sort
  UNION ALL SELECT '发票创建', 'crm:invoice:create', 2
  UNION ALL SELECT '发票修改', 'crm:invoice:update', 3
  UNION ALL SELECT '发票删除', 'crm:invoice:delete', 4
  UNION ALL SELECT '正式开票', 'crm:invoice:issue', 5
  UNION ALL SELECT '发票红冲', 'crm:invoice:red-flush', 6
  UNION ALL SELECT '发票作废', 'crm:invoice:void', 7
  UNION ALL SELECT '发票导出', 'crm:invoice:export', 8
) p
WHERE m.`path`='invoice'
  AND m.`parent_id` IN (SELECT id FROM (SELECT id FROM `system_menu` WHERE `path`='/crm' AND `parent_id`=0 AND `deleted`=b'0') root)
  AND m.`deleted`=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` x WHERE x.`permission`=p.permission AND x.`deleted`=b'0');

INSERT INTO `system_menu_i18n` (`menu_id`, `language`, `name`)
SELECT m.id, l.language,
  CASE l.language WHEN 'zh-CN' THEN '发票管理' WHEN 'en' THEN 'Invoice Management' ELSE 'إدارة الفواتير' END
FROM `system_menu` m
JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') l
WHERE m.`path`='invoice'
  AND m.`parent_id` IN (SELECT id FROM (SELECT id FROM `system_menu` WHERE `path`='/crm' AND `parent_id`=0 AND `deleted`=b'0') root)
  AND m.`deleted`=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `deleted`=b'0';

INSERT INTO `system_menu_i18n` (`menu_id`, `language`, `name`)
SELECT m.id, l.language,
  CASE m.permission
    WHEN 'crm:invoice:query' THEN CASE l.language WHEN 'zh-CN' THEN '发票查询' WHEN 'en' THEN 'Query Invoices' ELSE 'الاستعلام عن الفواتير' END
    WHEN 'crm:invoice:create' THEN CASE l.language WHEN 'zh-CN' THEN '发票创建' WHEN 'en' THEN 'Create Invoice' ELSE 'إنشاء فاتورة' END
    WHEN 'crm:invoice:update' THEN CASE l.language WHEN 'zh-CN' THEN '发票修改' WHEN 'en' THEN 'Update Invoice' ELSE 'تعديل الفاتورة' END
    WHEN 'crm:invoice:delete' THEN CASE l.language WHEN 'zh-CN' THEN '发票删除' WHEN 'en' THEN 'Delete Invoice' ELSE 'حذف الفاتورة' END
    WHEN 'crm:invoice:issue' THEN CASE l.language WHEN 'zh-CN' THEN '正式开票' WHEN 'en' THEN 'Issue Invoice' ELSE 'إصدار الفاتورة' END
    WHEN 'crm:invoice:red-flush' THEN CASE l.language WHEN 'zh-CN' THEN '发票红冲' WHEN 'en' THEN 'Credit Invoice' ELSE 'عكس الفاتورة' END
    WHEN 'crm:invoice:void' THEN CASE l.language WHEN 'zh-CN' THEN '发票作废' WHEN 'en' THEN 'Void Invoice' ELSE 'إبطال الفاتورة' END
    ELSE CASE l.language WHEN 'zh-CN' THEN '发票导出' WHEN 'en' THEN 'Export Invoices' ELSE 'تصدير الفواتير' END
  END
FROM `system_menu` m
JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') l
WHERE m.`permission` LIKE 'crm:invoice:%' AND m.`deleted`=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `deleted`=b'0';
