-- CRM 客服工单最小闭环（GAP-WO-001 / ADR-010）。脚本可重复执行。
CREATE TABLE IF NOT EXISTS `crm_work_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(50) NOT NULL COMMENT '工单编号',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `type` tinyint NOT NULL COMMENT '类型：1问题、2需求、3投诉、4咨询',
  `priority` tinyint NOT NULL COMMENT '优先级：1低、2中、3高',
  `status` tinyint NOT NULL COMMENT '状态：10待处理、20处理中、30已完结、40已退回',
  `customer_id` bigint NOT NULL COMMENT '客户编号',
  `source_type` tinyint NOT NULL COMMENT '来源：0客户、1商机、2合同',
  `source_id` bigint NULL COMMENT '来源对象编号',
  `handler_user_id` bigint NOT NULL COMMENT '处理人',
  `description` text NOT NULL COMMENT '工单描述',
  `solution` text NULL COMMENT '解决方案',
  `attachment_urls` json NULL COMMENT '附件 URL 数组',
  `process_time` datetime NULL COMMENT '开始处理时间',
  `complete_time` datetime NULL COMMENT '完结时间',
  `return_reason` varchar(1000) NULL COMMENT '退回原因',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_work_order_no_tenant` (`tenant_id`, `no`),
  KEY `idx_crm_work_order_scope` (`tenant_id`, `status`, `handler_user_id`, `customer_id`, `deleted`),
  KEY `idx_crm_work_order_creator` (`tenant_id`, `creator`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客服工单';

CREATE TABLE IF NOT EXISTS `crm_work_order_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `work_order_id` bigint NOT NULL COMMENT '工单编号',
  `action_type` tinyint NOT NULL COMMENT '操作：1创建、2修改、3开始、4退回、5重提、6完结、7分派',
  `from_status` tinyint NULL COMMENT '原状态',
  `to_status` tinyint NOT NULL COMMENT '目标状态',
  `operator_user_id` bigint NOT NULL COMMENT '操作人',
  `handler_user_id` bigint NOT NULL COMMENT '当时处理人',
  `remark` varchar(1000) NULL COMMENT '操作说明',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_work_order_record_order` (`tenant_id`, `work_order_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客服工单操作轨迹';

-- 兼容已创建的持久化表，仅更新动作字典注释，不改变列类型和数据。
ALTER TABLE `crm_work_order_record`
  MODIFY COLUMN `action_type` tinyint NOT NULL COMMENT '操作：1创建、2修改、3开始、4退回、5重提、6完结、7分派';

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'CRM 客服工单状态', 'crm_work_order_status', 0, 'CRM 客服工单状态', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'crm_work_order_status' AND `deleted` = b'0');
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'CRM 客服工单类型', 'crm_work_order_type', 0, 'CRM 客服工单类型', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'crm_work_order_type' AND `deleted` = b'0');
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'CRM 客服工单优先级', 'crm_work_order_priority', 0, 'CRM 客服工单优先级', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'crm_work_order_priority' AND `deleted` = b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 10, '待处理', '10', 'crm_work_order_status', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0' WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type`='crm_work_order_status' AND `value`='10' AND `deleted`=b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 20, '处理中', '20', 'crm_work_order_status', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0' WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type`='crm_work_order_status' AND `value`='20' AND `deleted`=b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 30, '已完结', '30', 'crm_work_order_status', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0' WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type`='crm_work_order_status' AND `value`='30' AND `deleted`=b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 40, '已退回', '40', 'crm_work_order_status', 0, 'danger', '', '', '1', NOW(), '1', NOW(), b'0' WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type`='crm_work_order_status' AND `value`='40' AND `deleted`=b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT d.sort, d.label, d.value, d.dict_type, 0, '', '', '', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT 1 sort, '问题' label, '1' value, 'crm_work_order_type' dict_type
  UNION ALL SELECT 2, '需求', '2', 'crm_work_order_type'
  UNION ALL SELECT 3, '投诉', '3', 'crm_work_order_type'
  UNION ALL SELECT 4, '咨询', '4', 'crm_work_order_type'
  UNION ALL SELECT 1, '低', '1', 'crm_work_order_priority'
  UNION ALL SELECT 2, '中', '2', 'crm_work_order_priority'
  UNION ALL SELECT 3, '高', '3', 'crm_work_order_priority'
) d
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` x WHERE x.`dict_type`=d.dict_type AND x.`value`=d.value AND x.`deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '客服工单', '', 2, 70, root.id, 'work-order', 'fa:ticket', 'crm/workorder/index', 'CrmWorkOrder', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM `system_menu` root
WHERE root.`path`='/crm' AND root.`parent_id`=0 AND root.`deleted`=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `path`='work-order' AND `parent_id`=root.id AND `deleted`=b'0');
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单查询', 'crm:work-order:query', 3, 1, id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0' FROM `system_menu` WHERE `path`='work-order' AND `parent_id` IN (SELECT id FROM (SELECT id FROM `system_menu` WHERE `path`='/crm' AND `parent_id`=0 AND `deleted`=b'0') root) AND `deleted`=b'0' AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='crm:work-order:query' AND `deleted`=b'0');
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单查询全部', 'crm:work-order:query-all', 3, 2, id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0' FROM `system_menu` WHERE `path`='work-order' AND `parent_id` IN (SELECT id FROM (SELECT id FROM `system_menu` WHERE `path`='/crm' AND `parent_id`=0 AND `deleted`=b'0') root) AND `deleted`=b'0' AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='crm:work-order:query-all' AND `deleted`=b'0');
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单创建', 'crm:work-order:create', 3, 3, id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0' FROM `system_menu` WHERE `path`='work-order' AND `parent_id` IN (SELECT id FROM (SELECT id FROM `system_menu` WHERE `path`='/crm' AND `parent_id`=0 AND `deleted`=b'0') root) AND `deleted`=b'0' AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='crm:work-order:create' AND `deleted`=b'0');
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单修改', 'crm:work-order:update', 3, 4, id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0' FROM `system_menu` WHERE `path`='work-order' AND `parent_id` IN (SELECT id FROM (SELECT id FROM `system_menu` WHERE `path`='/crm' AND `parent_id`=0 AND `deleted`=b'0') root) AND `deleted`=b'0' AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='crm:work-order:update' AND `deleted`=b'0');
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单删除', 'crm:work-order:delete', 3, 5, id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0' FROM `system_menu` WHERE `path`='work-order' AND `parent_id` IN (SELECT id FROM (SELECT id FROM `system_menu` WHERE `path`='/crm' AND `parent_id`=0 AND `deleted`=b'0') root) AND `deleted`=b'0' AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='crm:work-order:delete' AND `deleted`=b'0');
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单处理', 'crm:work-order:process', 3, 6, id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0' FROM `system_menu` WHERE `path`='work-order' AND `parent_id` IN (SELECT id FROM (SELECT id FROM `system_menu` WHERE `path`='/crm' AND `parent_id`=0 AND `deleted`=b'0') root) AND `deleted`=b'0' AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='crm:work-order:process' AND `deleted`=b'0');
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单分派', 'crm:work-order:assign', 3, 7, id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0' FROM `system_menu` WHERE `path`='work-order' AND `parent_id` IN (SELECT id FROM (SELECT id FROM `system_menu` WHERE `path`='/crm' AND `parent_id`=0 AND `deleted`=b'0') root) AND `deleted`=b'0' AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='crm:work-order:assign' AND `deleted`=b'0');

INSERT INTO `system_menu_i18n` (`menu_id`, `language`, `name`)
SELECT m.id, l.language,
  CASE l.language WHEN 'zh-CN' THEN '客服工单' WHEN 'en' THEN 'Service Work Orders' ELSE 'أوامر خدمة العملاء' END
FROM `system_menu` m
JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') l
WHERE m.`path`='work-order' AND m.`parent_id` IN (SELECT id FROM (SELECT id FROM `system_menu` WHERE `path`='/crm' AND `parent_id`=0 AND `deleted`=b'0') root) AND m.`deleted`=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `deleted`=b'0';

INSERT INTO `system_notify_template` (`name`, `code`, `nickname`, `content`, `type`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '客服工单已分派', 'crm-work-order-assigned', 'CRM', '客服工单 {no}「{title}」已分派给你，请及时处理。', 1, '["no","title"]', 0, 'GAP-WO-001', '1', NOW(), '1', NOW(), b'0' WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code`='crm-work-order-assigned' AND `deleted`=b'0');
INSERT INTO `system_notify_template` (`name`, `code`, `nickname`, `content`, `type`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '客服工单已退回', 'crm-work-order-returned', 'CRM', '客服工单 {no}「{title}」已退回，原因：{reason}。', 1, '["no","title","reason"]', 0, 'GAP-WO-001', '1', NOW(), '1', NOW(), b'0' WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code`='crm-work-order-returned' AND `deleted`=b'0');
INSERT INTO `system_notify_template` (`name`, `code`, `nickname`, `content`, `type`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '客服工单已完结', 'crm-work-order-completed', 'CRM', '客服工单 {no}「{title}」已完结，请查看处理结果。', 1, '["no","title"]', 0, 'GAP-WO-001', '1', NOW(), '1', NOW(), b'0' WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code`='crm-work-order-completed' AND `deleted`=b'0');
