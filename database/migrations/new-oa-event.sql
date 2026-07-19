CREATE TABLE IF NOT EXISTS `bpm_oa_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '创建人/负责人',
  `title` varchar(200) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `all_day` bit(1) NOT NULL DEFAULT b'0',
  `location` varchar(500) DEFAULT NULL,
  `participant_user_ids` varchar(2000) DEFAULT NULL COMMENT '参与人 JSON 数组',
  `reminder_minutes` int DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0正常 10已取消',
  `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), KEY `idx_owner_time` (`tenant_id`,`user_id`,`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA日程';

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '日程管理', 'bpm:oa-event:menu', 2, 35, parent.id, 'event', 'ep:calendar', 'bpm/oa/event/index', 'OaEvent',
       0, b'1', b'1', b'1', 'oa-event', NOW(), 'oa-event', NOW(), b'0'
FROM system_menu parent WHERE parent.path='collaboration' AND parent.type=1 AND parent.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.permission='bpm:oa-event:menu' AND m.deleted=b'0');
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name,
                         status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT x.name, x.permission, 3, x.sort, m.id, '', '', '', NULL, 0, b'1', b'1', b'1', 'oa-event', NOW(), 'oa-event', NOW(), b'0'
FROM (SELECT '日程查询' name,'bpm:oa-event:query' permission,1 sort UNION ALL
      SELECT '日程创建','bpm:oa-event:create',2 UNION ALL SELECT '日程更新','bpm:oa-event:update',3 UNION ALL SELECT '日程删除','bpm:oa-event:delete',4) x
JOIN system_menu m ON m.permission='bpm:oa-event:menu' AND m.deleted=b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu e WHERE e.permission=x.permission AND e.deleted=b'0');
