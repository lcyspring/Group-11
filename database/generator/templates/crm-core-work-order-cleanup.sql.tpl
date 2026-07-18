-- Replacement cleanup scope: tenant-crm-demo.
-- This is intentionally destructive for CRM business/master facts in the configured tenant.
-- It preserves the primary administrator and reusable policy configuration (pool/contract/work-order policy).
SET @demo_batch = _utf8mb4'__BATCH__' COLLATE utf8mb4_unicode_ci;
SET @demo_tenant = __TENANT__;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS=0;

-- Non-workflow OA facts shipped as part of this demonstration dataset.
DELETE FROM bpm_oa_task WHERE tenant_id=@demo_tenant;
DELETE FROM bpm_oa_event WHERE tenant_id=@demo_tenant;

-- CRM transaction histories and relationship tables.
DELETE FROM crm_approval_repair_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_business_quote_action_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_business_quote_item WHERE tenant_id=@demo_tenant;
DELETE FROM crm_business_quote WHERE tenant_id=@demo_tenant;
DELETE FROM crm_call_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_clue_conversion_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_clue_owner_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_clue_owner_capacity_guard WHERE tenant_id=@demo_tenant;
DELETE FROM crm_clue_pool_claim_counter WHERE tenant_id=@demo_tenant;
DELETE FROM crm_contact_business WHERE tenant_id=@demo_tenant;
DELETE FROM crm_contract_amendment WHERE tenant_id=@demo_tenant;
DELETE FROM crm_contract_attachment WHERE tenant_id=@demo_tenant;
DELETE FROM crm_contract_change_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_contract_fulfillment WHERE tenant_id=@demo_tenant;
DELETE FROM crm_contract_product WHERE tenant_id=@demo_tenant;
DELETE FROM crm_contract_signing WHERE tenant_id=@demo_tenant;
DELETE FROM crm_customer_care_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_customer_import_preview WHERE tenant_id=@demo_tenant;
DELETE FROM crm_customer_lifecycle_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_customer_owner_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_customer_pool_claim_counter WHERE tenant_id=@demo_tenant;
DELETE FROM crm_customer_visit WHERE tenant_id=@demo_tenant;
DELETE FROM crm_erp_customer_mapping WHERE tenant_id=@demo_tenant;
DELETE FROM crm_erp_product_mapping WHERE tenant_id=@demo_tenant;
DELETE FROM crm_export_task WHERE tenant_id=@demo_tenant;
DELETE FROM crm_follow_up_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_invoice_action_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_marketing_broadcast_recipient WHERE tenant_id=@demo_tenant;
DELETE FROM crm_marketing_campaign_relation WHERE tenant_id=@demo_tenant;
DELETE FROM crm_marketing_consent WHERE tenant_id=@demo_tenant;
DELETE FROM crm_marketing_link_recipient WHERE tenant_id=@demo_tenant;
DELETE FROM crm_permission WHERE tenant_id=@demo_tenant;
DELETE FROM crm_receivable_overdue_reminder WHERE tenant_id=@demo_tenant;
DELETE FROM crm_receivable_refund_action_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_receivable_write_off WHERE tenant_id=@demo_tenant;
DELETE FROM crm_reimbursement_action_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_reimbursement_item WHERE tenant_id=@demo_tenant;
DELETE FROM crm_task_action_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_work_order_cc WHERE tenant_id=@demo_tenant;
DELETE FROM crm_work_order_check_in WHERE tenant_id=@demo_tenant;
DELETE FROM crm_work_order_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_work_order_sla WHERE tenant_id=@demo_tenant;

-- CRM business facts and master data.  Policy/configuration tables are not listed here.
DELETE FROM crm_invoice WHERE tenant_id=@demo_tenant;
DELETE FROM crm_receivable_refund WHERE tenant_id=@demo_tenant;
DELETE FROM crm_reimbursement WHERE tenant_id=@demo_tenant;
DELETE FROM crm_expense_category WHERE tenant_id=@demo_tenant;
DELETE FROM crm_receivable WHERE tenant_id=@demo_tenant;
DELETE FROM crm_receivable_plan WHERE tenant_id=@demo_tenant;
DELETE FROM crm_contract WHERE tenant_id=@demo_tenant;
DELETE FROM crm_business_product WHERE tenant_id=@demo_tenant;
DELETE FROM crm_business WHERE tenant_id=@demo_tenant;
DELETE FROM crm_business_status WHERE tenant_id=@demo_tenant;
DELETE FROM crm_business_status_type WHERE tenant_id=@demo_tenant;
DELETE FROM crm_contact WHERE tenant_id=@demo_tenant;
DELETE FROM crm_clue WHERE tenant_id=@demo_tenant;
DELETE FROM crm_customer WHERE tenant_id=@demo_tenant;
DELETE FROM crm_product WHERE tenant_id=@demo_tenant;
DELETE FROM crm_product_category WHERE tenant_id=@demo_tenant;
DELETE FROM crm_competitor WHERE tenant_id=@demo_tenant;
DELETE FROM crm_customer_care_plan WHERE tenant_id=@demo_tenant;
DELETE FROM crm_marketing_broadcast WHERE tenant_id=@demo_tenant;
DELETE FROM crm_marketing_campaign WHERE tenant_id=@demo_tenant;
DELETE FROM crm_marketing_link WHERE tenant_id=@demo_tenant;
DELETE FROM crm_performance_target WHERE tenant_id=@demo_tenant;
DELETE FROM crm_sms_record WHERE tenant_id=@demo_tenant;
DELETE FROM crm_task WHERE tenant_id=@demo_tenant;
DELETE FROM crm_work_order WHERE tenant_id=@demo_tenant;
DELETE FROM crm_work_report WHERE tenant_id=@demo_tenant;

-- Remove every prior generated identity batch, while retaining the configured owner/admin.
DELETE cc FROM crm_work_order_group_member cc JOIN system_users u ON u.id=cc.user_id
 WHERE u.tenant_id=@demo_tenant AND u.remark LIKE 'generated-batch:%';
DELETE t FROM system_oauth2_access_token t JOIN system_users u ON u.id=t.user_id
 WHERE u.tenant_id=@demo_tenant AND u.remark LIKE 'generated-batch:%';
DELETE t FROM system_oauth2_refresh_token t JOIN system_users u ON u.id=t.user_id
 WHERE u.tenant_id=@demo_tenant AND u.remark LIKE 'generated-batch:%';
DELETE ur FROM system_user_role ur JOIN system_users u ON u.id=ur.user_id
 WHERE u.tenant_id=@demo_tenant AND u.remark LIKE 'generated-batch:%';
DELETE rm FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id
 WHERE r.tenant_id=@demo_tenant AND r.remark LIKE 'generated-batch:%';
DELETE FROM system_users
 WHERE tenant_id=@demo_tenant AND id<>__OWNER__ AND remark LIKE 'generated-batch:%';
DELETE FROM system_role
 WHERE tenant_id=@demo_tenant AND remark LIKE 'generated-batch:%';

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
