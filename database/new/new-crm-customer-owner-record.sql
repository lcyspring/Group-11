CREATE TABLE IF NOT EXISTS `crm_customer_owner_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `customer_id` bigint NOT NULL COMMENT '客户编号',
  `owner_user_id` bigint NOT NULL COMMENT '放入前或领取后的负责人编号',
  `type` tinyint NOT NULL COMMENT '类型：1 进入公海，2 领取或分配',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_tenant_owner_type_time` (`tenant_id`, `owner_user_id`, `type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户公海归属变更记录表';
