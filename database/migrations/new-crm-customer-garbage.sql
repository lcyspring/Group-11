-- CRM customer garbage quarantine, administrator permissions and navigation. Idempotent for MySQL 8.0.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `crm_customer_owner_record`
  MODIFY COLUMN `owner_user_id` bigint NULL COMMENT '兼容公海统计的负责人；无负责人池状态事件为空',
  MODIFY COLUMN `type` tinyint NOT NULL COMMENT '类型：1进入公海、2领取或分配、3初始分配、4转移、5转入垃圾池、6恢复公海、7永久删除';

SET @garbage_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'crm_customer'
      AND index_name = 'idx_crm_customer_garbage_state'
);
SET @garbage_index_sql = IF(@garbage_index_exists = 0,
    'ALTER TABLE crm_customer ADD INDEX idx_crm_customer_garbage_state (tenant_id,pool_status,garbage_time,id)',
    'SELECT 1');
PREPARE garbage_index_statement FROM @garbage_index_sql;
EXECUTE garbage_index_statement;
DEALLOCATE PREPARE garbage_index_statement;

INSERT INTO `system_menu`
  (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,
   `keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '客户垃圾池','',2,31,crm.id,'customer/garbage','ep:delete-filled',
       'crm/customer/garbage/index','CrmCustomerGarbage',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM `system_menu` crm
WHERE crm.`path`='/crm' AND crm.`parent_id`=0 AND crm.`deleted`=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` existing
                  WHERE existing.`path`='customer/garbage' AND existing.`parent_id`=crm.id
                    AND existing.`deleted`=b'0');

INSERT INTO `system_menu`
  (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,
   `keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT permission_name, permission_code, 3, permission_sort, page.id, '', '', '', '',
       0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM `system_menu` page
JOIN (
    SELECT '垃圾客户查询' permission_name, 'crm:customer-garbage:query' permission_code, 1 permission_sort
    UNION ALL SELECT '垃圾客户管理', 'crm:customer-garbage:manage', 2
    UNION ALL SELECT '垃圾客户永久删除', 'crm:customer-garbage:delete', 3
) permissions
WHERE page.`path`='customer/garbage' AND page.`deleted`=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` existing
                  WHERE existing.`permission`=permissions.permission_code AND existing.`deleted`=b'0');

INSERT INTO `system_menu_i18n` (`menu_id`,`language`,`name`)
SELECT menu.id, language.language,
       CASE language.language
           WHEN 'zh-CN' THEN '客户垃圾池'
           WHEN 'en' THEN 'Customer Garbage Pool'
           ELSE 'مجموعة العملاء المهملين'
       END
FROM `system_menu` menu
JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') language
WHERE menu.`path`='customer/garbage' AND menu.`deleted`=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `deleted`=b'0';

-- Garbage operations are intentionally granted only to the explicitly governed CRM administrator role.
INSERT INTO `system_role_menu`
  (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id, menu.id, 'pool-policy-migration', NOW(), 'pool-policy-migration', NOW(), b'0', role.tenant_id
FROM `system_role` role
JOIN `system_menu` menu ON (menu.`path`='customer/garbage'
                         OR menu.`permission` IN ('crm:customer-garbage:query',
                                                  'crm:customer-garbage:manage',
                                                  'crm:customer-garbage:delete'))
WHERE role.`code`='crm_admin' AND role.`deleted`=b'0' AND menu.`deleted`=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_role_menu` existing
                  WHERE existing.`role_id`=role.id AND existing.`menu_id`=menu.id
                    AND existing.`tenant_id`=role.tenant_id AND existing.`deleted`=b'0');
