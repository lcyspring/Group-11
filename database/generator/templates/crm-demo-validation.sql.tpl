-- Executable post-load assertions for the deterministic CRM demo dataset.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @demo_batch = _utf8mb4'__BATCH__' COLLATE utf8mb4_unicode_ci;
SET @demo_tenant = __TENANT__;
SET @demo_end = '__END__ 23:59:59';

DELIMITER $$
DROP PROCEDURE IF EXISTS validate_crm_demo_v2$$
CREATE PROCEDURE validate_crm_demo_v2()
BEGIN
  DECLARE status_type_id BIGINT;

  IF (SELECT COUNT(*) FROM crm_customer WHERE tenant_id=@demo_tenant
      AND name LIKE CONCAT(@demo_batch,'-CUS-%')) <> __CUSTOMERS__ THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: customer count';
  END IF;
  IF (SELECT COUNT(*) FROM crm_business WHERE tenant_id=@demo_tenant
      AND name LIKE CONCAT(@demo_batch,'-BUS-%')) <> __BUSINESSES__ THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: business count';
  END IF;
  IF (SELECT COUNT(*) FROM crm_contract WHERE tenant_id=@demo_tenant
      AND name LIKE CONCAT(@demo_batch,'-CONTRACT-%')) <> __CONTRACTS__ THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: contract count';
  END IF;
  IF (SELECT COUNT(*) FROM crm_receivable_plan WHERE tenant_id=@demo_tenant
      AND remark LIKE CONCAT('generated-batch:',@demo_batch,'%')) <> __PLANS__ THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: receivable plan count';
  END IF;
  IF (SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=@demo_tenant
      AND title LIKE CONCAT(@demo_batch,'-WO-%')) <> __WORK_ORDERS__ THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: work order count';
  END IF;

  SELECT id INTO status_type_id FROM crm_business_status_type
   WHERE tenant_id=@demo_tenant AND deleted=b'0' AND name=CONCAT(@demo_batch,'-标准销售流程') LIMIT 1;
  IF status_type_id IS NULL OR
     (SELECT COUNT(*) FROM crm_business_status WHERE type_id=status_type_id AND deleted=b'0') <> __STAGES__ THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: opportunity stage model';
  END IF;
  IF (SELECT COUNT(*) FROM crm_business WHERE tenant_id=@demo_tenant AND deleted=b'0'
      AND name LIKE CONCAT(@demo_batch,'-BUS-%') AND end_status IS NULL) = 0 OR
     (SELECT COUNT(*) FROM crm_business WHERE tenant_id=@demo_tenant AND deleted=b'0'
      AND name LIKE CONCAT(@demo_batch,'-BUS-%') AND end_status=1) = 0 OR
     (SELECT COUNT(*) FROM crm_business WHERE tenant_id=@demo_tenant AND deleted=b'0'
      AND name LIKE CONCAT(@demo_batch,'-BUS-%') AND end_status=2) = 0 OR
     (SELECT COUNT(*) FROM crm_business WHERE tenant_id=@demo_tenant AND deleted=b'0'
      AND name LIKE CONCAT(@demo_batch,'-BUS-%') AND end_status=3) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: opportunity outcomes';
  END IF;
  IF (SELECT COUNT(*) FROM crm_business WHERE tenant_id=@demo_tenant AND deleted=b'0'
      AND name LIKE CONCAT(@demo_batch,'-BUS-%') AND end_status IS NULL
      AND (status_id IS NULL OR deal_time IS NULL)) > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: open opportunity forecast source';
  END IF;
  IF (SELECT COUNT(*) FROM (
       SELECT s.id FROM crm_business_status s
       LEFT JOIN crm_business b ON b.status_id=s.id AND b.deleted=b'0'
        AND b.tenant_id=@demo_tenant AND b.name LIKE CONCAT(@demo_batch,'-BUS-%')
       WHERE s.type_id=status_type_id AND s.deleted=b'0'
       GROUP BY s.id HAVING COUNT(b.id)=0
     ) empty_stage) > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: empty opportunity stage';
  END IF;
  IF (SELECT COUNT(*) FROM crm_business WHERE tenant_id=@demo_tenant AND deleted=b'0'
      AND name LIKE CONCAT(@demo_batch,'-BUS-%') AND end_status=1 AND end_time IS NULL) > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: actual sales source';
  END IF;

  IF (SELECT COUNT(DISTINCT area_id) FROM crm_customer WHERE tenant_id=@demo_tenant
      AND name LIKE CONCAT(@demo_batch,'-CUS-%')) < 8 OR
     (SELECT COUNT(DISTINCT industry_id) FROM crm_customer WHERE tenant_id=@demo_tenant
      AND name LIKE CONCAT(@demo_batch,'-CUS-%')) < 8 OR
     (SELECT COUNT(DISTINCT source) FROM crm_customer WHERE tenant_id=@demo_tenant
      AND name LIKE CONCAT(@demo_batch,'-CUS-%')) < 8 OR
     (SELECT COUNT(DISTINCT level) FROM crm_customer WHERE tenant_id=@demo_tenant
      AND name LIKE CONCAT(@demo_batch,'-CUS-%')) < 5 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: customer portrait dimensions';
  END IF;
  IF (SELECT COUNT(*) FROM crm_customer WHERE tenant_id=@demo_tenant
      AND name LIKE CONCAT(@demo_batch,'-CUS-%')
      AND create_time>=DATE_SUB(@demo_end,INTERVAL 30 DAY)) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: recent time window';
  END IF;

  IF (SELECT COUNT(*) FROM crm_receivable_plan WHERE tenant_id=@demo_tenant
      AND remark LIKE CONCAT('generated-batch:',@demo_batch,'%')
      AND receivable_id IS NULL AND return_time<@demo_end) = 0 OR
     (SELECT COUNT(*) FROM crm_receivable_plan WHERE tenant_id=@demo_tenant
      AND remark LIKE CONCAT('generated-batch:',@demo_batch,'%')
      AND receivable_id IS NULL AND return_time>@demo_end) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: overdue and future plans';
  END IF;
  IF (SELECT COUNT(DISTINCT status) FROM crm_invoice WHERE tenant_id=@demo_tenant
      AND no LIKE CONCAT(@demo_batch,'-INV-%')) < 5 OR
     (SELECT COUNT(DISTINCT audit_status) FROM crm_reimbursement WHERE tenant_id=@demo_tenant
      AND no LIKE CONCAT(@demo_batch,'-RMB-%')) < 4 OR
     (SELECT COUNT(DISTINCT audit_status) FROM crm_receivable_refund WHERE tenant_id=@demo_tenant
      AND no LIKE CONCAT(@demo_batch,'-REF-%')) < 4 OR
     (SELECT COUNT(DISTINCT status) FROM crm_customer_care_record r
       JOIN crm_customer_care_plan p ON p.id=r.plan_id
      WHERE p.tenant_id=@demo_tenant AND p.code LIKE CONCAT(@demo_batch,'-CARE-%')) < 5 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: business status coverage';
  END IF;
  IF (SELECT COUNT(DISTINCT status) FROM crm_work_order WHERE tenant_id=@demo_tenant
      AND title LIKE CONCAT(@demo_batch,'-WO-%')) < 4 OR
     (SELECT COUNT(*) FROM crm_work_order_record r JOIN crm_work_order w ON w.id=r.work_order_id
      WHERE w.tenant_id=@demo_tenant AND w.title LIKE CONCAT(@demo_batch,'-WO-%')) <= __WORK_ORDERS__ THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: work order history';
  END IF;

  IF (SELECT COUNT(*) FROM crm_contract c LEFT JOIN crm_customer u ON u.id=c.customer_id
      LEFT JOIN crm_business b ON b.id=c.business_id WHERE c.tenant_id=@demo_tenant
      AND c.name LIKE CONCAT(@demo_batch,'-CONTRACT-%') AND (u.id IS NULL OR b.id IS NULL)) > 0 OR
     (SELECT COUNT(*) FROM crm_receivable_plan p LEFT JOIN crm_contract c ON c.id=p.contract_id
      WHERE p.tenant_id=@demo_tenant AND p.remark LIKE CONCAT('generated-batch:',@demo_batch,'%') AND c.id IS NULL) > 0 OR
     (SELECT COUNT(*) FROM crm_work_order w LEFT JOIN crm_customer c ON c.id=w.customer_id
      WHERE w.tenant_id=@demo_tenant AND w.title LIKE CONCAT(@demo_batch,'-WO-%') AND c.id IS NULL) > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo validation failed: orphan reference';
  END IF;
END$$
CALL validate_crm_demo_v2()$$
DROP PROCEDURE validate_crm_demo_v2$$
DELIMITER ;

SELECT
  (SELECT COUNT(*) FROM crm_customer WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-CUS-%')) AS customers,
  (SELECT COUNT(*) FROM crm_business WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-BUS-%')) AS businesses,
  (SELECT COUNT(*) FROM crm_contract WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-CONTRACT-%')) AS contracts,
  (SELECT COUNT(*) FROM crm_receivable_plan WHERE tenant_id=@demo_tenant AND remark LIKE CONCAT('generated-batch:',@demo_batch,'%')) AS plans,
  (SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=@demo_tenant AND title LIKE CONCAT(@demo_batch,'-WO-%')) AS work_orders;
