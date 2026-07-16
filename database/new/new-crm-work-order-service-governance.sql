-- CRM 工单移动签到、地理围栏、SLA、节假日、暂停和自动升级。
-- MySQL 8.0；所有 DDL/DML 均可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='crm_work_order' AND column_name='service_latitude');
SET @s = IF(@c=0, 'ALTER TABLE crm_work_order ADD COLUMN service_latitude decimal(10,7) NULL COMMENT ''服务地点纬度'' AFTER source_id', 'SELECT 1');
PREPARE x FROM @s; EXECUTE x; DEALLOCATE PREPARE x;
SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='crm_work_order' AND column_name='service_longitude');
SET @s = IF(@c=0, 'ALTER TABLE crm_work_order ADD COLUMN service_longitude decimal(10,7) NULL COMMENT ''服务地点经度'' AFTER service_latitude', 'SELECT 1');
PREPARE x FROM @s; EXECUTE x; DEALLOCATE PREPARE x;
SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='crm_work_order' AND column_name='geofence_radius_meters');
SET @s = IF(@c=0, 'ALTER TABLE crm_work_order ADD COLUMN geofence_radius_meters int NULL COMMENT ''地理围栏半径（米）'' AFTER service_longitude', 'SELECT 1');
PREPARE x FROM @s; EXECUTE x; DEALLOCATE PREPARE x;
SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='crm_work_order' AND column_name='check_in_required');
SET @s = IF(@c=0, 'ALTER TABLE crm_work_order ADD COLUMN check_in_required bit(1) NOT NULL DEFAULT b''0'' COMMENT ''完结前是否必须签到'' AFTER geofence_radius_meters', 'SELECT 1');
PREPARE x FROM @s; EXECUTE x; DEALLOCATE PREPARE x;

ALTER TABLE crm_work_order_record MODIFY COLUMN action_type tinyint NOT NULL COMMENT '操作：1创建、2修改、3开始、4退回、5重提、6完结、7分派、8领取、9抄送、10签到、11暂停SLA、12恢复SLA、13自动升级';

CREATE TABLE IF NOT EXISTS `crm_work_order_check_in` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `work_order_id` bigint NOT NULL COMMENT '工单编号',
  `user_id` bigint NOT NULL COMMENT '签到用户',
  `latitude` decimal(10,7) NOT NULL COMMENT '签到纬度',
  `longitude` decimal(10,7) NOT NULL COMMENT '签到经度',
  `accuracy_meters` decimal(10,2) NULL COMMENT '定位精度（米）',
  `distance_meters` decimal(10,2) NOT NULL COMMENT '距服务地点距离（米）',
  `result` tinyint NOT NULL DEFAULT 1 COMMENT '结果：1成功',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_work_order_check_in_order` (`tenant_id`,`work_order_id`,`create_time`,`deleted`),
  KEY `idx_crm_work_order_check_in_user` (`tenant_id`,`user_id`,`create_time`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 工单移动签到记录';

CREATE TABLE IF NOT EXISTS `crm_work_order_sla_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `code` varchar(64) NOT NULL COMMENT '策略编码',
  `name` varchar(100) NOT NULL COMMENT '策略名称',
  `priority` tinyint NOT NULL COMMENT '适用优先级',
  `response_minutes` int NOT NULL COMMENT '首次响应工作分钟数',
  `resolution_minutes` int NOT NULL COMMENT '解决工作分钟数',
  `escalation_minutes` int NOT NULL COMMENT '升级提前工作分钟数',
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `sort` int NOT NULL DEFAULT 0,
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_work_order_sla_policy` (`tenant_id`,`code`,`deleted`),
  KEY `idx_crm_work_order_sla_policy_priority` (`tenant_id`,`priority`,`enabled`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 工单 SLA 策略';

CREATE TABLE IF NOT EXISTS `crm_work_order_holiday` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `holiday_date` date NOT NULL COMMENT '日期',
  `name` varchar(100) NOT NULL COMMENT '名称',
  `working_day` bit(1) NOT NULL DEFAULT b'0' COMMENT '调休工作日',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_work_order_holiday` (`tenant_id`,`holiday_date`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 工单工作日历';

CREATE TABLE IF NOT EXISTS `crm_work_order_sla` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `work_order_id` bigint NOT NULL COMMENT '工单编号',
  `policy_id` bigint NOT NULL COMMENT '策略编号',
  `response_due_time` datetime NOT NULL COMMENT '首次响应截止时间',
  `escalation_due_time` datetime NULL COMMENT '自动升级时间',
  `resolution_due_time` datetime NOT NULL COMMENT '解决截止时间',
  `paused_seconds` bigint NOT NULL DEFAULT 0 COMMENT '累计暂停秒数',
  `paused_at` datetime NULL COMMENT '暂停开始时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0进行中、1已响应、2已解决、3已逾期、4已升级',
  `escalated_at` datetime NULL COMMENT '升级时间',
  `completed_at` datetime NULL COMMENT '完成时间',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_work_order_sla_order` (`tenant_id`,`work_order_id`,`deleted`),
  KEY `idx_crm_work_order_sla_due` (`tenant_id`,`status`,`resolution_due_time`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 工单 SLA 实例';

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='crm_work_order_sla' AND column_name='escalation_due_time');
SET @s = IF(@c=0, 'ALTER TABLE crm_work_order_sla ADD COLUMN escalation_due_time datetime NULL COMMENT ''自动升级时间'' AFTER response_due_time', 'SELECT 1');
PREPARE x FROM @s; EXECUTE x; DEALLOCATE PREPARE x;

INSERT INTO crm_work_order_sla_policy
 (`code`,`name`,`priority`,`response_minutes`,`resolution_minutes`,`escalation_minutes`,`enabled`,`sort`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT defs.code,defs.name,defs.priority,defs.response_minutes,defs.resolution_minutes,defs.escalation_minutes,b'1',defs.sort,'crm-sla',NOW(),'crm-sla',NOW(),b'0',tenant.id
FROM (SELECT 'LOW' code,'低优先级' name,1 priority,480 response_minutes,2880 resolution_minutes,240 escalation_minutes,1 sort
      UNION ALL SELECT 'MEDIUM','中优先级',2,240,1440,120,2
      UNION ALL SELECT 'HIGH','高优先级',3,60,480,60,3) defs
JOIN system_tenant tenant
WHERE NOT EXISTS (SELECT 1 FROM crm_work_order_sla_policy p WHERE p.tenant_id=tenant.id AND p.code=defs.code AND p.deleted=b'0');

INSERT INTO system_menu (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT defs.name,defs.permission,3,defs.sort,page.id,'','','','',0,b'1',b'1',b'1','crm-work-order-governance',NOW(),'crm-work-order-governance',NOW(),b'0'
FROM system_menu page JOIN (
 SELECT '工单签到' name,'crm:work-order:check-in' permission,4 sort
 UNION ALL SELECT '工单 SLA 管理','crm:work-order:sla-admin',5
 UNION ALL SELECT '工单 SLA 操作','crm:work-order:sla-action',6
) defs
WHERE page.path='work-order' AND page.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.permission=defs.permission AND existing.deleted=b'0');

INSERT INTO system_menu_i18n (`menu_id`,`language`,`name`)
SELECT menu.id,lang.language,
 CASE menu.permission WHEN 'crm:work-order:check-in' THEN CASE lang.language WHEN 'zh-CN' THEN '工单签到' WHEN 'en' THEN 'Work Order Check-in' ELSE 'تسجيل حضور التذكرة' END
 WHEN 'crm:work-order:sla-admin' THEN CASE lang.language WHEN 'zh-CN' THEN '工单 SLA 管理' WHEN 'en' THEN 'Work Order SLA Admin' ELSE 'إدارة SLA للتذاكر' END
 ELSE CASE lang.language WHEN 'zh-CN' THEN '工单 SLA 操作' WHEN 'en' THEN 'Work Order SLA Actions' ELSE 'إجراءات SLA للتذاكر' END END
FROM system_menu menu JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') lang
WHERE menu.permission IN ('crm:work-order:check-in','crm:work-order:sla-admin','crm:work-order:sla-action') AND menu.deleted=b'0'
ON DUPLICATE KEY UPDATE name=VALUES(name),deleted=b'0';

INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id,menu.id,'crm-work-order-governance',NOW(),'crm-work-order-governance',NOW(),b'0',role.tenant_id
FROM system_role role JOIN system_menu menu ON menu.permission IN
  ('crm:work-order:check-in','crm:work-order:sla-admin','crm:work-order:sla-action')
WHERE role.code='crm_admin' AND role.deleted=b'0' AND menu.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing WHERE existing.role_id=role.id AND existing.menu_id=menu.id
    AND existing.tenant_id=role.tenant_id AND existing.deleted=b'0');
