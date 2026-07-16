-- CRM 工单处理组、自动派单、未分配池与抄送协作。MySQL 8.0，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @wo_group_id_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_work_order' AND column_name='group_id');
SET @wo_group_id_sql = IF(@wo_group_id_exists=0,
  'ALTER TABLE crm_work_order ADD COLUMN group_id bigint NULL COMMENT ''客服处理组编号'' AFTER source_id', 'SELECT 1');
PREPARE wo_group_id_stmt FROM @wo_group_id_sql; EXECUTE wo_group_id_stmt; DEALLOCATE PREPARE wo_group_id_stmt;

SET @wo_dispatch_mode_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_work_order' AND column_name='dispatch_mode');
SET @wo_dispatch_mode_sql = IF(@wo_dispatch_mode_exists=0,
  'ALTER TABLE crm_work_order ADD COLUMN dispatch_mode tinyint NOT NULL DEFAULT 0 COMMENT ''派单方式：0未分配、1手工、2自动、3领取、4改派'' AFTER handler_user_id', 'SELECT 1');
PREPARE wo_dispatch_mode_stmt FROM @wo_dispatch_mode_sql; EXECUTE wo_dispatch_mode_stmt; DEALLOCATE PREPARE wo_dispatch_mode_stmt;

SET @wo_assign_time_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_work_order' AND column_name='assign_time');
SET @wo_assign_time_sql = IF(@wo_assign_time_exists=0,
  'ALTER TABLE crm_work_order ADD COLUMN assign_time datetime NULL COMMENT ''最近分配时间'' AFTER dispatch_mode', 'SELECT 1');
PREPARE wo_assign_time_stmt FROM @wo_assign_time_sql; EXECUTE wo_assign_time_stmt; DEALLOCATE PREPARE wo_assign_time_stmt;

ALTER TABLE crm_work_order MODIFY COLUMN handler_user_id bigint NULL COMMENT '处理人；未分配池为空';
ALTER TABLE crm_work_order_record MODIFY COLUMN handler_user_id bigint NULL COMMENT '当时处理人',
  MODIFY COLUMN action_type tinyint NOT NULL COMMENT '操作：1创建、2修改、3开始、4退回、5重提、6完结、7分派、8领取、9更新抄送人';

CREATE TABLE IF NOT EXISTS `crm_work_order_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `code` varchar(32) NOT NULL COMMENT '稳定编码',
  `name` varchar(100) NOT NULL COMMENT '处理组名称',
  `manager_user_id` bigint NOT NULL COMMENT '负责人用户编号',
  `supported_types` json NOT NULL COMMENT '支持工单类型',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0启用、1停用',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) NULL COMMENT '说明',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_work_order_group_code` (`tenant_id`,`code`),
  KEY `idx_crm_work_order_group_manager` (`tenant_id`,`manager_user_id`,`status`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客服处理组';

CREATE TABLE IF NOT EXISTS `crm_work_order_group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `group_id` bigint NOT NULL COMMENT '处理组编号',
  `user_id` bigint NOT NULL COMMENT '成员用户编号',
  `sort` int NOT NULL DEFAULT 0 COMMENT '组内排序',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_work_order_group_member` (`tenant_id`,`group_id`,`user_id`),
  KEY `idx_crm_work_order_group_member_user` (`tenant_id`,`user_id`,`group_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客服处理组成员';

CREATE TABLE IF NOT EXISTS `crm_work_order_cc` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `work_order_id` bigint NOT NULL COMMENT '工单编号',
  `user_id` bigint NOT NULL COMMENT '抄送用户编号',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_work_order_cc` (`tenant_id`,`work_order_id`,`user_id`),
  KEY `idx_crm_work_order_cc_user` (`tenant_id`,`user_id`,`work_order_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客服工单抄送人';

SET @wo_group_scope_index_exists = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='crm_work_order' AND index_name='idx_crm_work_order_group_scope');
SET @wo_group_scope_index_sql = IF(@wo_group_scope_index_exists=0,
  'ALTER TABLE crm_work_order ADD KEY idx_crm_work_order_group_scope (tenant_id,group_id,status,handler_user_id,priority,create_time,deleted)', 'SELECT 1');
PREPARE wo_group_scope_index_stmt FROM @wo_group_scope_index_sql; EXECUTE wo_group_scope_index_stmt; DEALLOCATE PREPARE wo_group_scope_index_stmt;

INSERT INTO system_menu
 (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '客服处理组','',2,71,root.id,'work-order-group','ep:user-filled','crm/workorder/group/index','CrmWorkOrderGroup',0,b'1',b'1',b'1','crm-work-order-group',NOW(),'crm-work-order-group',NOW(),b'0'
FROM system_menu root WHERE root.path='/crm' AND root.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.path='work-order-group' AND existing.deleted=b'0');

INSERT INTO system_menu
 (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT defs.name,defs.permission,3,defs.sort,page.id,'','','','',0,b'1',b'1',b'1','crm-work-order-group',NOW(),'crm-work-order-group',NOW(),b'0'
FROM system_menu page JOIN (
 SELECT '处理组查询' name,'crm:work-order-group:query' permission,1 sort
 UNION ALL SELECT '处理组维护','crm:work-order-group:update',2
 UNION ALL SELECT '处理组删除','crm:work-order-group:delete',3
) defs
WHERE page.path='work-order-group' AND page.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.permission=defs.permission AND existing.deleted=b'0');

INSERT INTO system_menu
 (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '跨组全量派单','crm:work-order:assign-all',3,8,page.id,'','','','',0,b'1',b'1',b'1','crm-work-order-group',NOW(),'crm-work-order-group',NOW(),b'0'
FROM system_menu page WHERE page.path='work-order' AND page.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.permission='crm:work-order:assign-all' AND existing.deleted=b'0');

INSERT INTO system_menu_i18n (`menu_id`,`language`,`name`)
SELECT menu.id,lang.language,
 CASE WHEN menu.path='work-order-group' THEN CASE lang.language WHEN 'zh-CN' THEN '客服处理组' WHEN 'en' THEN 'Service Groups' ELSE 'مجموعات الخدمة' END
      WHEN menu.permission='crm:work-order-group:query' THEN CASE lang.language WHEN 'zh-CN' THEN '处理组查询' WHEN 'en' THEN 'Query Service Groups' ELSE 'استعلام مجموعات الخدمة' END
      WHEN menu.permission='crm:work-order-group:update' THEN CASE lang.language WHEN 'zh-CN' THEN '处理组维护' WHEN 'en' THEN 'Maintain Service Groups' ELSE 'صيانة مجموعات الخدمة' END
      WHEN menu.permission='crm:work-order-group:delete' THEN CASE lang.language WHEN 'zh-CN' THEN '处理组删除' WHEN 'en' THEN 'Delete Service Groups' ELSE 'حذف مجموعات الخدمة' END
      ELSE CASE lang.language WHEN 'zh-CN' THEN '跨组全量派单' WHEN 'en' THEN 'Assign Across Groups' ELSE 'التعيين عبر المجموعات' END END
FROM system_menu menu JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') lang
WHERE (menu.path='work-order-group' OR menu.permission LIKE 'crm:work-order-group:%' OR menu.permission='crm:work-order:assign-all')
  AND menu.deleted=b'0'
ON DUPLICATE KEY UPDATE name=VALUES(name),deleted=b'0';

INSERT INTO system_notify_template
 (`name`,`code`,`nickname`,`content`,`type`,`params`,`status`,`remark`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '客服工单抄送','crm-work-order-copied','CRM','客服工单 {no}「{title}」已抄送给你，可查看后续处理结果。',1,'["no","title"]',0,'GAP-WO-002','crm-work-order-group',NOW(),'crm-work-order-group',NOW(),b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='crm-work-order-copied' AND deleted=b'0');

INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id,menu.id,'crm-work-order-group',NOW(),'crm-work-order-group',NOW(),b'0',role.tenant_id
FROM system_role role JOIN system_menu menu ON
  (menu.path='work-order-group' OR menu.permission LIKE 'crm:work-order-group:%' OR menu.permission='crm:work-order:assign-all')
WHERE role.code='crm_admin' AND role.deleted=b'0' AND menu.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing WHERE existing.role_id=role.id AND existing.menu_id=menu.id
    AND existing.tenant_id=role.tenant_id AND existing.deleted=b'0');
