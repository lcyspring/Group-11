-- Destructive only for the explicitly named generated batch.
SET @demo_batch = _utf8mb4'__BATCH__' COLLATE utf8mb4_unicode_ci;
SET @demo_tenant = __TENANT__;
DELETE p FROM crm_permission p JOIN crm_business b ON p.biz_type=4 AND p.biz_id=b.id
 WHERE b.tenant_id=@demo_tenant AND b.name LIKE CONCAT(@demo_batch,'-BUS-%');
DELETE p FROM crm_permission p JOIN crm_contact c ON p.biz_type=3 AND p.biz_id=c.id
 WHERE c.tenant_id=@demo_tenant AND c.name LIKE CONCAT(@demo_batch,'-CONTACT-%');
DELETE p FROM crm_permission p JOIN crm_customer c ON p.biz_type=2 AND p.biz_id=c.id
 WHERE c.tenant_id=@demo_tenant AND c.name LIKE CONCAT(@demo_batch,'-CUS-%');
DELETE r FROM crm_work_order_record r JOIN crm_work_order w ON w.id=r.work_order_id
 WHERE w.tenant_id=@demo_tenant AND w.title LIKE CONCAT(@demo_batch,'-WO-%');
DELETE FROM crm_work_order WHERE tenant_id=@demo_tenant AND title LIKE CONCAT(@demo_batch,'-WO-%');
DELETE FROM crm_business WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-BUS-%');
DELETE r FROM crm_customer_owner_record r JOIN crm_customer c ON c.id=r.customer_id
 WHERE c.tenant_id=@demo_tenant AND c.name LIKE CONCAT(@demo_batch,'-CUS-%');
DELETE c FROM crm_contact c JOIN crm_customer u ON u.id=c.customer_id
 WHERE u.tenant_id=@demo_tenant AND u.name LIKE CONCAT(@demo_batch,'-CUS-%');
DELETE FROM crm_customer WHERE tenant_id=@demo_tenant AND name LIKE CONCAT(@demo_batch,'-CUS-%');
