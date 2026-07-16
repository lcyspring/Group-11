-- CRM 合同到 ERP 销售履约订单：正式 ACL、主数据映射、幂等和状态回传。
-- MySQL 8.0，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @erp_sale_order_external_source_system_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND column_name='external_source_system');
SET @erp_sale_order_external_source_system_sql = IF(@erp_sale_order_external_source_system_exists=0,
  'ALTER TABLE erp_sale_order ADD COLUMN external_source_system varchar(32) NULL COMMENT ''外部来源系统'' AFTER remark', 'SELECT 1');
PREPARE erp_sale_order_external_source_system_stmt FROM @erp_sale_order_external_source_system_sql;
EXECUTE erp_sale_order_external_source_system_stmt;
DEALLOCATE PREPARE erp_sale_order_external_source_system_stmt;

SET @erp_sale_order_external_source_type_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND column_name='external_source_type');
SET @erp_sale_order_external_source_type_sql = IF(@erp_sale_order_external_source_type_exists=0,
  'ALTER TABLE erp_sale_order ADD COLUMN external_source_type varchar(32) NULL COMMENT ''外部来源类型'' AFTER external_source_system', 'SELECT 1');
PREPARE erp_sale_order_external_source_type_stmt FROM @erp_sale_order_external_source_type_sql;
EXECUTE erp_sale_order_external_source_type_stmt;
DEALLOCATE PREPARE erp_sale_order_external_source_type_stmt;

SET @erp_sale_order_external_source_id_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND column_name='external_source_id');
SET @erp_sale_order_external_source_id_sql = IF(@erp_sale_order_external_source_id_exists=0,
  'ALTER TABLE erp_sale_order ADD COLUMN external_source_id bigint NULL COMMENT ''外部来源业务编号'' AFTER external_source_type', 'SELECT 1');
PREPARE erp_sale_order_external_source_id_stmt FROM @erp_sale_order_external_source_id_sql;
EXECUTE erp_sale_order_external_source_id_stmt;
DEALLOCATE PREPARE erp_sale_order_external_source_id_stmt;

SET @erp_sale_order_external_request_id_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND column_name='external_request_id');
SET @erp_sale_order_external_request_id_sql = IF(@erp_sale_order_external_request_id_exists=0,
  'ALTER TABLE erp_sale_order ADD COLUMN external_request_id varchar(96) NULL COMMENT ''外部幂等请求号'' AFTER external_source_id', 'SELECT 1');
PREPARE erp_sale_order_external_request_id_stmt FROM @erp_sale_order_external_request_id_sql;
EXECUTE erp_sale_order_external_request_id_stmt;
DEALLOCATE PREPARE erp_sale_order_external_request_id_stmt;

SET @erp_sale_order_external_request_hash_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND column_name='external_request_hash');
SET @erp_sale_order_external_request_hash_sql = IF(@erp_sale_order_external_request_hash_exists=0,
  'ALTER TABLE erp_sale_order ADD COLUMN external_request_hash char(64) NULL COMMENT ''外部请求快照 SHA-256'' AFTER external_request_id', 'SELECT 1');
PREPARE erp_sale_order_external_request_hash_stmt FROM @erp_sale_order_external_request_hash_sql;
EXECUTE erp_sale_order_external_request_hash_stmt;
DEALLOCATE PREPARE erp_sale_order_external_request_hash_stmt;

SET @erp_sale_order_currency_code_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND column_name='currency_code');
SET @erp_sale_order_currency_code_sql = IF(@erp_sale_order_currency_code_exists=0,
  'ALTER TABLE erp_sale_order ADD COLUMN currency_code char(3) NULL COMMENT ''ERP 订单币种'' AFTER external_request_hash', 'SELECT 1');
PREPARE erp_sale_order_currency_code_stmt FROM @erp_sale_order_currency_code_sql;
EXECUTE erp_sale_order_currency_code_stmt;
DEALLOCATE PREPARE erp_sale_order_currency_code_stmt;

SET @erp_sale_order_source_currency_code_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND column_name='source_currency_code');
SET @erp_sale_order_source_currency_code_sql = IF(@erp_sale_order_source_currency_code_exists=0,
  'ALTER TABLE erp_sale_order ADD COLUMN source_currency_code char(3) NULL COMMENT ''来源合同币种'' AFTER currency_code', 'SELECT 1');
PREPARE erp_sale_order_source_currency_code_stmt FROM @erp_sale_order_source_currency_code_sql;
EXECUTE erp_sale_order_source_currency_code_stmt;
DEALLOCATE PREPARE erp_sale_order_source_currency_code_stmt;

SET @erp_sale_order_source_exchange_rate_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND column_name='source_exchange_rate');
SET @erp_sale_order_source_exchange_rate_sql = IF(@erp_sale_order_source_exchange_rate_exists=0,
  'ALTER TABLE erp_sale_order ADD COLUMN source_exchange_rate decimal(24,6) NULL COMMENT ''来源币种兑订单币种汇率'' AFTER source_currency_code', 'SELECT 1');
PREPARE erp_sale_order_source_exchange_rate_stmt FROM @erp_sale_order_source_exchange_rate_sql;
EXECUTE erp_sale_order_source_exchange_rate_stmt;
DEALLOCATE PREPARE erp_sale_order_source_exchange_rate_stmt;

SET @erp_sale_order_source_gross_amount_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND column_name='source_gross_amount');
SET @erp_sale_order_source_gross_amount_sql = IF(@erp_sale_order_source_gross_amount_exists=0,
  'ALTER TABLE erp_sale_order ADD COLUMN source_gross_amount decimal(24,6) NULL COMMENT ''来源合同含税金额'' AFTER source_exchange_rate', 'SELECT 1');
PREPARE erp_sale_order_source_gross_amount_stmt FROM @erp_sale_order_source_gross_amount_sql;
EXECUTE erp_sale_order_source_gross_amount_stmt;
DEALLOCATE PREPARE erp_sale_order_source_gross_amount_stmt;

SET @erp_sale_order_external_source_index_exists = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND index_name='uk_erp_sale_order_external_source');
SET @erp_sale_order_external_source_index_sql = IF(@erp_sale_order_external_source_index_exists=0,
  'ALTER TABLE erp_sale_order ADD UNIQUE KEY uk_erp_sale_order_external_source (tenant_id,external_source_system,external_source_type,external_source_id)', 'SELECT 1');
PREPARE erp_sale_order_external_source_index_stmt FROM @erp_sale_order_external_source_index_sql;
EXECUTE erp_sale_order_external_source_index_stmt;
DEALLOCATE PREPARE erp_sale_order_external_source_index_stmt;

SET @erp_sale_order_external_request_index_exists = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='erp_sale_order' AND index_name='uk_erp_sale_order_external_request');
SET @erp_sale_order_external_request_index_sql = IF(@erp_sale_order_external_request_index_exists=0,
  'ALTER TABLE erp_sale_order ADD UNIQUE KEY uk_erp_sale_order_external_request (tenant_id,external_request_id)', 'SELECT 1');
PREPARE erp_sale_order_external_request_index_stmt FROM @erp_sale_order_external_request_index_sql;
EXECUTE erp_sale_order_external_request_index_stmt;
DEALLOCATE PREPARE erp_sale_order_external_request_index_stmt;

CREATE TABLE IF NOT EXISTS `crm_erp_customer_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `crm_customer_id` bigint NOT NULL COMMENT 'CRM 客户编号',
  `erp_customer_id` bigint NOT NULL COMMENT 'ERP 客户编号',
  `remark` varchar(500) NULL COMMENT '映射说明',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_erp_customer_crm` (`tenant_id`,`crm_customer_id`),
  UNIQUE KEY `uk_crm_erp_customer_erp` (`tenant_id`,`erp_customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 到 ERP 客户一对一映射';

CREATE TABLE IF NOT EXISTS `crm_erp_product_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `crm_product_id` bigint NOT NULL COMMENT 'CRM 产品编号',
  `erp_product_id` bigint NOT NULL COMMENT 'ERP 产品编号',
  `remark` varchar(500) NULL COMMENT '映射说明',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_erp_product_crm` (`tenant_id`,`crm_product_id`),
  UNIQUE KEY `uk_crm_erp_product_erp` (`tenant_id`,`erp_product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 到 ERP 产品一对一映射';

CREATE TABLE IF NOT EXISTS `crm_contract_fulfillment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `contract_id` bigint NOT NULL COMMENT 'CRM 合同编号',
  `contract_version` int NOT NULL COMMENT '签署合同版本',
  `request_id` varchar(96) NOT NULL COMMENT '稳定幂等请求号',
  `request_hash` char(64) NOT NULL COMMENT '请求快照 SHA-256',
  `request_snapshot` json NOT NULL COMMENT '不可变 ERP 创建请求',
  `status` tinyint NOT NULL COMMENT '0创建中、10已创建、20失败',
  `erp_order_id` bigint NULL COMMENT 'ERP 销售订单编号',
  `erp_order_no` varchar(128) NULL COMMENT 'ERP 销售订单号',
  `erp_order_status` tinyint NULL COMMENT 'ERP 审核状态',
  `source_currency_code` char(3) NOT NULL COMMENT '合同币种',
  `erp_currency_code` char(3) NOT NULL COMMENT 'ERP 订单币种',
  `exchange_rate` decimal(24,6) NOT NULL COMMENT '兑 ERP 币种汇率',
  `source_gross_amount` decimal(24,6) NOT NULL COMMENT '合同含税金额',
  `erp_total_amount` decimal(24,6) NULL COMMENT 'ERP 订单金额',
  `total_count` decimal(24,6) NULL COMMENT '订单总数量',
  `out_count` decimal(24,6) NULL COMMENT '累计出库数量',
  `return_count` decimal(24,6) NULL COMMENT '累计退货数量',
  `attempt_count` int NOT NULL DEFAULT 1 COMMENT '创建尝试次数',
  `last_error_code` varchar(64) NULL COMMENT '最近错误码',
  `last_error_message` varchar(1000) NULL COMMENT '最近错误',
  `last_attempt_time` datetime NOT NULL COMMENT '最近尝试时间',
  `completed_time` datetime NULL COMMENT '首次创建完成时间',
  `last_sync_time` datetime NULL COMMENT '最近状态同步时间',
  `creator` varchar(64) NULL DEFAULT '', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NULL DEFAULT '', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_contract_fulfillment_contract` (`tenant_id`,`contract_id`),
  UNIQUE KEY `uk_crm_contract_fulfillment_request` (`tenant_id`,`request_id`),
  UNIQUE KEY `uk_crm_contract_fulfillment_erp` (`tenant_id`,`erp_order_id`),
  KEY `idx_crm_contract_fulfillment_status` (`tenant_id`,`status`,`last_attempt_time`,`id`),
  CONSTRAINT `chk_crm_contract_fulfillment_status` CHECK (`status` IN (0,10,20)),
  CONSTRAINT `chk_crm_contract_fulfillment_attempt` CHECK (`attempt_count` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 合同到 ERP 履约记录';

INSERT INTO system_menu
 (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT 'ERP 履约映射','',2,95,root.id,'erp-fulfillment-mapping','ep:connection','crm/erpFulfillment/mapping/index','CrmErpFulfillmentMapping',0,b'1',b'1',b'1','crm-erp-fulfillment',NOW(),'crm-erp-fulfillment',NOW(),b'0'
FROM system_menu root WHERE root.path='/crm' AND root.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.path='erp-fulfillment-mapping' AND existing.deleted=b'0');

INSERT INTO system_menu
 (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT defs.name,defs.permission,3,defs.sort,page.id,'','','','',0,b'1',b'1',b'1','crm-erp-fulfillment',NOW(),'crm-erp-fulfillment',NOW(),b'0'
FROM system_menu page JOIN (
 SELECT '履约映射查询' name,'crm:erp-mapping:query' permission,1 sort
 UNION ALL SELECT '履约映射维护','crm:erp-mapping:update',2
 UNION ALL SELECT '履约映射删除','crm:erp-mapping:delete',3
) defs
WHERE page.path='erp-fulfillment-mapping' AND page.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.permission=defs.permission AND existing.deleted=b'0');

INSERT INTO system_menu
 (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT defs.name,defs.permission,3,defs.sort,contract_menu.id,'','','','',0,b'1',b'1',b'1','crm-erp-fulfillment',NOW(),'crm-erp-fulfillment',NOW(),b'0'
FROM system_menu contract_menu JOIN (
 SELECT '履约状态查询' name,'crm:erp-fulfillment:query' permission,20 sort
 UNION ALL SELECT '创建或重试履约单','crm:erp-fulfillment:create',21
 UNION ALL SELECT '刷新履约状态','crm:erp-fulfillment:refresh',22
) defs
WHERE contract_menu.path='contract' AND contract_menu.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_menu existing WHERE existing.permission=defs.permission AND existing.deleted=b'0');

INSERT INTO system_menu_i18n (`menu_id`,`language`,`name`)
SELECT menu.id,lang.language,
 CASE WHEN menu.path='erp-fulfillment-mapping' THEN CASE lang.language WHEN 'zh-CN' THEN 'ERP 履约映射' WHEN 'en' THEN 'ERP Fulfillment Mapping' ELSE 'ربط تنفيذ ERP' END
      WHEN menu.permission='crm:erp-mapping:query' THEN CASE lang.language WHEN 'zh-CN' THEN '履约映射查询' WHEN 'en' THEN 'Query Fulfillment Mapping' ELSE 'استعلام ربط التنفيذ' END
      WHEN menu.permission='crm:erp-mapping:update' THEN CASE lang.language WHEN 'zh-CN' THEN '履约映射维护' WHEN 'en' THEN 'Maintain Fulfillment Mapping' ELSE 'صيانة ربط التنفيذ' END
      WHEN menu.permission='crm:erp-mapping:delete' THEN CASE lang.language WHEN 'zh-CN' THEN '履约映射删除' WHEN 'en' THEN 'Delete Fulfillment Mapping' ELSE 'حذف ربط التنفيذ' END
      WHEN menu.permission='crm:erp-fulfillment:query' THEN CASE lang.language WHEN 'zh-CN' THEN '履约状态查询' WHEN 'en' THEN 'Query Fulfillment Status' ELSE 'استعلام حالة التنفيذ' END
      WHEN menu.permission='crm:erp-fulfillment:create' THEN CASE lang.language WHEN 'zh-CN' THEN '创建或重试履约单' WHEN 'en' THEN 'Create or Retry Fulfillment' ELSE 'إنشاء أو إعادة محاولة التنفيذ' END
      ELSE CASE lang.language WHEN 'zh-CN' THEN '刷新履约状态' WHEN 'en' THEN 'Refresh Fulfillment Status' ELSE 'تحديث حالة التنفيذ' END END
FROM system_menu menu JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') lang
WHERE (menu.path='erp-fulfillment-mapping' OR menu.permission LIKE 'crm:erp-mapping:%' OR menu.permission LIKE 'crm:erp-fulfillment:%')
  AND menu.deleted=b'0'
ON DUPLICATE KEY UPDATE name=VALUES(name),deleted=b'0';

-- 合同读者可查看履约状态；合同编辑者可显式创建和刷新。
INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT DISTINCT source.role_id,target.id,'crm-erp-fulfillment',NOW(),'crm-erp-fulfillment',NOW(),b'0',source.tenant_id
FROM system_role_menu source JOIN system_menu source_menu ON source_menu.id=source.menu_id AND source_menu.permission='crm:contract:query'
JOIN system_menu target ON target.permission='crm:erp-fulfillment:query' AND target.deleted=b'0'
WHERE source.deleted=b'0' AND source_menu.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing WHERE existing.role_id=source.role_id AND existing.menu_id=target.id AND existing.tenant_id=source.tenant_id AND existing.deleted=b'0');

INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT DISTINCT source.role_id,target.id,'crm-erp-fulfillment',NOW(),'crm-erp-fulfillment',NOW(),b'0',source.tenant_id
FROM system_role_menu source JOIN system_menu source_menu ON source_menu.id=source.menu_id AND source_menu.permission='crm:contract:update'
JOIN system_menu target ON target.permission IN ('crm:erp-fulfillment:create','crm:erp-fulfillment:refresh') AND target.deleted=b'0'
WHERE source.deleted=b'0' AND source_menu.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing WHERE existing.role_id=source.role_id AND existing.menu_id=target.id AND existing.tenant_id=source.tenant_id AND existing.deleted=b'0');

-- 主数据映射属于 CRM 管理职责，不按 Controller 中的角色名硬编码，仅在迁移时给默认 CRM 管理员模板授权。
INSERT INTO system_role_menu (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id,menu.id,'crm-erp-fulfillment',NOW(),'crm-erp-fulfillment',NOW(),b'0',role.tenant_id
FROM system_role role JOIN system_menu menu ON (menu.path='erp-fulfillment-mapping' OR menu.permission LIKE 'crm:erp-mapping:%')
WHERE role.code='crm_admin' AND role.deleted=b'0' AND menu.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing WHERE existing.role_id=role.id AND existing.menu_id=menu.id AND existing.tenant_id=role.tenant_id AND existing.deleted=b'0');
