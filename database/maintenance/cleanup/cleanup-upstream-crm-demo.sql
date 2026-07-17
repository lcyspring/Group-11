-- Empty-volume production dataset cleanup.
-- This script is selected only by database/datasets/none.manifest after the upstream
-- bootstrap dump has been imported. It must never be added to the compatibility manifest.

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS=0;

DELETE FROM crm_permission;
DELETE FROM crm_follow_up_record;
DELETE FROM crm_receivable_plan;
DELETE FROM crm_receivable;
DELETE FROM crm_contract_product;
DELETE FROM crm_contract;
DELETE FROM crm_contact_business;
DELETE FROM crm_business_product;
DELETE FROM crm_business;
DELETE FROM crm_clue;
DELETE FROM crm_contact;
DELETE FROM crm_customer;
DELETE FROM crm_product;
DELETE FROM crm_product_category;

-- The upstream limits reference demonstration users and departments and are not
-- safe production defaults. Contract/pool configuration and sales stages remain
-- as editable base configuration rather than business transactions.
DELETE FROM crm_customer_limit_config;

-- Keep only the primary administrator, its tenant, role bindings and department
-- ancestry required for the first production login. Remove upstream demo accounts,
-- tenants, organization leaves, credentials, message channels and runtime traces.
DELETE FROM system_user_post WHERE user_id <> 1;
DELETE FROM system_user_role WHERE user_id <> 1;
DELETE FROM system_social_user_bind;
DELETE FROM system_social_user;
DELETE FROM system_users WHERE id <> 1;
DELETE FROM system_dept WHERE tenant_id <> 1 OR id NOT IN (100,101,103);
DELETE FROM system_tenant WHERE id <> 1;
DELETE FROM system_post;

DELETE FROM system_oauth2_access_token;
DELETE FROM system_oauth2_refresh_token;
DELETE FROM system_oauth2_approve;
DELETE FROM system_oauth2_code;
DELETE FROM system_login_log;
DELETE FROM system_operate_log;
DELETE FROM system_notify_message;
DELETE FROM system_notice;
DELETE FROM system_mail_log;
DELETE FROM system_mail_account;
DELETE FROM system_mail_template;
DELETE FROM system_sms_log;
DELETE FROM system_sms_code;
DELETE FROM system_sms_channel;
DELETE FROM system_sms_template;

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
