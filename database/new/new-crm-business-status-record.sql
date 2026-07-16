CREATE TABLE `crm_business_status_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `business_id` bigint NOT NULL COMMENT '商机编号',
    `old_status_type_id` bigint NULL COMMENT '原状态类型编号',
    `old_status_id` bigint NULL COMMENT '原状态编号',
    `new_status_type_id` bigint NULL COMMENT '新状态类型编号',
    `new_status_id` bigint NULL COMMENT '新状态编号',
    `old_end_status` int NULL COMMENT '原结束状态',
    `new_end_status` int NULL COMMENT '新结束状态',
    `operator_id` bigint NOT NULL COMMENT '操作人编号',
    `remark` varchar(500) NULL COMMENT '流转备注',
    `create_time` datetime NOT NULL COMMENT '流转时间',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商机状态流转记录表';