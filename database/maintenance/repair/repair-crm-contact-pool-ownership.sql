-- Repair contact ownership and OWNER permissions from the authoritative customer assignment.
-- Orphan contacts without a valid customer are intentionally left untouched.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @crm_contact_biz_type := 3;
SET @crm_owner_permission_level := 1;
START TRANSACTION;

UPDATE `crm_contact` c
JOIN `crm_customer` u
  ON u.`id` = c.`customer_id`
 AND u.`tenant_id` = c.`tenant_id`
 AND u.`deleted` = b'0'
SET c.`owner_user_id` = u.`owner_user_id`,
    c.`updater` = 'pool-owner-repair',
    c.`update_time` = NOW()
WHERE c.`deleted` = b'0'
  AND NOT (c.`owner_user_id` <=> u.`owner_user_id`);

DELETE p
FROM `crm_permission` p
JOIN `crm_contact` c
  ON c.`id` = p.`biz_id`
 AND c.`tenant_id` = p.`tenant_id`
 AND c.`deleted` = b'0'
JOIN `crm_customer` u
  ON u.`id` = c.`customer_id`
 AND u.`tenant_id` = c.`tenant_id`
 AND u.`deleted` = b'0'
WHERE p.`biz_type` = @crm_contact_biz_type
  AND p.`level` = @crm_owner_permission_level
  AND p.`deleted` = b'0'
  AND (u.`owner_user_id` IS NULL OR p.`user_id` <> u.`owner_user_id`);

DELETE duplicate
FROM `crm_permission` duplicate
JOIN `crm_contact` c
  ON c.`id` = duplicate.`biz_id`
 AND c.`tenant_id` = duplicate.`tenant_id`
 AND c.`deleted` = b'0'
JOIN `crm_customer` u
  ON u.`id` = c.`customer_id`
 AND u.`tenant_id` = c.`tenant_id`
 AND u.`deleted` = b'0'
JOIN `crm_permission` keep
  ON keep.`biz_type` = duplicate.`biz_type`
 AND keep.`biz_id` = duplicate.`biz_id`
 AND keep.`tenant_id` = duplicate.`tenant_id`
 AND keep.`user_id` = duplicate.`user_id`
 AND keep.`level` = duplicate.`level`
 AND keep.`deleted` = b'0'
 AND keep.`id` < duplicate.`id`
WHERE duplicate.`biz_type` = @crm_contact_biz_type
  AND duplicate.`level` = @crm_owner_permission_level
  AND duplicate.`deleted` = b'0'
  AND duplicate.`user_id` = u.`owner_user_id`;

INSERT INTO `crm_permission`
  (`biz_type`, `biz_id`, `user_id`, `level`, `creator`, `create_time`,
   `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT @crm_contact_biz_type, c.`id`, u.`owner_user_id`, @crm_owner_permission_level,
       'pool-owner-repair', NOW(),
       'pool-owner-repair', NOW(), b'0', c.`tenant_id`
FROM `crm_contact` c
JOIN `crm_customer` u
  ON u.`id` = c.`customer_id`
 AND u.`tenant_id` = c.`tenant_id`
 AND u.`deleted` = b'0'
LEFT JOIN `crm_permission` p
  ON p.`biz_type` = @crm_contact_biz_type
 AND p.`biz_id` = c.`id`
 AND p.`user_id` = u.`owner_user_id`
 AND p.`level` = @crm_owner_permission_level
 AND p.`tenant_id` = c.`tenant_id`
 AND p.`deleted` = b'0'
WHERE c.`deleted` = b'0'
  AND u.`owner_user_id` IS NOT NULL
  AND p.`id` IS NULL;

COMMIT;
