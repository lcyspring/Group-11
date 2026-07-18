-- Deterministic associated CRM finance, marketing and non-workflow OA facts.
-- Approval facts are marked as imported history and never create fake Flowable rows.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @demo_batch = _utf8mb4'__BATCH__' COLLATE utf8mb4_unicode_ci;
SET @demo_tenant = __TENANT__;
SET @demo_owner = __OWNER__;
SET @demo_start = '__START__ 00:00:00';
SET @demo_end = '__END__ 23:59:59';
SET @demo_span_days = __SPAN__;

DELIMITER $$
DROP PROCEDURE IF EXISTS generate_crm_associated_demo_v2$$
CREATE PROCEDURE generate_crm_associated_demo_v2()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE customer_id BIGINT;
  DECLARE contact_id BIGINT;
  DECLARE business_id BIGINT;
  DECLARE product_id BIGINT;
  DECLARE contract_id BIGINT;
  DECLARE plan_id BIGINT;
  DECLARE receivable_id BIGINT;
  DECLARE invoice_id BIGINT;
  DECLARE reimbursement_id BIGINT;
  DECLARE refund_id BIGINT;
  DECLARE category_id BIGINT;
  DECLARE campaign_id BIGINT;
  DECLARE care_plan_id BIGINT;
  DECLARE product_name VARCHAR(100);
  DECLARE product_no VARCHAR(20);
  DECLARE product_unit INT;
  DECLARE product_category_id BIGINT;
  DECLARE product_version INT;
  DECLARE contract_amount DECIMAL(24,6);
  DECLARE plan_amount DECIMAL(24,6);
  DECLARE receipt_amount DECIMAL(24,6);
  DECLARE event_time DATETIME;
  DECLARE finance_status INT;
  DECLARE record_owner BIGINT;
  DECLARE handler_owner BIGINT;
  DECLARE plans_per_contract INT DEFAULT (__PLANS__ / __CONTRACTS__);

  IF (SELECT COUNT(*) FROM crm_customer WHERE tenant_id=@demo_tenant
      AND name LIKE CONCAT(@demo_batch,'-CUS-%')) <> __CUSTOMERS__ THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Associated demo requires the complete generated customer batch';
  END IF;
  IF EXISTS (SELECT 1 FROM crm_contract WHERE tenant_id=@demo_tenant
             AND name LIKE CONCAT(@demo_batch,'-CONTRACT-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Associated demo batch already exists; explicit cleanup is required';
  END IF;

  CREATE TEMPORARY TABLE demo_contract_ids(
    seq INT PRIMARY KEY,id BIGINT NOT NULL,customer_id BIGINT NOT NULL,total_price DECIMAL(24,6) NOT NULL);
  CREATE TEMPORARY TABLE demo_plan_ids(
    seq INT PRIMARY KEY,id BIGINT NOT NULL,contract_id BIGINT NOT NULL,customer_id BIGINT NOT NULL,price DECIMAL(24,6) NOT NULL);
  CREATE TEMPORARY TABLE demo_receivable_ids(
    seq INT PRIMARY KEY,id BIGINT NOT NULL,contract_id BIGINT NOT NULL,customer_id BIGINT NOT NULL,price DECIMAL(24,6) NOT NULL);
  CREATE TEMPORARY TABLE demo_owner_ids(seq INT PRIMARY KEY,id BIGINT NOT NULL);
  SET i=1;
  WHILE i<=__DEMO_USERS__ DO
    INSERT INTO demo_owner_ids(seq,id)
    SELECT i,id FROM system_users WHERE tenant_id=@demo_tenant AND deleted=b'0'
     AND username=CONCAT('d',MOD(__SEED__,100000000),'user',LPAD(i,2,'0'));
    IF ROW_COUNT()<>1 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Associated demo requires all generated login users';
    END IF;
    SET i=i+1;
  END WHILE;

  INSERT INTO crm_product_category
    (name,parent_id,creator,create_time,updater,update_time,deleted,tenant_id)
    VALUES(CONCAT(@demo_batch,'-CATEGORY-ROOT'),0,CAST(@demo_owner AS CHAR),NOW(),
           CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
  SET category_id=LAST_INSERT_ID();
  SET i=1;
  WHILE i<=5 DO
    INSERT INTO crm_product_category
      (name,parent_id,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-CATEGORY-',LPAD(i,2,'0')),category_id,
       CAST(@demo_owner AS CHAR),NOW(),CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;
  SET i=1;
  WHILE i<=__PRODUCTS__ DO
    SELECT id INTO record_owner FROM demo_owner_ids WHERE seq=MOD(i-1,4)+1;
    SET event_time=DATE_ADD(@demo_start,INTERVAL MOD(__SEED__+i*59,@demo_span_days) DAY);
    SELECT id INTO category_id FROM crm_product_category WHERE tenant_id=@demo_tenant
      AND name=CONCAT(@demo_batch,'-CATEGORY-',LPAD(MOD(i-1,5)+1,2,'0'));
    INSERT INTO crm_product
      (name,no,unit,price,status,category_id,description,owner_user_id,version,
       creator,create_time,updater,update_time,tenant_id,deleted)
      VALUES(CONCAT(@demo_batch,'-PRODUCT-',LPAD(i,4,'0')),
       CONCAT('D2P',MOD(__SEED__,100000),LPAD(i,4,'0')),MOD(i,4)+1,500+MOD(i*__SEED__,9500),0,
       category_id,'固定种子演示产品',record_owner,1,CAST(record_owner AS CHAR),event_time,
       CAST(@demo_owner AS CHAR),event_time,@demo_tenant,b'0');
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(6,LAST_INSERT_ID(),record_owner,1,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i<=__BUSINESSES__ DO
    SELECT id INTO business_id FROM crm_business WHERE tenant_id=@demo_tenant
      AND name=CONCAT(@demo_batch,'-BUS-',LPAD(i,6,'0'));
    SELECT id,price INTO product_id,receipt_amount FROM crm_product WHERE tenant_id=@demo_tenant
      AND name=CONCAT(@demo_batch,'-PRODUCT-',LPAD(MOD(i-1,__PRODUCTS__)+1,4,'0'));
    INSERT INTO crm_business_product
      (business_id,product_id,product_price,business_price,count,total_price,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(business_id,product_id,receipt_amount,receipt_amount,1,receipt_amount,
       CAST(@demo_owner AS CHAR),NOW(),CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i<=__CONTRACTS__ DO
    SELECT id INTO record_owner FROM demo_owner_ids WHERE seq=MOD(i-1,4)+1;
    SET event_time=DATE_ADD(@demo_start,INTERVAL MOD(__SEED__+i*43,GREATEST(@demo_span_days-60,1)) DAY);
    SELECT c.id,b.id,ct.id INTO customer_id,business_id,contact_id
      FROM crm_customer c
      JOIN crm_business b ON b.tenant_id=@demo_tenant
       AND b.name=CONCAT(@demo_batch,'-BUS-',LPAD(MOD(i-1,__BUSINESSES__)+1,6,'0'))
      JOIN crm_contact ct ON ct.customer_id=c.id AND ct.primary_contact=b'1' AND ct.deleted=b'0'
     WHERE c.tenant_id=@demo_tenant
       AND c.name=CONCAT(@demo_batch,'-CUS-',LPAD(MOD(i-1,__CUSTOMERS__)+1,6,'0'))
     LIMIT 1;
    SET contract_amount=10000+MOD(i*__SEED__,90000);
    UPDATE crm_business SET status_id=NULL,end_status=1,
      deal_time=DATE_ADD(event_time,INTERVAL 30 DAY),
      end_time=DATE_ADD(event_time,INTERVAL 45 DAY),
      total_price=contract_amount,update_time=DATE_ADD(event_time,INTERVAL 45 DAY)
      WHERE id=business_id;
    INSERT INTO crm_contract
      (name,no,customer_id,business_id,owner_user_id,process_instance_id,audit_status,
       order_date,start_time,end_time,total_product_price,discount_percent,total_price,
       currency_code,base_currency_code,exchange_rate_to_base,tax_amount,gross_amount,
       base_gross_amount,sign_contact_id,sign_user_id,remark,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-CONTRACT-',LPAD(i,6,'0')),
       CONCAT('D2C-',__SEED__,'-',LPAD(i,6,'0')),customer_id,business_id,record_owner,NULL,20,
       DATE_ADD(event_time,INTERVAL 45 DAY),DATE_ADD(event_time,INTERVAL 45 DAY),
       DATE_ADD(event_time,INTERVAL 410 DAY),
       contract_amount,100,contract_amount,'CNY','CNY',1,
       ROUND(contract_amount*0.13,6),ROUND(contract_amount*1.13,6),ROUND(contract_amount*1.13,6),
       contact_id,record_owner,CONCAT('generated-batch:',@demo_batch,'; imported-approved-history'),
       CAST(@demo_owner AS CHAR),DATE_ADD(event_time,INTERVAL 45 DAY),CAST(@demo_owner AS CHAR),
       DATE_ADD(event_time,INTERVAL 45 DAY),b'0',@demo_tenant);
    SET contract_id=LAST_INSERT_ID();
    INSERT INTO demo_contract_ids VALUES(i,contract_id,customer_id,contract_amount);
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(5,contract_id,record_owner,1,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SELECT id,name,no,unit,category_id,version,price
      INTO product_id,product_name,product_no,product_unit,product_category_id,product_version,receipt_amount
      FROM crm_product WHERE tenant_id=@demo_tenant
       AND name=CONCAT(@demo_batch,'-PRODUCT-',LPAD(MOD(i-1,__PRODUCTS__)+1,4,'0'));
    INSERT INTO crm_contract_product
      (contract_id,product_id,product_name_snapshot,product_no_snapshot,product_unit_snapshot,
       product_category_id_snapshot,product_version_snapshot,product_price,contract_price,count,total_price,
       tax_rate_percent,tax_amount,gross_amount,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(contract_id,product_id,product_name,product_no,product_unit,product_category_id,product_version,
       receipt_amount,contract_amount,1,contract_amount,13,ROUND(contract_amount*0.13,6),
       ROUND(contract_amount*1.13,6),CAST(@demo_owner AS CHAR),NOW(),
       CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i<=__PLANS__ DO
    SELECT id INTO record_owner FROM demo_owner_ids WHERE seq=MOD(i-1,4)+1;
    SELECT id,customer_id,total_price INTO contract_id,customer_id,contract_amount
      FROM demo_contract_ids WHERE seq=MOD(i-1,__CONTRACTS__)+1;
    SET plan_amount=ROUND(contract_amount/plans_per_contract,6);
    SET event_time=CASE MOD(i,4)
      WHEN 0 THEN DATE_SUB(@demo_end,INTERVAL 120 DAY)
      WHEN 1 THEN DATE_SUB(@demo_end,INTERVAL 45 DAY)
      WHEN 2 THEN DATE_ADD(@demo_end,INTERVAL 30 DAY)
      ELSE DATE_ADD(@demo_end,INTERVAL 90 DAY) END;
    INSERT INTO crm_receivable_plan
      (period,customer_id,contract_id,owner_user_id,receivable_id,return_time,return_type,
       price,remind_days,remind_time,remark,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(MOD(i-1,plans_per_contract)+1,customer_id,contract_id,record_owner,NULL,
       event_time,1,plan_amount,7,DATE_SUB(event_time,INTERVAL 7 DAY),
       CONCAT('generated-batch:',@demo_batch,'; contract-installment'),CAST(@demo_owner AS CHAR),
       DATE_SUB(event_time,INTERVAL 60 DAY),CAST(@demo_owner AS CHAR),DATE_SUB(event_time,INTERVAL 60 DAY),b'0',@demo_tenant);
    SET plan_id=LAST_INSERT_ID();
    INSERT INTO demo_plan_ids VALUES(i,plan_id,contract_id,customer_id,plan_amount);
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(8,plan_id,record_owner,1,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i<=__RECEIVABLES__ DO
    SELECT id INTO record_owner FROM demo_owner_ids WHERE seq=MOD(i-1,4)+1;
    SELECT id,contract_id,customer_id,price INTO plan_id,contract_id,customer_id,plan_amount
      FROM demo_plan_ids WHERE seq=i;
    SET finance_status=IF(i<=__REFUNDS__,20,
      CASE MOD(i,5) WHEN 0 THEN 0 WHEN 1 THEN 20 WHEN 2 THEN 20 WHEN 3 THEN 30 ELSE 40 END);
    SELECT return_time INTO event_time FROM crm_receivable_plan WHERE id=plan_id;
    INSERT INTO crm_receivable
      (no,plan_id,customer_id,contract_id,owner_user_id,audit_status,process_instance_id,
       return_time,return_type,price,remark,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-REC-',LPAD(i,6,'0')),plan_id,customer_id,contract_id,record_owner,finance_status,NULL,
       event_time,1,plan_amount,
       CONCAT('generated-batch:',@demo_batch,IF(finance_status=0,'; editable-draft','; imported-audit-history')),
       CAST(@demo_owner AS CHAR),event_time,CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    SET receivable_id=LAST_INSERT_ID();
    UPDATE crm_receivable_plan SET receivable_id=receivable_id,update_time=NOW() WHERE id=plan_id;
    INSERT INTO demo_receivable_ids VALUES(i,receivable_id,contract_id,customer_id,plan_amount);
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(7,receivable_id,record_owner,1,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    INSERT INTO crm_receivable_write_off
      (receivable_id,amount,write_off_time,source_type,reference_no,remark,status,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(receivable_id,ROUND(plan_amount*0.8,6),event_time,3,
       CONCAT(@demo_batch,'-WOFF-',LPAD(i,6,'0')),CONCAT('generated-batch:',@demo_batch),0,
       CAST(@demo_owner AS CHAR),NOW(),CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i<=__REFUNDS__ DO
    SELECT id INTO record_owner FROM demo_owner_ids WHERE seq=MOD(i-1,2)+5;
    SELECT id,contract_id,customer_id,price INTO receivable_id,contract_id,customer_id,receipt_amount
      FROM demo_receivable_ids WHERE seq=i;
    SET finance_status=CASE MOD(i,4) WHEN 0 THEN 0 WHEN 1 THEN 20 WHEN 2 THEN 30 ELSE 40 END;
    SET event_time=DATE_ADD(@demo_start,INTERVAL MOD(__SEED__+i*61,@demo_span_days) DAY);
    INSERT INTO crm_receivable_refund
      (no,receivable_id,customer_id,contract_id,owner_user_id,type,refund_time,amount,reason,
       remark,process_instance_id,audit_status,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-REF-',LPAD(i,6,'0')),receivable_id,customer_id,contract_id,record_owner,
       IF(MOD(i,2)=0,2,1),event_time,ROUND(receipt_amount*0.1,6),
       '固定种子演示退款，已校验不超过原回款可退金额',
       CONCAT('generated-batch:',@demo_batch,IF(finance_status=0,'; editable-draft','; imported-audit-history')),
       NULL,finance_status,CAST(@demo_owner AS CHAR),event_time,CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    SET refund_id=LAST_INSERT_ID();
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(10,refund_id,record_owner,1,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    INSERT INTO crm_receivable_refund_action_record
      (refund_id,action_type,from_status,to_status,operator_user_id,action_time,process_instance_id,
       remark,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(refund_id,CASE finance_status WHEN 20 THEN 4 WHEN 30 THEN 5 WHEN 40 THEN 6 ELSE 1 END,
       NULL,finance_status,record_owner,event_time,NULL,
       IF(finance_status=0,'固定种子草稿创建','导入历史审批结果'),CAST(@demo_owner AS CHAR),event_time,
       CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i<=__INVOICES__ DO
    SELECT id INTO record_owner FROM demo_owner_ids WHERE seq=MOD(i-1,4)+1;
    SELECT id INTO handler_owner FROM demo_owner_ids WHERE seq=MOD(i-1,2)+5;
    SET event_time=DATE_ADD(@demo_start,INTERVAL MOD(__SEED__+i*67,@demo_span_days) DAY);
    SET finance_status=CASE MOD(i,5) WHEN 0 THEN 0 WHEN 1 THEN 10 WHEN 2 THEN 20 WHEN 3 THEN 30 ELSE 40 END;
    SELECT id,customer_id,total_price INTO contract_id,customer_id,contract_amount
      FROM demo_contract_ids WHERE seq=i;
    INSERT INTO crm_invoice
      (no,contract_id,customer_id,owner_user_id,handler_user_id,direction,original_invoice_id,status,
       type,amount,red_amount,invoice_no,invoice_date,title,tax_no,email,content,remark,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-INV-',LPAD(i,6,'0')),contract_id,customer_id,record_owner,handler_owner,
       1,NULL,finance_status,IF(MOD(i,2)=0,2,1),ROUND(contract_amount*0.5,6),
       CASE finance_status WHEN 20 THEN ROUND(contract_amount*0.2,6)
            WHEN 30 THEN ROUND(contract_amount*0.5,6) ELSE 0 END,
       IF(finance_status=0,NULL,CONCAT('D2F',__SEED__,LPAD(i,6,'0'))),
       IF(finance_status=0,NULL,event_time),CONCAT(@demo_batch,' 演示客户'),
       CONCAT('TAX',__SEED__,LPAD(i,6,'0')),CONCAT('invoice.',__SEED__,'.',i,'@example.invalid'),
       '软件与技术服务',CONCAT('generated-batch:',@demo_batch),CAST(@demo_owner AS CHAR),event_time,
       CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    SET invoice_id=LAST_INSERT_ID();
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(9,invoice_id,record_owner,1,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(9,invoice_id,handler_owner,3,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    INSERT INTO crm_invoice_action_record
      (invoice_id,action_type,from_status,to_status,operator_user_id,action_time,remark,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(invoice_id,1,NULL,0,handler_owner,event_time,'固定种子发票草稿创建',CAST(handler_owner AS CHAR),event_time,
       CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    IF finance_status<>0 THEN
      INSERT INTO crm_invoice_action_record
        (invoice_id,action_type,from_status,to_status,operator_user_id,action_time,remark,
         creator,create_time,updater,update_time,deleted,tenant_id)
        VALUES(invoice_id,3,0,10,handler_owner,event_time,'固定种子演示开票',CAST(handler_owner AS CHAR),event_time,
         CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    END IF;
    IF finance_status IN (20,30) THEN
      INSERT INTO crm_invoice_action_record
        (invoice_id,action_type,from_status,to_status,operator_user_id,action_time,remark,
         creator,create_time,updater,update_time,deleted,tenant_id)
        VALUES(invoice_id,5,10,finance_status,handler_owner,event_time,'固定种子演示红冲',CAST(handler_owner AS CHAR),event_time,
         CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    ELSEIF finance_status=40 THEN
      INSERT INTO crm_invoice_action_record
        (invoice_id,action_type,from_status,to_status,operator_user_id,action_time,remark,
         creator,create_time,updater,update_time,deleted,tenant_id)
        VALUES(invoice_id,4,10,40,handler_owner,event_time,'固定种子演示作废',CAST(handler_owner AS CHAR),event_time,
         CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    END IF;
    SET i=i+1;
  END WHILE;

  INSERT INTO crm_expense_category
    (code,name,status,sort,description,creator,create_time,updater,update_time,deleted,tenant_id)
    VALUES(CONCAT(@demo_batch,'-EXP-01'),CONCAT(@demo_batch,'-CATEGORY-差旅'),0,1,
     '固定种子演示费用分类',CAST(@demo_owner AS CHAR),NOW(),CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
  SET category_id=LAST_INSERT_ID();
  SET i=1;
  WHILE i<=__REIMBURSEMENTS__ DO
    SELECT id INTO record_owner FROM demo_owner_ids WHERE seq=MOD(i-1,2)+5;
    SET event_time=DATE_ADD(@demo_start,INTERVAL MOD(__SEED__+i*71,@demo_span_days) DAY);
    SET finance_status=CASE MOD(i,4) WHEN 0 THEN 0 WHEN 1 THEN 20 WHEN 2 THEN 30 ELSE 40 END;
    SELECT id,customer_id INTO contract_id,customer_id FROM demo_contract_ids WHERE seq=i;
    SET receipt_amount=200+MOD(i*__SEED__,4800);
    INSERT INTO crm_reimbursement
      (no,applicant_user_id,owner_user_id,department_id,customer_id,contract_id,trip_id,currency,
       total_amount,expense_start_date,expense_end_date,reason,remark,process_instance_id,audit_status,version,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-RMB-',LPAD(i,6,'0')),record_owner,record_owner,NULL,customer_id,contract_id,NULL,
       'CNY',receipt_amount,DATE_SUB(DATE(event_time),INTERVAL MOD(i,5)+1 DAY),DATE(event_time),
       '固定种子演示项目差旅报销',
       CONCAT('generated-batch:',@demo_batch,IF(finance_status=0,'; editable-draft','; imported-audit-history')),
       NULL,finance_status,0,CAST(@demo_owner AS CHAR),event_time,CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    SET reimbursement_id=LAST_INSERT_ID();
    INSERT INTO crm_reimbursement_item
      (reimbursement_id,category_id,occurred_date,amount,description,invoice_no,attachment_urls,sort,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(reimbursement_id,category_id,DATE(event_time),receipt_amount,'客户项目差旅费用',
       CONCAT('D2R-',__SEED__,'-',LPAD(i,6,'0')),NULL,1,CAST(@demo_owner AS CHAR),NOW(),
       CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(11,reimbursement_id,record_owner,1,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    INSERT INTO crm_reimbursement_action_record
      (reimbursement_id,action_type,from_status,to_status,amount_snapshot,operator_user_id,action_time,
       process_instance_id,remark,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(reimbursement_id,CASE finance_status WHEN 20 THEN 4 WHEN 30 THEN 5 WHEN 40 THEN 6 ELSE 1 END,
       NULL,finance_status,receipt_amount,record_owner,event_time,NULL,
       IF(finance_status=0,'固定种子草稿创建','导入历史审批结果'),CAST(@demo_owner AS CHAR),event_time,
       CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i<=__CAMPAIGNS__ DO
    SELECT id INTO record_owner FROM demo_owner_ids WHERE seq=8;
    SET event_time=DATE_ADD(@demo_start,INTERVAL MOD(__SEED__+i*73,GREATEST(@demo_span_days-30,1)) DAY);
    INSERT INTO crm_marketing_campaign
      (code,name,status,owner_user_id,start_time,end_time,budget_amount,actual_cost_amount,
       target_lead_count,target_customer_count,description,summary,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-MKT-',LPAD(i,4,'0')),CONCAT(@demo_batch,'-营销活动-',LPAD(i,4,'0')),
       CASE MOD(i,5) WHEN 0 THEN 10 WHEN 1 THEN 20 WHEN 2 THEN 30 WHEN 3 THEN 40 ELSE 50 END,record_owner,
       event_time,DATE_ADD(event_time,INTERVAL 30 DAY),
       10000+MOD(i*137,90000),5000+MOD(i*97,40000),100,50,
       CONCAT('generated-batch:',@demo_batch),'固定种子活动总结',CAST(@demo_owner AS CHAR),event_time,
       CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    SET campaign_id=LAST_INSERT_ID();
    SELECT id INTO customer_id FROM crm_customer WHERE tenant_id=@demo_tenant
      AND name=CONCAT(@demo_batch,'-CUS-',LPAD(MOD(i-1,__CUSTOMERS__)+1,6,'0'));
    SELECT id INTO business_id FROM crm_business WHERE tenant_id=@demo_tenant
      AND name=CONCAT(@demo_batch,'-BUS-',LPAD(MOD(i-1,__BUSINESSES__)+1,6,'0'));
    INSERT INTO crm_marketing_campaign_relation
      (campaign_id,biz_type,biz_id,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(campaign_id,2,customer_id,CAST(@demo_owner AS CHAR),NOW(),CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant),
            (campaign_id,3,business_id,CAST(@demo_owner AS CHAR),NOW(),CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i<=3 DO
    INSERT INTO crm_customer_care_plan
      (code,name,rule_type,event_month_day,follow_up_days,channel,sms_template_code,mail_template_code,
       enabled,target_scope,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-CARE-',i),CONCAT(@demo_batch,'-客户关怀-',i),i,
       IF(i=2,'10-01',NULL),IF(i=3,30,NULL),IF(i=2,2,1),
       IF(i<>2,'crm_demo_care_sms',NULL),IF(i=2,'crm_demo_care_mail',NULL),b'0','READABLE_CUSTOMERS',
       CAST(@demo_owner AS CHAR),NOW(),CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;
  SET i=1;
  WHILE i<=__CARE_RECORDS__ DO
    SET event_time=DATE_ADD(@demo_start,INTERVAL MOD(__SEED__+i*79,@demo_span_days) DAY);
    SET finance_status=CASE MOD(i,5) WHEN 0 THEN 10 WHEN 1 THEN 20 WHEN 2 THEN 30 WHEN 3 THEN 40 ELSE 50 END;
    SELECT p.id,c.id,ct.id INTO care_plan_id,customer_id,contact_id
      FROM crm_customer_care_plan p
      JOIN crm_customer c ON c.tenant_id=@demo_tenant
       AND c.name=CONCAT(@demo_batch,'-CUS-',LPAD(MOD(i-1,__CUSTOMERS__)+1,6,'0'))
      JOIN crm_contact ct ON ct.customer_id=c.id AND ct.primary_contact=b'1' AND ct.deleted=b'0'
     WHERE p.tenant_id=@demo_tenant AND p.code=CONCAT(@demo_batch,'-CARE-',MOD(i-1,3)+1);
    INSERT INTO crm_customer_care_record
      (plan_id,customer_id,contact_id,event_date,channel,status,failure_reason,provider_log_id,sent_at,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(care_plan_id,customer_id,contact_id,
       DATE(event_time),MOD(i,2)+1,finance_status,
       IF(finance_status=30,'演示渠道发送失败',NULL),NULL,IF(finance_status IN (20,50),event_time,NULL),
       CAST(@demo_owner AS CHAR),event_time,CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i<=__OA_EVENTS__ DO
    SELECT id INTO record_owner FROM demo_owner_ids WHERE seq=MOD(i-1,__DEMO_USERS__)+1;
    SET event_time=DATE_ADD(@demo_start,INTERVAL MOD(__SEED__+i*83,@demo_span_days) DAY);
    INSERT INTO bpm_oa_event
      (user_id,title,description,start_time,end_time,all_day,location,participant_user_ids,
       reminder_minutes,status,reminder_status,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(record_owner,CONCAT(@demo_batch,'-EVENT-',LPAD(i,6,'0')),
       '固定种子客户拜访日程',event_time,DATE_ADD(event_time,INTERVAL 1 HOUR),b'0','客户现场',
       CAST(record_owner AS CHAR),30,IF(MOD(i,10)=0,10,0),0,CAST(record_owner AS CHAR),event_time,
       CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i<=__OA_TASKS__ DO
    SELECT id INTO record_owner FROM demo_owner_ids WHERE seq=MOD(i-1,__DEMO_USERS__)+1;
    SET event_time=DATE_ADD(@demo_start,INTERVAL MOD(__SEED__+i*89,@demo_span_days) DAY);
    SELECT id INTO customer_id FROM crm_customer WHERE tenant_id=@demo_tenant
      AND name=CONCAT(@demo_batch,'-CUS-',LPAD(MOD(i-1,__CUSTOMERS__)+1,6,'0'));
    INSERT INTO bpm_oa_task
      (title,description,creator_user_id,assignee_user_id,participant_user_ids,priority,status,due_time,
       reminder_minutes,reminder_status,completed_time,business_type,business_id,result,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-TASK-',LPAD(i,6,'0')),'跟进关联演示客户',record_owner,record_owner,
       CAST(record_owner AS CHAR),MOD(i,3),MOD(i,3),
       DATE_ADD(event_time,INTERVAL 14 DAY),60,0,
       IF(MOD(i,3)=2,DATE_ADD(event_time,INTERVAL 7 DAY),NULL),'CRM_CUSTOMER',customer_id,
       IF(MOD(i,3)=2,'已完成客户跟进',NULL),CAST(record_owner AS CHAR),event_time,
       CAST(@demo_owner AS CHAR),event_time,b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  DROP TEMPORARY TABLE demo_receivable_ids;
  DROP TEMPORARY TABLE demo_plan_ids;
  DROP TEMPORARY TABLE demo_contract_ids;
  DROP TEMPORARY TABLE demo_owner_ids;
END$$
CALL generate_crm_associated_demo_v2()$$
DROP PROCEDURE generate_crm_associated_demo_v2$$
DELIMITER ;
