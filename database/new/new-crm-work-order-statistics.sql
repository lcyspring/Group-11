-- CRM 客服工单统计菜单与查询权限（GAP-WO-STAT-001）。脚本可重复执行。
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单统计', 'crm:statistics-work-order:query', 2, 5, statistics.id, 'work-order', 'ep:data-analysis',
       'crm/statistics/workorder/index', 'CrmStatisticsWorkOrder', 0, b'1', b'1', b'1',
       '1', NOW(), '1', NOW(), b'0'
  FROM `system_menu` statistics
  JOIN `system_menu` crm ON crm.id = statistics.parent_id
 WHERE statistics.path = 'statistics' AND statistics.type = 1 AND statistics.deleted = b'0'
   AND crm.path = '/crm' AND crm.parent_id = 0 AND crm.deleted = b'0'
   AND NOT EXISTS (
       SELECT 1 FROM `system_menu` existing
        WHERE existing.permission = 'crm:statistics-work-order:query' AND existing.deleted = b'0'
   );

INSERT INTO `system_menu_i18n` (`menu_id`, `language`, `name`)
SELECT menu.id, language.language,
       CASE language.language
           WHEN 'zh-CN' THEN '工单统计'
           WHEN 'en' THEN 'Work Order Analytics'
           ELSE 'تحليلات أوامر العمل'
       END
  FROM `system_menu` menu
  JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') language
 WHERE menu.permission = 'crm:statistics-work-order:query' AND menu.deleted = b'0'
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `deleted` = b'0';
