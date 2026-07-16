-- =============================================
-- 营销模块 DDL - 客户关怀配置表
-- 注意：节日场景暂仅支持公历固定日期。
--       农历节日需手动配置公历日期。
-- =============================================

DROP TABLE IF EXISTS `marketing_customer_care_config`;
CREATE TABLE `marketing_customer_care_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `scene` varchar(16) NOT NULL COMMENT '场景：BIRTHDAY / HOLIDAY',
  `name` varchar(64) NOT NULL COMMENT '配置名称',
  `channel` varchar(8) NOT NULL COMMENT '发送渠道：SMS / MAIL',
  `template_code` varchar(63) NOT NULL COMMENT '模板编码',
  `template_params_template` varchar(512) DEFAULT NULL COMMENT '模板参数模板（支持 {nickname} 等变量）',
  `holiday_dates` varchar(512) DEFAULT NULL COMMENT '节日日期列表（JSON数组 ["01-01","10-01"]）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '启用状态：0=启用 1=禁用',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scene`(`scene`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '客户关怀配置表';
