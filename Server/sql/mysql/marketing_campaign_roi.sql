-- =============================================
-- 营销模块 DDL - 营销活动 ROI 数据表
-- =============================================

DROP TABLE IF EXISTS marketing_campaign_roi;
CREATE TABLE marketing_campaign_roi  (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  stat_date date NOT NULL COMMENT '统计日期',
  campaign_id bigint NOT NULL COMMENT '营销活动编号',
  channel varchar(16) NOT NULL DEFAULT 'OTHER' COMMENT '渠道：SMS / MAIL / OTHER',
  cost_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '成本金额',
  revenue_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '收入金额',
  lead_count int NOT NULL DEFAULT 0 COMMENT '线索数量',
  customer_count int NOT NULL DEFAULT 0 COMMENT '客户数量',
  opportunity_count int NOT NULL DEFAULT 0 COMMENT '商机数量',
  deal_count int NOT NULL DEFAULT 0 COMMENT '成交数量',
  remark varchar(256) DEFAULT NULL COMMENT '备注',
  tenant_id bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  creator varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updater varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (id) USING BTREE,
  INDEX idx_campaign_id(campaign_id) USING BTREE,
  INDEX idx_channel(channel) USING BTREE,
  INDEX idx_stat_date(stat_date) USING BTREE,
  INDEX idx_tenant_id(tenant_id) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 COMMENT = '营销活动 ROI 数据表';
