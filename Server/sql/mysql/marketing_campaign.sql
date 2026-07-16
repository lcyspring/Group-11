-- =============================================
-- 营销模块 DDL - 营销活动表
-- 适配数据库: MySQL 5.7+
-- =============================================

-- ----------------------------
-- Table structure for marketing_campaign
-- ----------------------------
DROP TABLE IF EXISTS `marketing_campaign`;
CREATE TABLE `marketing_campaign`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `type` tinyint NOT NULL COMMENT '活动类型：1=短信营销，2=邮件营销',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '活动状态：0=草稿，1=待审核，2=已审核，3=进行中，4=已结束，5=已终止',
  `template_id` bigint NOT NULL COMMENT '关联模板ID（短信模板或邮件模板）',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1=全部会员，2=指定会员，3=按标签筛选',
  `target_tags` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标标签（JSON数组）',
  `target_user_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '指定用户ID列表（JSON数组）',
  `send_time` datetime NULL DEFAULT NULL COMMENT '计划发送时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '活动结束时间',
  `bpm_process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例ID（群发审核）',
  `sent_count` int NOT NULL DEFAULT 0 COMMENT '已发送数量',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功数量',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数量',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '营销活动';
