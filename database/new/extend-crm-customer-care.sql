-- CRM customer-care maintenance and post-deal follow-up compatibility migration.
-- Safe to execute repeatedly on MySQL 8.

ALTER TABLE `crm_customer_care_plan`
  MODIFY COLUMN `event_month_day` varchar(5) NULL COMMENT '节假日日期 MM-dd';

SET @care_follow_up_days_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'crm_customer_care_plan'
    AND column_name = 'follow_up_days'
);
SET @care_follow_up_days_sql = IF(
  @care_follow_up_days_exists = 0,
  'ALTER TABLE crm_customer_care_plan ADD COLUMN follow_up_days int NULL COMMENT ''成交后第 N 天触达，仅定期回访使用'' AFTER event_month_day',
  'SELECT 1'
);
PREPARE care_follow_up_days_statement FROM @care_follow_up_days_sql;
EXECUTE care_follow_up_days_statement;
DEALLOCATE PREPARE care_follow_up_days_statement;

INSERT INTO system_menu (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '关怀删除','crm:customer-care:delete',3,3,page.id,'','','','',0,b'1',b'1',b'1','crm-care',NOW(),'crm-care',NOW(),b'0'
FROM system_menu page
WHERE page.path='customer-care' AND page.deleted=b'0'
  AND NOT EXISTS (
    SELECT 1 FROM system_menu existing
    WHERE existing.permission='crm:customer-care:delete' AND existing.deleted=b'0'
  );

INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id,menu.id,'crm-care',NOW(),'crm-care',NOW(),b'0',role.tenant_id
FROM system_role role
JOIN system_menu menu ON menu.permission='crm:customer-care:delete'
WHERE role.code='crm_admin' AND role.deleted=b'0' AND menu.deleted=b'0'
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu existing
    WHERE existing.role_id=role.id AND existing.menu_id=menu.id
      AND existing.tenant_id=role.tenant_id AND existing.deleted=b'0'
  );
