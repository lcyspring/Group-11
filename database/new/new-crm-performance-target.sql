-- CRM 业绩目标：个人、部门、公司三层月度目标。
-- 年度和季度目标由月度数据汇总，不重复存储。
CREATE TABLE IF NOT EXISTS `crm_performance_target` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `scope_type` tinyint NOT NULL COMMENT '范围类型：1 公司、2 部门、3 个人',
  `scope_id` bigint NOT NULL COMMENT '范围编号；公司范围固定为 0',
  `target_year` smallint NOT NULL COMMENT '目标年度',
  `target_month` tinyint NOT NULL COMMENT '目标月份：1 至 12',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1 成交金额、2 回款金额、3 跟进次数、4 新增客户、5 新增商机',
  `target_value` decimal(20,2) NOT NULL DEFAULT 0.00 COMMENT '月度目标值',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `active_target_key` varchar(160) GENERATED ALWAYS AS (
    CASE WHEN `deleted` = b'0' THEN concat(`tenant_id`, '#', `scope_type`, '#', `scope_id`, '#',
      `target_year`, '#', `target_month`, '#', `target_type`) ELSE NULL END
  ) STORED COMMENT '有效目标唯一键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_performance_target_active` (`active_target_key`),
  KEY `idx_crm_performance_target_query` (`tenant_id`, `scope_type`, `scope_id`, `target_year`, `target_type`, `deleted`),
  CONSTRAINT `chk_crm_performance_target_scope` CHECK (
    (`scope_type` = 1 AND `scope_id` = 0) OR (`scope_type` IN (2, 3) AND `scope_id` > 0)
  ),
  CONSTRAINT `chk_crm_performance_target_period` CHECK (`target_year` BETWEEN 2000 AND 2100 AND `target_month` BETWEEN 1 AND 12),
  CONSTRAINT `chk_crm_performance_target_type` CHECK (`target_type` BETWEEN 1 AND 5),
  CONSTRAINT `chk_crm_performance_target_value` CHECK (`target_value` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 月度业绩目标表';

-- 在既有“员工业绩”菜单下增加独立维护权限，不自动向已有角色授予写权限。
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '业绩目标维护', 'crm:performance-target:update', 3, 1, 2736, '', '', '', '', 0, b'1', b'1', b'1',
  '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'crm:performance-target:update' AND `deleted` = b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '业绩目标删除', 'crm:performance-target:delete', 3, 2, 2736, '', '', '', '', 0, b'1', b'1', b'1',
  '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'crm:performance-target:delete' AND `deleted` = b'0');

INSERT INTO `system_menu_i18n` (`menu_id`, `language`, `name`)
SELECT `id`, 'zh-CN', '业绩目标维护' FROM `system_menu` WHERE `permission` = 'crm:performance-target:update' AND `deleted` = b'0'
UNION ALL SELECT `id`, 'en', 'Maintain Targets' FROM `system_menu` WHERE `permission` = 'crm:performance-target:update' AND `deleted` = b'0'
UNION ALL SELECT `id`, 'ar', 'صيانة الأهداف' FROM `system_menu` WHERE `permission` = 'crm:performance-target:update' AND `deleted` = b'0'
UNION ALL SELECT `id`, 'zh-CN', '业绩目标删除' FROM `system_menu` WHERE `permission` = 'crm:performance-target:delete' AND `deleted` = b'0'
UNION ALL SELECT `id`, 'en', 'Delete Targets' FROM `system_menu` WHERE `permission` = 'crm:performance-target:delete' AND `deleted` = b'0'
UNION ALL SELECT `id`, 'ar', 'حذف الأهداف' FROM `system_menu` WHERE `permission` = 'crm:performance-target:delete' AND `deleted` = b'0'
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `deleted` = b'0';
