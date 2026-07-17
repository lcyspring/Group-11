-- OA menu foundation. Idempotent for existing Podman volumes.
-- Business collaboration remains separate from the generic BPM approval center.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE system_menu
SET name = '办公协作', path = 'collaboration', icon = 'ep:office-building',
    updater = 'oa-menu-foundation', update_time = NOW()
WHERE id = 5 AND deleted = b'0';

UPDATE system_menu
SET name = '请假管理', sort = 10,
    updater = 'oa-menu-foundation', update_time = NOW()
WHERE id = 1118 AND parent_id = 5 AND deleted = b'0';

UPDATE system_menu
SET name = '审批中心', icon = 'ep:finished',
    updater = 'oa-menu-foundation', update_time = NOW()
WHERE id = 1200 AND deleted = b'0';

UPDATE system_menu
SET name = '逾期与待办', icon = 'ep:alarm-clock',
    updater = 'oa-menu-foundation', update_time = NOW()
WHERE id = 2701 AND path = 'backlog' AND deleted = b'0';

INSERT INTO system_menu_i18n (`menu_id`, `language`, `name`)
SELECT menu.id, lang.language,
       CASE menu.id
         WHEN 5 THEN CASE lang.language WHEN 'zh-CN' THEN '办公协作' WHEN 'en' THEN 'Office Collaboration' ELSE 'التعاون المكتبي' END
         WHEN 1118 THEN CASE lang.language WHEN 'zh-CN' THEN '请假管理' WHEN 'en' THEN 'Leave Management' ELSE 'إدارة الإجازات' END
         WHEN 1200 THEN CASE lang.language WHEN 'zh-CN' THEN '审批中心' WHEN 'en' THEN 'Approval Center' ELSE 'مركز الموافقات' END
         ELSE CASE lang.language WHEN 'zh-CN' THEN '逾期与待办' WHEN 'en' THEN 'Overdue & Backlog' ELSE 'المتأخرات والمهام' END
       END
FROM system_menu menu
JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') lang
WHERE menu.id IN (5, 1118, 1200, 2701) AND menu.deleted = b'0'
ON DUPLICATE KEY UPDATE name = VALUES(name), deleted = b'0';
