SET @customer_birthday_column = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='crm_customer' AND column_name='birthday');
SET @customer_birthday_sql = IF(@customer_birthday_column=0,
 'ALTER TABLE crm_customer ADD COLUMN birthday date NULL COMMENT ''客户自身生日'' AFTER email', 'SELECT 1');
PREPARE customer_birthday_stmt FROM @customer_birthday_sql;
EXECUTE customer_birthday_stmt;
DEALLOCATE PREPARE customer_birthday_stmt;
