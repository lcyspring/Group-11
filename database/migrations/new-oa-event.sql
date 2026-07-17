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
