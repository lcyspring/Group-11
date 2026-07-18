-- Generated from an explicit YAML configuration. Do not edit the rendered file.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @demo_batch = _utf8mb4'__BATCH__' COLLATE utf8mb4_unicode_ci;
SET @demo_tenant = __TENANT__;
SET @demo_owner = __OWNER__;
SET @demo_start = '__START__ 00:00:00';
SET @demo_span_days = __SPAN__;

DELIMITER $$
DROP PROCEDURE IF EXISTS generate_crm_demo_v2$$
CREATE PROCEDURE generate_crm_demo_v2()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE customer_id BIGINT;
  DECLARE contact_id BIGINT;
  DECLARE business_id BIGINT;
  IF EXISTS (SELECT 1 FROM crm_customer WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch, '-CUS-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Generated demo batch already exists; explicit cleanup is required';
  END IF;
  CREATE TEMPORARY TABLE demo_customer_ids(seq INT PRIMARY KEY, id BIGINT NOT NULL);

  WHILE i <= __CUSTOMERS__ DO
    INSERT INTO crm_customer
      (name,owner_user_id,owner_time,pool_status,lock_status,deal_status,lifecycle_status,
       lifecycle_status_change_time,lifecycle_lost_reason,mobile,email,birthday,remark,creator,create_time,updater,update_time,deleted,tenant_id)
    VALUES
      (CONCAT(@demo_batch,'-CUS-',LPAD(i,6,'0')),@demo_owner,
       DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),0,b'0',IF(MOD(i,4)=2,b'1',b'0'),
       CASE MOD(i,4) WHEN 0 THEN 10 WHEN 1 THEN 20 WHEN 2 THEN 30 ELSE 40 END,
       DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),
       IF(MOD(i,4)=3,'固定种子演示流失样本',NULL),
       CONCAT('18',LPAD(MOD(__SEED__+i,1000000000),9,'0')),
       CONCAT('demo.',__SEED__,'.',i,'@example.invalid'),DATE_ADD('1970-01-01',INTERVAL MOD(i*131,18000) DAY),
       CONCAT('generated-batch:',@demo_batch),
       CAST(@demo_owner AS CHAR),DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),
       CAST(@demo_owner AS CHAR),DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),b'0',@demo_tenant);
    SET customer_id=LAST_INSERT_ID();
    INSERT INTO demo_customer_ids VALUES(i,customer_id);
    INSERT INTO crm_customer_owner_record
      (customer_id,owner_user_id,previous_owner_user_id,new_owner_user_id,type,source,reason,creator,updater,deleted,tenant_id)
      VALUES(customer_id,@demo_owner,NULL,@demo_owner,1,'DEMO_GENERATOR',@demo_batch,
             CAST(@demo_owner AS CHAR),CAST(@demo_owner AS CHAR),b'0',@demo_tenant);
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(2,customer_id,@demo_owner,1,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    INSERT INTO crm_contact
      (name,customer_id,owner_user_id,mobile,email,sex,birthday,master,primary_contact,remark,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-CONTACT-',LPAD(i,6,'0')),customer_id,CAST(@demo_owner AS CHAR),
       CONCAT('17',LPAD(MOD(__SEED__+i,1000000000),9,'0')),
       CONCAT('contact.',__SEED__,'.',i,'@example.invalid'),MOD(i,2),DATE_ADD('1980-01-01',INTERVAL MOD(i*97,12000) DAY),
       b'1',b'1',CONCAT('generated-batch:',@demo_batch),CAST(@demo_owner AS CHAR),NOW(),CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET contact_id=LAST_INSERT_ID();
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(3,contact_id,@demo_owner,1,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i <= __CLUES__ DO
    INSERT INTO crm_clue
      (name,follow_up_status,owner_user_id,owner_time,pool_status,pool_entry_time,
       pool_previous_owner_user_id,pool_reason,pool_reason_detail,pool_cycle_count,
       transform_status,customer_id,mobile,email,level,source,remark,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-CLUE-',LPAD(i,6,'0')),b'0',
       IF(MOD(i,5)=0,NULL,@demo_owner),
       IF(MOD(i,5)=0,NULL,DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY)),
       IF(MOD(i,5)=0,1,0),
       IF(MOD(i,5)=0,DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),NULL),
       NULL,IF(MOD(i,5)=0,'CREATE_UNASSIGNED',NULL),
       IF(MOD(i,5)=0,'固定种子公共线索样本',NULL),IF(MOD(i,5)=0,1,0),b'0',NULL,
       CONCAT('16',LPAD(MOD(__SEED__+i,1000000000),9,'0')),
       CONCAT('clue.',__SEED__,'.',i,'@example.invalid'),MOD(i,5)+1,MOD(i,7)+1,
       CONCAT('generated-batch:',@demo_batch),CAST(@demo_owner AS CHAR),NOW(),
       CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET business_id=LAST_INSERT_ID();
    INSERT INTO crm_clue_owner_record
      (clue_id,previous_owner_user_id,new_owner_user_id,type,source,reason,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(business_id,NULL,IF(MOD(i,5)=0,NULL,@demo_owner),
       IF(MOD(i,5)=0,1,3),IF(MOD(i,5)=0,'CREATE_UNASSIGNED','INITIAL_ASSIGN'),
       CONCAT('generated-batch:',@demo_batch),CAST(@demo_owner AS CHAR),NOW(),
       CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    IF MOD(i,5)<>0 THEN
      INSERT INTO crm_permission
        (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
        VALUES(1,business_id,@demo_owner,1,CAST(@demo_owner AS CHAR),NOW(),
               CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    END IF;
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i <= __BUSINESSES__ DO
    SELECT id INTO customer_id FROM demo_customer_ids WHERE seq=MOD(i-1,__CUSTOMERS__)+1;
    INSERT INTO crm_business
      (name,customer_id,owner_user_id,end_status,deal_time,total_product_price,discount_percent,total_price,
       remark,creator,updater,create_time,update_time,deleted,tenant_id)
      VALUES(CONCAT(@demo_batch,'-BUS-',LPAD(i,6,'0')),customer_id,@demo_owner,
       CASE MOD(i,4) WHEN 0 THEN NULL WHEN 1 THEN 1 WHEN 2 THEN 2 ELSE 3 END,
       IF(MOD(i,4)=1,DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),NULL),
       1000+MOD(i*137,500000),95,ROUND((1000+MOD(i*137,500000))*0.95,2),
       CONCAT('generated-batch:',@demo_batch),CAST(@demo_owner AS CHAR),CAST(@demo_owner AS CHAR),
       DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),NOW(),b'0',@demo_tenant);
    SET business_id=LAST_INSERT_ID();
    INSERT INTO crm_permission
      (biz_type,biz_id,user_id,level,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(4,business_id,@demo_owner,1,CAST(@demo_owner AS CHAR),NOW(),
             CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i <= __FOLLOW_UPS__ DO
    SELECT id INTO customer_id FROM demo_customer_ids WHERE seq=MOD(i-1,__CUSTOMERS__)+1;
    INSERT INTO crm_follow_up_record
      (biz_type,biz_id,type,content,next_time,business_ids,contact_ids,
       creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(2,customer_id,MOD(i,3)+1,
       CONCAT('固定种子客户跟进记录，generated-batch:',@demo_batch),
       DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),'','',
       CAST(@demo_owner AS CHAR),NOW(),CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;

  SET i=1;
  WHILE i <= __WORK_ORDERS__ DO
    SELECT id INTO customer_id FROM demo_customer_ids WHERE seq=MOD(i-1,__CUSTOMERS__)+1;
    INSERT INTO crm_work_order
      (no,title,type,priority,status,customer_id,source_type,handler_user_id,dispatch_mode,assign_time,
       description,solution,process_time,complete_time,return_reason,creator,create_time,updater,update_time,deleted,tenant_id)
      VALUES(CONCAT('D2-',__SEED__,'-',LPAD(i,8,'0')),CONCAT(@demo_batch,'-WO-',LPAD(i,7,'0')),
       MOD(i,4)+1,MOD(i,3)+1,CASE MOD(i,4) WHEN 0 THEN 10 WHEN 1 THEN 20 WHEN 2 THEN 30 ELSE 40 END,
       customer_id,0,@demo_owner,1,DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),
       CONCAT('固定种子工单演示数据，批次 ',@demo_batch),IF(MOD(i,4)=2,'演示工单已完成处理与复核。',NULL),
       IF(MOD(i,4) IN (1,2),DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),NULL),
       IF(MOD(i,4)=2,DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__+1,@demo_span_days) DAY),NULL),
       IF(MOD(i,4)=3,'演示退回原因',NULL),CAST(@demo_owner AS CHAR),
       DATE_ADD(@demo_start,INTERVAL MOD(i*__SEED__,@demo_span_days) DAY),CAST(@demo_owner AS CHAR),NOW(),b'0',@demo_tenant);
    INSERT INTO crm_work_order_record
      (work_order_id,action_type,from_status,to_status,operator_user_id,handler_user_id,remark,creator,deleted,tenant_id)
      VALUES(LAST_INSERT_ID(),1,NULL,10,@demo_owner,@demo_owner,CONCAT('generated-batch:',@demo_batch),CAST(@demo_owner AS CHAR),b'0',@demo_tenant);
    SET i=i+1;
  END WHILE;
  DROP TEMPORARY TABLE demo_customer_ids;
END$$
CALL generate_crm_demo_v2()$$
DROP PROCEDURE generate_crm_demo_v2$$
DELIMITER ;
