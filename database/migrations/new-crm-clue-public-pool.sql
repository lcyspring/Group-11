-- CRM public clue state machine, ownership history, concurrent quota and navigation.
-- Idempotent for MySQL 8.0.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `crm_clue_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `crm_clue_add_column_if_missing`(
    IN table_name_value varchar(64),
    IN column_name_value varchar(64),
    IN column_definition_value text
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @clue_column_ddl = CONCAT('ALTER TABLE `', table_name_value,
                                      '` ADD COLUMN ', column_definition_value);
        PREPARE clue_column_statement FROM @clue_column_ddl;
        EXECUTE clue_column_statement;
        DEALLOCATE PREPARE clue_column_statement;
    END IF;
END$$
DELIMITER ;

ALTER TABLE `crm_clue`
  MODIFY COLUMN `owner_user_id` bigint NULL COMMENT '负责人；公共线索为空';

CALL `crm_clue_add_column_if_missing`('crm_clue', 'owner_time',
    '`owner_time` datetime NULL COMMENT ''当前负责人取得线索时间'' AFTER `owner_user_id`');
CALL `crm_clue_add_column_if_missing`('crm_clue', 'pool_status',
    '`pool_status` tinyint NOT NULL DEFAULT 0 COMMENT ''池状态：0在管、1公共线索'' AFTER `owner_time`');
CALL `crm_clue_add_column_if_missing`('crm_clue', 'pool_entry_time',
    '`pool_entry_time` datetime NULL COMMENT ''本次进入公共池时间'' AFTER `pool_status`');
CALL `crm_clue_add_column_if_missing`('crm_clue', 'pool_previous_owner_user_id',
    '`pool_previous_owner_user_id` bigint NULL COMMENT ''本次入池前负责人'' AFTER `pool_entry_time`');
CALL `crm_clue_add_column_if_missing`('crm_clue', 'pool_reason',
    '`pool_reason` varchar(40) NULL COMMENT ''本次入池原因编码'' AFTER `pool_previous_owner_user_id`');
CALL `crm_clue_add_column_if_missing`('crm_clue', 'pool_reason_detail',
    '`pool_reason_detail` varchar(500) NULL COMMENT ''本次入池原因详情'' AFTER `pool_reason`');
CALL `crm_clue_add_column_if_missing`('crm_clue', 'pool_cycle_count',
    '`pool_cycle_count` int NOT NULL DEFAULT 0 COMMENT ''累计进入公共池次数'' AFTER `pool_reason_detail`');

DROP PROCEDURE `crm_clue_add_column_if_missing`;

CREATE TABLE IF NOT EXISTS `crm_clue_owner_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `clue_id` bigint NOT NULL COMMENT '线索编号',
  `previous_owner_user_id` bigint NULL COMMENT '原负责人',
  `new_owner_user_id` bigint NULL COMMENT '新负责人',
  `type` tinyint NOT NULL COMMENT '类型：1进入公共池、2领取或分配、3初始分配、4转移',
  `source` varchar(40) NOT NULL COMMENT '事件来源编码',
  `reason` varchar(500) NULL COMMENT '原因',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_crm_clue_owner_record_clue` (`tenant_id`,`clue_id`,`create_time`,`id`),
  KEY `idx_crm_clue_owner_record_claim` (`tenant_id`,`source`,`new_owner_user_id`,`create_time`,`clue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 线索归属事件';

CREATE TABLE IF NOT EXISTS `crm_clue_pool_claim_counter` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '领取用户',
  `claim_date` date NOT NULL COMMENT '领取业务日期',
  `claim_count` int NOT NULL DEFAULT 0 COMMENT '当日已领取数量',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_clue_pool_claim_counter` (`tenant_id`,`user_id`,`claim_date`),
  CONSTRAINT `chk_crm_clue_pool_claim_counter` CHECK (`claim_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 公共线索每日领取并发计数';

CREATE TABLE IF NOT EXISTS `crm_clue_owner_capacity_guard` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '负责人',
  `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_clue_owner_capacity_guard` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 线索负责人容量串行化锁';

-- Existing transformed clues remain terminal and never become public. Existing active owners become OWNED.
UPDATE `crm_clue` SET `transform_status` = b'0' WHERE `transform_status` IS NULL;
UPDATE `crm_clue` SET `follow_up_status` = b'0' WHERE `follow_up_status` IS NULL;

UPDATE `crm_clue`
SET `pool_status` = 0,
    `owner_time` = COALESCE(`owner_time`, `update_time`, `create_time`),
    `pool_entry_time` = NULL,
    `pool_previous_owner_user_id` = NULL,
    `pool_reason` = NULL,
    `pool_reason_detail` = NULL
WHERE `owner_user_id` IS NOT NULL OR `transform_status` = b'1';

UPDATE `crm_clue`
SET `pool_status` = 1,
    `pool_entry_time` = COALESCE(`pool_entry_time`, `update_time`, `create_time`),
    `pool_reason` = COALESCE(`pool_reason`, 'CREATE_UNASSIGNED'),
    `pool_cycle_count` = GREATEST(`pool_cycle_count`, 1)
WHERE `deleted` = b'0' AND COALESCE(`transform_status`, b'0') = b'0' AND `owner_user_id` IS NULL;

INSERT INTO `crm_clue_owner_record`
  (`clue_id`,`previous_owner_user_id`,`new_owner_user_id`,`type`,`source`,`reason`,
   `creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT clue.id, NULL, clue.owner_user_id, 3, 'INITIAL_ASSIGN', '历史在管线索归属回填',
       'clue-pool-migration', clue.create_time, 'clue-pool-migration', NOW(), b'0', clue.tenant_id
FROM `crm_clue` clue
WHERE clue.`deleted` = b'0' AND clue.`owner_user_id` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `crm_clue_owner_record` existing
                  WHERE existing.`tenant_id`=clue.`tenant_id` AND existing.`clue_id`=clue.`id`
                    AND existing.`deleted`=b'0');

INSERT INTO `crm_clue_owner_record`
  (`clue_id`,`previous_owner_user_id`,`new_owner_user_id`,`type`,`source`,`reason`,
   `creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT clue.id, NULL, NULL, 1, 'CREATE_UNASSIGNED', '历史未分配线索回填',
       'clue-pool-migration', COALESCE(clue.pool_entry_time, clue.create_time),
       'clue-pool-migration', NOW(), b'0', clue.tenant_id
FROM `crm_clue` clue
WHERE clue.`deleted` = b'0' AND clue.`pool_status` = 1
  AND NOT EXISTS (SELECT 1 FROM `crm_clue_owner_record` existing
                  WHERE existing.`tenant_id`=clue.`tenant_id` AND existing.`clue_id`=clue.`id`
                    AND existing.`deleted`=b'0');

SET @clue_pool_check_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'crm_clue'
      AND constraint_name = 'chk_crm_clue_pool_state'
);
SET @clue_pool_check_sql = IF(@clue_pool_check_exists = 0,
    'ALTER TABLE crm_clue ADD CONSTRAINT chk_crm_clue_pool_state CHECK (pool_status IN (0,1))',
    'SELECT 1');
PREPARE clue_pool_check_statement FROM @clue_pool_check_sql;
EXECUTE clue_pool_check_statement;
DEALLOCATE PREPARE clue_pool_check_statement;

SET @clue_owner_check_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'crm_clue'
      AND constraint_name = 'chk_crm_clue_pool_owner'
);
SET @clue_owner_check_sql = IF(@clue_owner_check_exists = 0,
    'ALTER TABLE crm_clue ADD CONSTRAINT chk_crm_clue_pool_owner CHECK ((COALESCE(transform_status,0)=1 AND pool_status=0) OR (COALESCE(transform_status,0)=0 AND ((pool_status=0 AND owner_user_id IS NOT NULL) OR (pool_status=1 AND owner_user_id IS NULL))))',
    'SELECT 1');
PREPARE clue_owner_check_statement FROM @clue_owner_check_sql;
EXECUTE clue_owner_check_statement;
DEALLOCATE PREPARE clue_owner_check_statement;

SET @clue_pool_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'crm_clue'
      AND index_name = 'idx_crm_clue_pool_state'
);
SET @clue_pool_index_sql = IF(@clue_pool_index_exists = 0,
    'ALTER TABLE crm_clue ADD INDEX idx_crm_clue_pool_state (tenant_id,pool_status,transform_status,pool_entry_time,id)',
    'SELECT 1');
PREPARE clue_pool_index_statement FROM @clue_pool_index_sql;
EXECUTE clue_pool_index_statement;
DEALLOCATE PREPARE clue_pool_index_statement;

SET @clue_auto_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'crm_clue'
      AND index_name = 'idx_crm_clue_auto_pool'
);
SET @clue_auto_index_sql = IF(@clue_auto_index_exists = 0,
    'ALTER TABLE crm_clue ADD INDEX idx_crm_clue_auto_pool (tenant_id,pool_status,transform_status,owner_user_id,id)',
    'SELECT 1');
PREPARE clue_auto_index_statement FROM @clue_auto_index_sql;
EXECUTE clue_auto_index_statement;
DEALLOCATE PREPARE clue_auto_index_statement;

INSERT INTO `system_menu`
  (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,
   `keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT '公共线索','',2,12,crm.id,'clue/public','ep:share','crm/clue/public/index','CrmCluePublic',
       0,b'1',b'1',b'1','clue-pool-migration',NOW(),'clue-pool-migration',NOW(),b'0'
FROM `system_menu` crm
WHERE crm.`path`='/crm' AND crm.`parent_id`=0 AND crm.`deleted`=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` existing
                  WHERE existing.`path`='clue/public' AND existing.`parent_id`=crm.id
                    AND existing.`deleted`=b'0');

INSERT INTO `system_menu`
  (`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,
   `keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT permissions.permission_name, permissions.permission_code, 3, permissions.permission_sort,
       page.id,'','','','',0,b'1',b'1',b'1','clue-pool-migration',NOW(),'clue-pool-migration',NOW(),b'0'
FROM `system_menu` page
JOIN (
    SELECT '公共线索查询' permission_name, 'crm:clue-public:query' permission_code, 1 permission_sort
    UNION ALL SELECT '线索放入公共池', 'crm:clue-public:put', 2
    UNION ALL SELECT '公共线索领取', 'crm:clue-public:claim', 3
    UNION ALL SELECT '公共线索分配', 'crm:clue-public:assign', 4
) permissions
WHERE page.`path`='clue/public' AND page.`deleted`=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` existing
                  WHERE existing.`permission`=permissions.permission_code AND existing.`deleted`=b'0');

INSERT INTO `system_menu_i18n` (`menu_id`,`language`,`name`)
SELECT menu.id, language.language,
       CASE language.language WHEN 'zh-CN' THEN '公共线索'
            WHEN 'en' THEN 'Public Clues' ELSE 'الدلائل العامة' END
FROM `system_menu` menu
JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') language
WHERE menu.`path`='clue/public' AND menu.`deleted`=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `deleted`=b'0';

-- Existing clue readers receive the page/query/claim capability; existing clue editors may return owned clues.
INSERT INTO `system_role_menu`
  (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT DISTINCT role_menu.role_id, target.id, 'clue-pool-migration', NOW(),
       'clue-pool-migration', NOW(), b'0', role_menu.tenant_id
FROM `system_role_menu` role_menu
JOIN `system_menu` source ON source.id=role_menu.menu_id AND source.permission='crm:clue:query'
JOIN `system_menu` target ON (target.path='clue/public'
                           OR target.permission IN ('crm:clue-public:query','crm:clue-public:claim'))
WHERE role_menu.deleted=b'0' AND source.deleted=b'0' AND target.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_role_menu` existing
                  WHERE existing.role_id=role_menu.role_id AND existing.menu_id=target.id
                    AND existing.tenant_id=role_menu.tenant_id AND existing.deleted=b'0');

INSERT INTO `system_role_menu`
  (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT DISTINCT role_menu.role_id, target.id, 'clue-pool-migration', NOW(),
       'clue-pool-migration', NOW(), b'0', role_menu.tenant_id
FROM `system_role_menu` role_menu
JOIN `system_menu` source ON source.id=role_menu.menu_id AND source.permission='crm:clue:update'
JOIN `system_menu` target ON target.permission='crm:clue-public:put'
WHERE role_menu.deleted=b'0' AND source.deleted=b'0' AND target.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_role_menu` existing
                  WHERE existing.role_id=role_menu.role_id AND existing.menu_id=target.id
                    AND existing.tenant_id=role_menu.tenant_id AND existing.deleted=b'0');

-- Assignment follows the existing distribute capability instead of hard-coding a manager role name.
INSERT INTO `system_role_menu`
  (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT DISTINCT role_menu.role_id, target.id, 'clue-pool-migration', NOW(),
       'clue-pool-migration', NOW(), b'0', role_menu.tenant_id
FROM `system_role_menu` role_menu
JOIN `system_menu` source ON source.id=role_menu.menu_id AND source.permission='crm:customer:distribute'
JOIN `system_menu` target ON target.permission='crm:clue-public:assign'
WHERE role_menu.deleted=b'0' AND source.deleted=b'0' AND target.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_role_menu` existing
                  WHERE existing.role_id=role_menu.role_id AND existing.menu_id=target.id
                    AND existing.tenant_id=role_menu.tenant_id AND existing.deleted=b'0');

INSERT INTO `system_role_menu`
  (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT role.id, menu.id, 'clue-pool-migration', NOW(), 'clue-pool-migration', NOW(), b'0', role.tenant_id
FROM `system_role` role
JOIN `system_menu` menu ON (menu.path='clue/public' OR menu.permission LIKE 'crm:clue-public:%')
WHERE role.code='crm_admin' AND role.deleted=b'0' AND menu.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_role_menu` existing
                  WHERE existing.role_id=role.id AND existing.menu_id=menu.id
                    AND existing.tenant_id=role.tenant_id AND existing.deleted=b'0');
