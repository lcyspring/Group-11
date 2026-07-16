SET @primary_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_contact'
      AND column_name = 'primary_contact'
);
SET @primary_column_sql = IF(
    @primary_column_exists = 0,
    'ALTER TABLE crm_contact ADD COLUMN primary_contact bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否首联系人'' AFTER master',
    'SELECT 1'
);
PREPARE primary_column_statement FROM @primary_column_sql;
EXECUTE primary_column_statement;
DEALLOCATE PREPARE primary_column_statement;

UPDATE `crm_contact` contact
JOIN (
    SELECT tenant_id,
           customer_id,
           COALESCE(MIN(CASE WHEN primary_contact = b'1' THEN id END), MIN(id)) AS primary_contact_id
    FROM `crm_contact`
    WHERE deleted = b'0'
      AND customer_id IS NOT NULL
    GROUP BY tenant_id, customer_id
) selected
  ON selected.tenant_id = contact.tenant_id
 AND selected.customer_id = contact.customer_id
SET contact.primary_contact = IF(contact.id = selected.primary_contact_id, b'1', b'0')
WHERE contact.deleted = b'0';

SET @primary_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'crm_contact'
      AND index_name = 'idx_crm_contact_customer_primary'
);
SET @primary_index_sql = IF(
    @primary_index_exists = 0,
    'CREATE INDEX idx_crm_contact_customer_primary ON crm_contact (tenant_id, customer_id, primary_contact, deleted)',
    'SELECT 1'
);
PREPARE primary_index_statement FROM @primary_index_sql;
EXECUTE primary_index_statement;
DEALLOCATE PREPARE primary_index_statement;
