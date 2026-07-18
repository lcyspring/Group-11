-- Destructive only for the explicitly named generated batch.
SET @demo_batch = _utf8mb4'__BATCH__' COLLATE utf8mb4_unicode_ci;
SET @demo_tenant = __TENANT__;
DELETE FROM bpm_oa_task WHERE tenant_id=@demo_tenant AND title LIKE CONCAT(@demo_batch,'-TASK-%');
DELETE FROM bpm_oa_event WHERE tenant_id=@demo_tenant AND title LIKE CONCAT(@demo_batch,'-EVENT-%');
DELETE r FROM crm_customer_care_record r JOIN crm_customer_care_plan p ON p.id=r.plan_id
 WHERE p.tenant_id=@demo_tenant AND p.code LIKE CONCAT(@demo_batch,'-CARE-%');
DELETE FROM crm_customer_care_plan WHERE tenant_id=@demo_tenant AND code LIKE CONCAT(@demo_batch,'-CARE-%');
DELETE r FROM crm_marketing_campaign_relation r JOIN crm_marketing_campaign c ON c.id=r.campaign_id
 WHERE c.tenant_id=@demo_tenant AND c.code LIKE CONCAT(@demo_batch,'-MKT-%');
DELETE FROM crm_marketing_campaign WHERE tenant_id=@demo_tenant AND code LIKE CONCAT(@demo_batch,'-MKT-%');
DELETE a FROM crm_receivable_refund_action_record a JOIN crm_receivable_refund r ON r.id=a.refund_id
 WHERE r.tenant_id=@demo_tenant AND r.no LIKE CONCAT(@demo_batch,'-REF-%');
DELETE p FROM crm_permission p JOIN crm_receivable_refund r ON p.biz_type=10 AND p.biz_id=r.id
 WHERE r.tenant_id=@demo_tenant AND r.no LIKE CONCAT(@demo_batch,'-REF-%');
DELETE FROM crm_receivable_refund WHERE tenant_id=@demo_tenant AND no LIKE CONCAT(@demo_batch,'-REF-%');
DELETE a FROM crm_reimbursement_action_record a JOIN crm_reimbursement r ON r.id=a.reimbursement_id
 WHERE r.tenant_id=@demo_tenant AND r.no LIKE CONCAT(@demo_batch,'-RMB-%');
DELETE i FROM crm_reimbursement_item i JOIN crm_reimbursement r ON r.id=i.reimbursement_id
 WHERE r.tenant_id=@demo_tenant AND r.no LIKE CONCAT(@demo_batch,'-RMB-%');
DELETE p FROM crm_permission p JOIN crm_reimbursement r ON p.biz_type=11 AND p.biz_id=r.id
 WHERE r.tenant_id=@demo_tenant AND r.no LIKE CONCAT(@demo_batch,'-RMB-%');
DELETE FROM crm_reimbursement WHERE tenant_id=@demo_tenant AND no LIKE CONCAT(@demo_batch,'-RMB-%');
DELETE FROM crm_expense_category WHERE tenant_id=@demo_tenant AND code LIKE CONCAT(@demo_batch,'-EXP-%');
DELETE a FROM crm_invoice_action_record a JOIN crm_invoice i ON i.id=a.invoice_id
 WHERE i.tenant_id=@demo_tenant AND i.no LIKE CONCAT(@demo_batch,'-INV-%');
DELETE p FROM crm_permission p JOIN crm_invoice i ON p.biz_type=9 AND p.biz_id=i.id
 WHERE i.tenant_id=@demo_tenant AND i.no LIKE CONCAT(@demo_batch,'-INV-%');
DELETE FROM crm_invoice WHERE tenant_id=@demo_tenant AND no LIKE CONCAT(@demo_batch,'-INV-%');
DELETE w FROM crm_receivable_write_off w JOIN crm_receivable r ON r.id=w.receivable_id
 WHERE r.tenant_id=@demo_tenant AND r.no LIKE CONCAT(@demo_batch,'-REC-%');
DELETE p FROM crm_permission p JOIN crm_receivable r ON p.biz_type=7 AND p.biz_id=r.id
 WHERE r.tenant_id=@demo_tenant AND r.no LIKE CONCAT(@demo_batch,'-REC-%');
DELETE FROM crm_receivable WHERE tenant_id=@demo_tenant AND no LIKE CONCAT(@demo_batch,'-REC-%');
DELETE o FROM crm_receivable_overdue_reminder o JOIN crm_receivable_plan r ON r.id=o.receivable_plan_id
 WHERE r.tenant_id=@demo_tenant AND r.remark LIKE CONCAT('generated-batch:',@demo_batch,'%');
DELETE p FROM crm_permission p JOIN crm_receivable_plan r ON p.biz_type=8 AND p.biz_id=r.id
 WHERE r.tenant_id=@demo_tenant AND r.remark LIKE CONCAT('generated-batch:',@demo_batch,'%');
DELETE FROM crm_receivable_plan WHERE tenant_id=@demo_tenant AND remark LIKE CONCAT('generated-batch:',@demo_batch,'%');
DELETE p FROM crm_permission p JOIN crm_contract c ON p.biz_type=5 AND p.biz_id=c.id
 WHERE c.tenant_id=@demo_tenant AND c.name LIKE CONCAT(@demo_batch,'-CONTRACT-%');
DELETE x FROM crm_contract_product x JOIN crm_contract c ON c.id=x.contract_id
 WHERE c.tenant_id=@demo_tenant AND c.name LIKE CONCAT(@demo_batch,'-CONTRACT-%');
DELETE FROM crm_contract WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-CONTRACT-%');
DELETE p FROM crm_permission p JOIN crm_product x ON p.biz_type=6 AND p.biz_id=x.id
 WHERE x.tenant_id=@demo_tenant AND x.name LIKE CONCAT(@demo_batch,'-PRODUCT-%');
DELETE x FROM crm_business_product x JOIN crm_business b ON b.id=x.business_id
 WHERE b.tenant_id=@demo_tenant AND b.name LIKE CONCAT(@demo_batch,'-BUS-%');
DELETE FROM crm_product WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-PRODUCT-%');
DELETE FROM crm_product_category WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-CATEGORY-%');
DELETE p FROM crm_permission p JOIN crm_business b ON p.biz_type=4 AND p.biz_id=b.id
 WHERE b.tenant_id=@demo_tenant AND b.name LIKE CONCAT(@demo_batch,'-BUS-%');
DELETE p FROM crm_permission p JOIN crm_contact c ON p.biz_type=3 AND p.biz_id=c.id
 WHERE c.tenant_id=@demo_tenant AND c.name LIKE CONCAT(@demo_batch,'-CONTACT-%');
DELETE p FROM crm_permission p JOIN crm_customer c ON p.biz_type=2 AND p.biz_id=c.id
 WHERE c.tenant_id=@demo_tenant AND c.name LIKE CONCAT(@demo_batch,'-CUS-%');
DELETE r FROM crm_work_order_record r JOIN crm_work_order w ON w.id=r.work_order_id
 WHERE w.tenant_id=@demo_tenant AND w.title LIKE CONCAT(@demo_batch,'-WO-%');
DELETE FROM crm_work_order WHERE tenant_id=@demo_tenant AND title LIKE CONCAT(@demo_batch,'-WO-%');
DELETE FROM crm_follow_up_record WHERE tenant_id=@demo_tenant
 AND content LIKE CONCAT('%generated-batch:',@demo_batch,'%');
DELETE FROM crm_business WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-BUS-%');
DELETE s FROM crm_business_status s JOIN crm_business_status_type t ON t.id=s.type_id
 WHERE t.tenant_id=@demo_tenant AND t.name=CONCAT(@demo_batch,'-标准销售流程');
DELETE FROM crm_business_status_type
 WHERE tenant_id=@demo_tenant AND name=CONCAT(@demo_batch,'-标准销售流程');
DELETE p FROM crm_permission p JOIN crm_clue c ON p.biz_type=1 AND p.biz_id=c.id
 WHERE c.tenant_id=@demo_tenant AND c.name LIKE CONCAT(@demo_batch,'-CLUE-%');
DELETE r FROM crm_clue_owner_record r JOIN crm_clue c ON c.id=r.clue_id
 WHERE c.tenant_id=@demo_tenant AND c.name LIKE CONCAT(@demo_batch,'-CLUE-%');
DELETE FROM crm_clue WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-CLUE-%');
DELETE r FROM crm_customer_owner_record r JOIN crm_customer c ON c.id=r.customer_id
 WHERE c.tenant_id=@demo_tenant AND c.name LIKE CONCAT(@demo_batch,'-CUS-%');
DELETE c FROM crm_contact c JOIN crm_customer u ON u.id=c.customer_id
 WHERE u.tenant_id=@demo_tenant AND u.name LIKE CONCAT(@demo_batch,'-CUS-%');
DELETE FROM crm_customer WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-CUS-%');
