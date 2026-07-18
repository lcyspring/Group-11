-- Preserve recipient names at broadcast creation so historical delivery results do not depend on mutable master data.
SET @recipient_customer_name_column = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_marketing_broadcast_recipient' AND column_name='customer_name');
SET @recipient_customer_name_sql = IF(@recipient_customer_name_column=0,
  'ALTER TABLE crm_marketing_broadcast_recipient ADD COLUMN customer_name varchar(200) NULL COMMENT ''发送时客户名称快照'' AFTER customer_id',
  'SELECT 1');
PREPARE recipient_customer_name_stmt FROM @recipient_customer_name_sql;
EXECUTE recipient_customer_name_stmt;
DEALLOCATE PREPARE recipient_customer_name_stmt;

SET @recipient_contact_name_column = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_marketing_broadcast_recipient' AND column_name='contact_name');
SET @recipient_contact_name_sql = IF(@recipient_contact_name_column=0,
  'ALTER TABLE crm_marketing_broadcast_recipient ADD COLUMN contact_name varchar(200) NULL COMMENT ''发送时联系人名称快照'' AFTER contact_id',
  'SELECT 1');
PREPARE recipient_contact_name_stmt FROM @recipient_contact_name_sql;
EXECUTE recipient_contact_name_stmt;
DEALLOCATE PREPARE recipient_contact_name_stmt;

UPDATE crm_marketing_broadcast_recipient recipient
LEFT JOIN crm_customer customer
  ON customer.id=recipient.customer_id AND customer.tenant_id=recipient.tenant_id AND customer.deleted=b'0'
LEFT JOIN crm_contact contact
  ON contact.id=recipient.contact_id AND contact.tenant_id=recipient.tenant_id AND contact.deleted=b'0'
SET recipient.customer_name=COALESCE(NULLIF(recipient.customer_name,''),customer.name),
    recipient.contact_name=COALESCE(NULLIF(recipient.contact_name,''),contact.name)
WHERE recipient.deleted=b'0'
  AND ((recipient.customer_name IS NULL OR recipient.customer_name='')
    OR (recipient.contact_id IS NOT NULL AND (recipient.contact_name IS NULL OR recipient.contact_name='')));
