-- CRM 合同产品成交快照（GAP-OPP-002、ADR-003）。脚本可重复执行。
-- 字段保持可空，以兼容历史上引用已物理删除产品的合同产品行；新写入由服务端保证快照完整。
SET @contract_product_name_snapshot_exists = (
    SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'crm_contract_product'
       AND column_name = 'product_name_snapshot'
);
SET @contract_product_name_snapshot_sql = IF(
    @contract_product_name_snapshot_exists = 0,
    'ALTER TABLE crm_contract_product ADD COLUMN product_name_snapshot varchar(100) NULL COMMENT ''成交时产品名称快照'' AFTER product_id',
    'SELECT 1'
);
PREPARE contract_product_name_snapshot_statement FROM @contract_product_name_snapshot_sql;
EXECUTE contract_product_name_snapshot_statement;
DEALLOCATE PREPARE contract_product_name_snapshot_statement;

SET @contract_product_no_snapshot_exists = (
    SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'crm_contract_product'
       AND column_name = 'product_no_snapshot'
);
SET @contract_product_no_snapshot_sql = IF(
    @contract_product_no_snapshot_exists = 0,
    'ALTER TABLE crm_contract_product ADD COLUMN product_no_snapshot varchar(20) NULL COMMENT ''成交时产品编码快照'' AFTER product_name_snapshot',
    'SELECT 1'
);
PREPARE contract_product_no_snapshot_statement FROM @contract_product_no_snapshot_sql;
EXECUTE contract_product_no_snapshot_statement;
DEALLOCATE PREPARE contract_product_no_snapshot_statement;

SET @contract_product_unit_snapshot_exists = (
    SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'crm_contract_product'
       AND column_name = 'product_unit_snapshot'
);
SET @contract_product_unit_snapshot_sql = IF(
    @contract_product_unit_snapshot_exists = 0,
    'ALTER TABLE crm_contract_product ADD COLUMN product_unit_snapshot tinyint NULL COMMENT ''成交时产品单位快照'' AFTER product_no_snapshot',
    'SELECT 1'
);
PREPARE contract_product_unit_snapshot_statement FROM @contract_product_unit_snapshot_sql;
EXECUTE contract_product_unit_snapshot_statement;
DEALLOCATE PREPARE contract_product_unit_snapshot_statement;

-- 仅填空值：首次迁移后即冻结，重复执行不会用后来修改的产品目录覆盖历史快照。
UPDATE crm_contract_product contract_product
JOIN crm_product product ON product.id = contract_product.product_id
SET contract_product.product_name_snapshot = COALESCE(contract_product.product_name_snapshot, product.name),
    contract_product.product_no_snapshot = COALESCE(contract_product.product_no_snapshot, product.no),
    contract_product.product_unit_snapshot = COALESCE(contract_product.product_unit_snapshot, product.unit)
WHERE contract_product.product_name_snapshot IS NULL
   OR contract_product.product_no_snapshot IS NULL
   OR contract_product.product_unit_snapshot IS NULL;
