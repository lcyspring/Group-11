-- CRM 客户导入预检、字段映射和幂等确认任务。可重复执行。

CREATE TABLE IF NOT EXISTS `crm_customer_import_preview` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预检任务编号',
  `file_name` varchar(255) NOT NULL COMMENT '原文件名',
  `field_mapping` text NOT NULL COMMENT '表头到系统字段映射 JSON',
  `update_support` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否允许更新同名客户',
  `owner_user_id` bigint NULL COMMENT '新建客户负责人；为空进入公海',
  `status` tinyint NOT NULL DEFAULT 10 COMMENT '10已预检、20已确认、30已过期',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '数据行数',
  `create_count` int NOT NULL DEFAULT 0 COMMENT '预计创建数',
  `update_count` int NOT NULL DEFAULT 0 COMMENT '预计更新数',
  `failure_count` int NOT NULL DEFAULT 0 COMMENT '预检失败数',
  `rows_snapshot` mediumtext NOT NULL COMMENT '规范化行和预检结论 JSON',
  `result_snapshot` mediumtext NULL COMMENT '确认导入结果 JSON',
  `expires_at` datetime NOT NULL COMMENT '确认截止时间',
  `confirmed_at` datetime NULL COMMENT '确认时间',
  `creator` varchar(64) NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_customer_import_preview_owner` (`tenant_id`,`creator`,`status`,`expires_at`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户导入预检任务';
