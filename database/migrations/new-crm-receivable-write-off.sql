CREATE TABLE IF NOT EXISTS `crm_receivable_write_off` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receivable_id` bigint NOT NULL COMMENT '回款编号',
  `amount` decimal(18,6) NOT NULL COMMENT '核销金额',
  `write_off_time` datetime NOT NULL COMMENT '核销时间',
  `source_type` tinyint NOT NULL DEFAULT 1 COMMENT '1人工 2银行流水 3导入',
  `reference_no` varchar(128) DEFAULT NULL COMMENT '外部流水号',
  `remark` varchar(500) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0有效 10已冲销',
  `reversed_at` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_reference_tenant` (`reference_no`,`tenant_id`),
  KEY `idx_receivable_status` (`tenant_id`,`receivable_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM回款核销台账';

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '回款核销', 'crm:receivable:write-off', 3, 6, parent.id, '', '', '', NULL,
       0, b'1', b'1', b'1', 'receivable-write-off', NOW(), 'receivable-write-off', NOW(), b'0'
FROM system_menu parent
WHERE parent.path='receivable' AND parent.type=2 AND parent.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.permission='crm:receivable:write-off' AND m.deleted=b'0');

INSERT INTO system_menu_i18n (menu_id, language, name)
SELECT m.id, l.language,
       CASE l.language WHEN 'zh-CN' THEN '回款核销' WHEN 'en' THEN 'Receivable Write-off' ELSE 'تسوية المستحقات' END
FROM system_menu m CROSS JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') l
WHERE m.permission='crm:receivable:write-off' AND m.deleted=b'0'
ON DUPLICATE KEY UPDATE name=VALUES(name), deleted=b'0';

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT DISTINCT rm.role_id, target.id, 'receivable-write-off', NOW(), 'receivable-write-off', NOW(), b'0', rm.tenant_id
FROM system_role_menu rm JOIN system_menu source ON source.id=rm.menu_id
JOIN system_menu target ON target.permission='crm:receivable:write-off' AND target.deleted=b'0'
WHERE source.permission='crm:receivable:query' AND source.deleted=b'0' AND rm.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=rm.role_id AND x.menu_id=target.id
                 AND x.tenant_id=rm.tenant_id AND x.deleted=b'0');
