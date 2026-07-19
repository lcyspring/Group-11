-- CRM 工单筛选/勾选导出权限。MySQL 8.0，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- `work-order` 同时被历史工单统计页面使用。清理由旧版迁移误挂到非客服工单页面的权限节点。
DELETE i18n FROM system_menu_i18n i18n
JOIN system_menu menu ON menu.id=i18n.menu_id
JOIN system_menu parent ON parent.id=menu.parent_id
WHERE menu.permission='crm:work-order:export'
  AND COALESCE(parent.component,'')<>'crm/workorder/index';

DELETE role_menu FROM system_role_menu role_menu
JOIN system_menu menu ON menu.id=role_menu.menu_id
JOIN system_menu parent ON parent.id=menu.parent_id
WHERE menu.permission='crm:work-order:export'
  AND COALESCE(parent.component,'')<>'crm/workorder/index';

DELETE menu FROM system_menu menu
JOIN system_menu parent ON parent.id=menu.parent_id
WHERE menu.permission='crm:work-order:export'
  AND COALESCE(parent.component,'')<>'crm/workorder/index';

INSERT INTO system_menu
 (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '工单导出','crm:work-order:export',3,9,page.id,'','','','',0,b'1',b'1',b'1',
       'crm-work-order-export',NOW(),'crm-work-order-export',NOW(),b'0'
FROM system_menu page
WHERE page.component='crm/workorder/index' AND page.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu existing
                  WHERE existing.permission='crm:work-order:export' AND existing.deleted=b'0');

INSERT INTO system_menu_i18n (`menu_id`,`language`,`name`)
SELECT menu.id,lang.language,
       CASE lang.language WHEN 'zh-CN' THEN '工单导出'
                          WHEN 'en' THEN 'Export Work Orders'
                          ELSE 'تصدير أوامر العمل' END
FROM system_menu menu
JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') lang
WHERE menu.permission='crm:work-order:export' AND menu.deleted=b'0'
ON DUPLICATE KEY UPDATE name=VALUES(name),deleted=b'0';

INSERT INTO system_role_menu
 (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id,menu.id,'crm-work-order-export',NOW(),'crm-work-order-export',NOW(),b'0',role.tenant_id
FROM system_role role
JOIN system_menu menu ON menu.permission='crm:work-order:export' AND menu.deleted=b'0'
WHERE role.code='crm_admin' AND role.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
                  WHERE existing.role_id=role.id AND existing.menu_id=menu.id
                    AND existing.tenant_id=role.tenant_id AND existing.deleted=b'0');
