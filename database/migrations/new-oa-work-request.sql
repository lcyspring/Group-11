CREATE TABLE IF NOT EXISTS `bpm_oa_work_request` (
 `id` bigint NOT NULL AUTO_INCREMENT, `user_id` bigint NOT NULL, `title` varchar(200) NOT NULL,
 `content` varchar(10000) NOT NULL, `urgency` tinyint NOT NULL DEFAULT 1, `status` tinyint NOT NULL DEFAULT 1,
 `process_instance_id` varchar(64) DEFAULT NULL, `approved_time` datetime DEFAULT NULL,
 `creator` varchar(64) DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0, PRIMARY KEY (`id`), KEY `idx_request_user` (`tenant_id`,`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA请示';
INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
SELECT '请示审批','bpm:oa-work-request:menu',2,37,parent.id,'work-request','ep:document','bpm/oa/work-request/index','OaWorkRequest',0,b'1',b'1',b'1','oa-work-request',NOW(),'oa-work-request',NOW(),b'0'
FROM system_menu parent WHERE parent.path='collaboration' AND parent.type=1 AND parent.deleted=b'0' AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.permission='bpm:oa-work-request:menu' AND m.deleted=b'0');
INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
SELECT x.name,x.permission,3,x.sort,m.id,'','','',NULL,0,b'1',b'1',b'1','oa-work-request',NOW(),'oa-work-request',NOW(),b'0' FROM (SELECT '请示查询' name,'bpm:oa-work-request:query' permission,1 sort UNION ALL SELECT '发起请示','bpm:oa-work-request:create',2) x JOIN system_menu m ON m.permission='bpm:oa-work-request:menu' AND m.deleted=b'0' WHERE NOT EXISTS (SELECT 1 FROM system_menu e WHERE e.permission=x.permission AND e.deleted=b'0');
