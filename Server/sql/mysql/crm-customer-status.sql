-- ============================================================
-- 客户状态相关数据库迁移脚本
-- ============================================================

-- 1. 为 crm_customer 表添加 status 字段
ALTER TABLE `crm_customer` 
ADD COLUMN `status` TINYINT(4) NULL DEFAULT 1 COMMENT '客户状态 1-潜在客户 2-意向客户 3-谈判中 4-已成交 5-流失客户' AFTER `level`;

-- 2. 创建 crm_customer_config 表（客户配置表）
CREATE TABLE IF NOT EXISTS `crm_customer_config` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_type` VARCHAR(50) NOT NULL COMMENT '配置类型',
  `config_value` INT(11) NULL COMMENT '配置值',
  `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
  `color` VARCHAR(50) NULL COMMENT '颜色',
  `sort` INT(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_config_type` (`config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户配置表';

-- 3. 插入客户状态字典数据
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES (1500, 1, '潜在客户', '1', 'crm_customer_status', 0, 'info', '', '', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES (1501, 2, '意向客户', '2', 'crm_customer_status', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES (1502, 3, '谈判中', '3', 'crm_customer_status', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES (1503, 4, '已成交', '4', 'crm_customer_status', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES (1504, 5, '流失客户', '5', 'crm_customer_status', 0, 'danger', '', '', '1', NOW(), '1', NOW(), b'0');