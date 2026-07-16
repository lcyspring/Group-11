-- =============================================
-- 营销模块 DDL - 发送记录关联表
-- =============================================

DROP TABLE IF EXISTS `marketing_send_record`;
CREATE TABLE `marketing_send_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `campaign_id` bigint NOT NULL COMMENT '营销活动编号',
  `channel` varchar(8) NOT NULL COMMENT '渠道：SMS / MAIL',
  `system_log_id` bigint NOT NULL COMMENT 'system_sms_log.id 或 system_mail_log.id',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_campaign_id`(`campaign_id`) USING BTREE,
  INDEX `idx_channel`(`channel`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '营销发送记录关联表';
