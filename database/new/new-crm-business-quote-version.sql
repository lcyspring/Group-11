-- CRM business quotation versions, immutable price snapshots and explicit currency/tax policy.
-- Idempotent for MySQL 8.0.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @crm_product_version_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_product' AND column_name='version'
);
SET @crm_product_version_sql = IF(@crm_product_version_exists=0,
  'ALTER TABLE crm_product ADD COLUMN version int NOT NULL DEFAULT 1 COMMENT ''产品主数据版本'' AFTER owner_user_id',
  'SELECT 1');
PREPARE crm_product_version_stmt FROM @crm_product_version_sql;
EXECUTE crm_product_version_stmt;
DEALLOCATE PREPARE crm_product_version_stmt;
UPDATE crm_product SET version=1 WHERE version IS NULL OR version < 1;

SET @crm_business_product_tax_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_business_product' AND column_name='tax_rate_percent'
);
SET @crm_business_product_tax_sql = IF(@crm_business_product_tax_exists=0,
  'ALTER TABLE crm_business_product ADD COLUMN tax_rate_percent decimal(10,6) NOT NULL DEFAULT 0 COMMENT ''报价税率百分比'' AFTER total_price',
  'SELECT 1');
PREPARE crm_business_product_tax_stmt FROM @crm_business_product_tax_sql;
EXECUTE crm_business_product_tax_stmt;
DEALLOCATE PREPARE crm_business_product_tax_stmt;

SET @crm_contract_source_quote_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_contract' AND column_name='source_quote_id');
SET @crm_contract_source_quote_sql = IF(@crm_contract_source_quote_exists=0,
  'ALTER TABLE crm_contract ADD COLUMN source_quote_id bigint NULL COMMENT ''锁定报价来源'' AFTER source_business_id',
  'SELECT 1');
PREPARE crm_contract_source_quote_stmt FROM @crm_contract_source_quote_sql;
EXECUTE crm_contract_source_quote_stmt;
DEALLOCATE PREPARE crm_contract_source_quote_stmt;

-- The legacy contract amount column is decimal(10,2). A converted quote may carry
-- fractional quantities, tax and configured six-decimal precision, so retaining
-- the old scale would silently truncate the immutable source quote snapshot.
SET @crm_contract_total_price_needs_precision = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_contract' AND column_name='total_price'
    AND (numeric_precision<>24 OR numeric_scale<>6));
SET @crm_contract_total_price_precision_sql = IF(@crm_contract_total_price_needs_precision>0,
  'ALTER TABLE crm_contract MODIFY COLUMN total_price decimal(24,6) NULL DEFAULT NULL COMMENT ''合同总金额''',
  'SELECT 1');
PREPARE crm_contract_total_price_precision_stmt FROM @crm_contract_total_price_precision_sql;
EXECUTE crm_contract_total_price_precision_stmt;
DEALLOCATE PREPARE crm_contract_total_price_precision_stmt;

SET @crm_contract_currency_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_contract' AND column_name='currency_code');
SET @crm_contract_currency_sql = IF(@crm_contract_currency_exists=0,
  'ALTER TABLE crm_contract ADD COLUMN currency_code char(3) NOT NULL DEFAULT ''CNY'' COMMENT ''合同币种'' AFTER total_price, ADD COLUMN base_currency_code char(3) NOT NULL DEFAULT ''CNY'' COMMENT ''本位币'' AFTER currency_code, ADD COLUMN exchange_rate_to_base decimal(24,6) NOT NULL DEFAULT 1 COMMENT ''兑本位币汇率'' AFTER base_currency_code, ADD COLUMN tax_amount decimal(24,6) NOT NULL DEFAULT 0 COMMENT ''税额'' AFTER exchange_rate_to_base, ADD COLUMN gross_amount decimal(24,6) NOT NULL DEFAULT 0 COMMENT ''含税金额'' AFTER tax_amount, ADD COLUMN base_gross_amount decimal(24,6) NOT NULL DEFAULT 0 COMMENT ''本位币含税金额'' AFTER gross_amount',
  'SELECT 1');
PREPARE crm_contract_currency_stmt FROM @crm_contract_currency_sql;
EXECUTE crm_contract_currency_stmt;
DEALLOCATE PREPARE crm_contract_currency_stmt;
UPDATE crm_contract SET currency_code=COALESCE(currency_code,'CNY'),
  base_currency_code=COALESCE(base_currency_code,'CNY'), exchange_rate_to_base=COALESCE(exchange_rate_to_base,1),
  tax_amount=COALESCE(tax_amount,0), gross_amount=IF(gross_amount=0,COALESCE(total_price,0),gross_amount),
  base_gross_amount=IF(base_gross_amount=0,COALESCE(total_price,0),base_gross_amount)
WHERE deleted=b'0';

SET @crm_contract_product_quote_fields_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_contract_product' AND column_name='product_version_snapshot');
SET @crm_contract_product_quote_fields_sql = IF(@crm_contract_product_quote_fields_exists=0,
  'ALTER TABLE crm_contract_product ADD COLUMN product_category_id_snapshot bigint NULL COMMENT ''产品分类快照'' AFTER product_unit_snapshot, ADD COLUMN product_version_snapshot int NULL COMMENT ''产品版本快照'' AFTER product_category_id_snapshot, ADD COLUMN tax_rate_percent decimal(10,6) NOT NULL DEFAULT 0 COMMENT ''税率快照'' AFTER total_price, ADD COLUMN tax_amount decimal(24,6) NOT NULL DEFAULT 0 COMMENT ''税额'' AFTER tax_rate_percent, ADD COLUMN gross_amount decimal(24,6) NOT NULL DEFAULT 0 COMMENT ''含税金额'' AFTER tax_amount',
  'SELECT 1');
PREPARE crm_contract_product_quote_fields_stmt FROM @crm_contract_product_quote_fields_sql;
EXECUTE crm_contract_product_quote_fields_stmt;
DEALLOCATE PREPARE crm_contract_product_quote_fields_stmt;
UPDATE crm_contract_product contract_product
LEFT JOIN crm_product product ON product.id=contract_product.product_id AND product.tenant_id=contract_product.tenant_id
SET contract_product.product_category_id_snapshot=COALESCE(contract_product.product_category_id_snapshot,product.category_id),
    contract_product.product_version_snapshot=COALESCE(contract_product.product_version_snapshot,product.version,1),
    contract_product.tax_rate_percent=COALESCE(contract_product.tax_rate_percent,0),
    contract_product.tax_amount=COALESCE(contract_product.tax_amount,0),
    contract_product.gross_amount=IF(contract_product.gross_amount=0,contract_product.total_price,contract_product.gross_amount)
WHERE contract_product.deleted=b'0';

CREATE TABLE IF NOT EXISTS `crm_business_quote` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `business_id` bigint NOT NULL COMMENT '商机编号',
  `version_no` int NOT NULL COMMENT '报价版本号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0草稿、10锁定、20已被新版本替代、30随商机终止',
  `source_quote_id` bigint NULL COMMENT '重开来源报价编号',
  `currency_code` char(3) NOT NULL COMMENT '报价币种 ISO-4217',
  `base_currency_code` char(3) NOT NULL COMMENT '本位币 ISO-4217',
  `exchange_rate_to_base` decimal(24,6) NOT NULL COMMENT '锁定时兑本位币汇率',
  `discount_percent` decimal(10,6) NOT NULL DEFAULT 0 COMMENT '整单折扣百分比',
  `subtotal` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '折扣前净额',
  `discount_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '折扣金额',
  `net_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '折扣后未税金额',
  `tax_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '税额',
  `gross_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '含税金额',
  `base_gross_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '本位币含税金额',
  `locked_by` bigint NULL COMMENT '锁定人',
  `locked_at` datetime NULL COMMENT '锁定时间',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观版本',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_business_quote_version` (`tenant_id`,`business_id`,`version_no`),
  KEY `idx_crm_business_quote_current` (`tenant_id`,`business_id`,`version_no`,`status`,`id`),
  CONSTRAINT `chk_crm_business_quote_status` CHECK (`status` IN (0,10,20,30)),
  CONSTRAINT `chk_crm_business_quote_version` CHECK (`version_no` > 0),
  CONSTRAINT `chk_crm_business_quote_currency` CHECK (CHAR_LENGTH(`currency_code`)=3 AND CHAR_LENGTH(`base_currency_code`)=3),
  CONSTRAINT `chk_crm_business_quote_rate` CHECK (`exchange_rate_to_base` > 0),
  CONSTRAINT `chk_crm_business_quote_discount` CHECK (`discount_percent` >= 0 AND `discount_percent` <= 100),
  CONSTRAINT `chk_crm_business_quote_amounts` CHECK (`subtotal` >= 0 AND `discount_amount` >= 0 AND `net_amount` >= 0 AND `tax_amount` >= 0 AND `gross_amount` >= 0 AND `base_gross_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 商机报价版本';

SET @crm_quote_status_check_exists = (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE() AND table_name='crm_business_quote'
    AND constraint_name='chk_crm_business_quote_status' AND constraint_type='CHECK');
SET @crm_quote_status_drop_sql = IF(@crm_quote_status_check_exists=1,
  'ALTER TABLE crm_business_quote DROP CHECK chk_crm_business_quote_status', 'SELECT 1');
PREPARE crm_quote_status_drop_stmt FROM @crm_quote_status_drop_sql;
EXECUTE crm_quote_status_drop_stmt;
DEALLOCATE PREPARE crm_quote_status_drop_stmt;
ALTER TABLE crm_business_quote ADD CONSTRAINT chk_crm_business_quote_status
  CHECK (`status` IN (0,10,20,30));

CREATE TABLE IF NOT EXISTS `crm_business_quote_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `quote_id` bigint NOT NULL COMMENT '报价编号',
  `product_id` bigint NOT NULL COMMENT 'CRM 产品编号',
  `product_name_snapshot` varchar(100) NOT NULL COMMENT '产品名称快照',
  `product_no_snapshot` varchar(20) NOT NULL COMMENT '产品/SKU 编码快照',
  `product_unit_snapshot` tinyint NULL COMMENT '产品单位快照',
  `product_category_id_snapshot` bigint NULL COMMENT '产品分类快照',
  `product_version_snapshot` int NOT NULL COMMENT '产品主数据版本快照',
  `list_price` decimal(24,6) NOT NULL COMMENT '目录价快照',
  `business_price` decimal(24,6) NOT NULL COMMENT '报价单价',
  `count` decimal(24,6) NOT NULL COMMENT '数量',
  `tax_rate_percent` decimal(10,6) NOT NULL DEFAULT 0 COMMENT '税率百分比快照',
  `line_subtotal` decimal(24,6) NOT NULL COMMENT '行折扣前金额',
  `line_discount_amount` decimal(24,6) NOT NULL COMMENT '行分摊折扣',
  `net_amount` decimal(24,6) NOT NULL COMMENT '行未税金额',
  `tax_amount` decimal(24,6) NOT NULL COMMENT '行税额',
  `gross_amount` decimal(24,6) NOT NULL COMMENT '行含税金额',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_business_quote_item_quote` (`tenant_id`,`quote_id`,`id`),
  KEY `idx_crm_business_quote_item_product` (`tenant_id`,`product_id`,`product_version_snapshot`,`id`),
  CONSTRAINT `chk_crm_business_quote_item_values` CHECK (`product_version_snapshot` > 0 AND `list_price` >= 0 AND `business_price` >= 0 AND `count` > 0 AND `tax_rate_percent` >= 0 AND `tax_rate_percent` <= 100 AND `line_subtotal` >= 0 AND `line_discount_amount` >= 0 AND `net_amount` >= 0 AND `tax_amount` >= 0 AND `gross_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 报价不可变产品价格快照';

CREATE TABLE IF NOT EXISTS `crm_business_quote_action_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `quote_id` bigint NOT NULL COMMENT '报价编号',
  `action_type` tinyint NOT NULL COMMENT '动作：1创建、2更新、3锁定、4重开、5终止',
  `from_status` tinyint NULL COMMENT '原状态',
  `to_status` tinyint NOT NULL COMMENT '目标状态',
  `operator_user_id` bigint NULL COMMENT '操作人',
  `remark` varchar(500) NOT NULL COMMENT '动作说明',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_business_quote_action` (`tenant_id`,`quote_id`,`create_time`,`id`),
  CONSTRAINT `chk_crm_business_quote_action` CHECK (`action_type` IN (1,2,3,4,5) AND `to_status` IN (0,10,20,30))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 报价不可变动作轨迹';

SET @crm_quote_action_check_exists = (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE() AND table_name='crm_business_quote_action_record'
    AND constraint_name='chk_crm_business_quote_action' AND constraint_type='CHECK');
SET @crm_quote_action_drop_sql = IF(@crm_quote_action_check_exists=1,
  'ALTER TABLE crm_business_quote_action_record DROP CHECK chk_crm_business_quote_action', 'SELECT 1');
PREPARE crm_quote_action_drop_stmt FROM @crm_quote_action_drop_sql;
EXECUTE crm_quote_action_drop_stmt;
DEALLOCATE PREPARE crm_quote_action_drop_stmt;
ALTER TABLE crm_business_quote_action_record ADD CONSTRAINT chk_crm_business_quote_action
  CHECK (`action_type` IN (1,2,3,4,5) AND `to_status` IN (0,10,20,30));

-- Historical business products are the only provable source. Preserve old totals with zero tax and CNY rate 1.
INSERT INTO crm_business_quote
  (business_id,version_no,status,source_quote_id,currency_code,base_currency_code,exchange_rate_to_base,
   discount_percent,subtotal,discount_amount,net_amount,tax_amount,gross_amount,base_gross_amount,
   locked_by,locked_at,version,creator,create_time,updater,update_time,deleted,tenant_id)
SELECT business.id,1,CASE WHEN business.end_status=1 THEN 10
                          WHEN business.end_status IN (2,3) THEN 30 ELSE 0 END,NULL,'CNY','CNY',1,
       COALESCE(business.discount_percent,0),COALESCE(business.total_product_price,0),
       GREATEST(COALESCE(business.total_product_price,0)-COALESCE(business.total_price,0),0),
       COALESCE(business.total_price,0),0,COALESCE(business.total_price,0),COALESCE(business.total_price,0),
       IF(business.end_status=1,CAST(NULLIF(business.updater,'') AS UNSIGNED),NULL),
       IF(business.end_status=1,business.update_time,NULL),0,
       'business-quote-migration',business.create_time,'business-quote-migration',NOW(),b'0',business.tenant_id
FROM crm_business business
WHERE business.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM crm_business_quote quote
                  WHERE quote.tenant_id=business.tenant_id AND quote.business_id=business.id AND quote.version_no=1);

INSERT INTO crm_business_quote_item
  (quote_id,product_id,product_name_snapshot,product_no_snapshot,product_unit_snapshot,
   product_category_id_snapshot,product_version_snapshot,list_price,business_price,count,tax_rate_percent,
   line_subtotal,line_discount_amount,net_amount,tax_amount,gross_amount,
   creator,create_time,updater,update_time,deleted,tenant_id)
SELECT quote.id,bp.product_id,COALESCE(product.name,CONCAT('历史产品#',bp.product_id)),
       COALESCE(product.no,CONCAT('LEGACY-',bp.product_id)),product.unit,product.category_id,
       COALESCE(product.version,1),bp.product_price,bp.business_price,bp.count,COALESCE(bp.tax_rate_percent,0),
       bp.total_price,ROUND(bp.total_price*COALESCE(business.discount_percent,0)/100,6),
       ROUND(bp.total_price*(1-COALESCE(business.discount_percent,0)/100),6),0,
       ROUND(bp.total_price*(1-COALESCE(business.discount_percent,0)/100),6),
       'business-quote-migration',bp.create_time,'business-quote-migration',NOW(),b'0',bp.tenant_id
FROM crm_business_quote quote
JOIN crm_business business ON business.id=quote.business_id AND business.tenant_id=quote.tenant_id
JOIN crm_business_product bp ON bp.business_id=business.id AND bp.tenant_id=business.tenant_id AND bp.deleted=b'0'
LEFT JOIN crm_product product ON product.id=bp.product_id AND product.tenant_id=bp.tenant_id AND product.deleted=b'0'
WHERE quote.version_no=1 AND quote.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM crm_business_quote_item item
                  WHERE item.tenant_id=quote.tenant_id AND item.quote_id=quote.id AND item.deleted=b'0');

INSERT INTO crm_business_quote_action_record
  (quote_id,action_type,from_status,to_status,operator_user_id,remark,
   creator,create_time,updater,update_time,deleted,tenant_id)
SELECT quote.id,CASE WHEN quote.status=10 THEN 3 WHEN quote.status=30 THEN 5 ELSE 1 END,
       NULL,quote.status,quote.locked_by,
       CASE WHEN quote.status=10 THEN '历史赢单报价迁移并锁定'
            WHEN quote.status=30 THEN '历史输单或无效报价迁移并终止'
            ELSE '历史商机报价迁移为草稿' END,
       'business-quote-migration',quote.create_time,'business-quote-migration',NOW(),b'0',quote.tenant_id
FROM crm_business_quote quote
WHERE quote.version_no=1 AND quote.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM crm_business_quote_action_record action
                  WHERE action.tenant_id=quote.tenant_id AND action.quote_id=quote.id AND action.deleted=b'0');

UPDATE crm_contract contract
JOIN crm_business_quote quote ON quote.business_id=contract.source_business_id
  AND quote.tenant_id=contract.tenant_id AND quote.status=10 AND quote.deleted=b'0'
SET contract.source_quote_id=COALESCE(contract.source_quote_id,quote.id),
    contract.currency_code=quote.currency_code,
    contract.base_currency_code=quote.base_currency_code,
    contract.exchange_rate_to_base=quote.exchange_rate_to_base,
    contract.tax_amount=quote.tax_amount,
    contract.gross_amount=quote.gross_amount,
    contract.base_gross_amount=quote.base_gross_amount
WHERE contract.source_business_id IS NOT NULL AND contract.deleted=b'0';

SET @crm_contract_source_quote_index_exists = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='crm_contract' AND index_name='uk_crm_contract_source_quote');
SET @crm_contract_source_quote_index_sql = IF(@crm_contract_source_quote_index_exists=0,
  'ALTER TABLE crm_contract ADD UNIQUE KEY uk_crm_contract_source_quote (tenant_id,source_quote_id)',
  'SELECT 1');
PREPARE crm_contract_source_quote_index_stmt FROM @crm_contract_source_quote_index_sql;
EXECUTE crm_contract_source_quote_index_stmt;
DEALLOCATE PREPARE crm_contract_source_quote_index_stmt;

INSERT INTO system_menu
  (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,
   `keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT permission_name,permission_code,3,permission_sort,business_menu.id,'','','','',0,b'1',b'1',b'1',
       'business-quote-migration',NOW(),'business-quote-migration',NOW(),b'0'
FROM system_menu business_menu
JOIN (
  SELECT '锁定商机报价' permission_name,'crm:business:quote:lock' permission_code,7 permission_sort
  UNION ALL SELECT '重开商机报价','crm:business:quote:reopen',8
) permission_defs
WHERE business_menu.path='business' AND business_menu.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu existing
                  WHERE existing.permission=permission_defs.permission_code AND existing.deleted=b'0');

INSERT INTO system_role_menu
  (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT DISTINCT source.role_id,target.id,'business-quote-migration',NOW(),
       'business-quote-migration',NOW(),b'0',source.tenant_id
FROM system_role_menu source
JOIN system_menu source_menu ON source_menu.id=source.menu_id
  AND source_menu.permission='crm:business:update' AND source_menu.deleted=b'0'
JOIN system_menu target ON target.permission IN ('crm:business:quote:lock','crm:business:quote:reopen')
  AND target.deleted=b'0'
WHERE source.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
                  WHERE existing.role_id=source.role_id AND existing.menu_id=target.id
                    AND existing.tenant_id=source.tenant_id AND existing.deleted=b'0');
