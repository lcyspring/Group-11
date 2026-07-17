CREATE TABLE IF NOT EXISTS `crm_approval_repair_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型',
  `biz_id` bigint NOT NULL COMMENT '业务编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `old_process_instance_id` varchar(64) NOT NULL COMMENT '原流程实例编号或缺失标记',
  `old_audit_status` int NOT NULL COMMENT '原审批状态',
  `new_audit_status` int NOT NULL COMMENT '修复后审批状态',
  `reason` varchar(255) NOT NULL COMMENT '修复原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crm_approval_repair` (`tenant_id`,`biz_type`,`biz_id`,`old_process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 孤立审批状态修复记录';

DELIMITER $$

DROP PROCEDURE IF EXISTS `repair_crm_orphan_approval_state`$$
CREATE PROCEDURE `repair_crm_orphan_approval_state`()
BEGIN
IF EXISTS (
  SELECT 1 FROM information_schema.tables
  WHERE table_schema=DATABASE() AND table_name='ACT_HI_PROCINST'
) THEN

INSERT IGNORE INTO crm_approval_repair_record
  (biz_type,biz_id,tenant_id,old_process_instance_id,old_audit_status,new_audit_status,reason)
SELECT 'contract',source.id,source.tenant_id,COALESCE(source.process_instance_id,'missing-id'),
       source.audit_status,40,'审批中记录不存在对应 Flowable 历史实例，取消后允许修订重提'
FROM crm_contract source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
WHERE source.deleted=b'0' AND source.audit_status=10 AND flow.ID_ IS NULL;

UPDATE crm_contract source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
SET source.audit_status=40,source.process_instance_id=NULL,
    source.updater='crm-orphan-approval-repair',source.update_time=NOW()
WHERE source.deleted=b'0' AND source.audit_status=10 AND flow.ID_ IS NULL;

INSERT IGNORE INTO crm_approval_repair_record
  (biz_type,biz_id,tenant_id,old_process_instance_id,old_audit_status,new_audit_status,reason)
SELECT 'receivable',source.id,source.tenant_id,COALESCE(source.process_instance_id,'missing-id'),
       source.audit_status,40,'审批中记录不存在对应 Flowable 历史实例，取消后允许修订重提'
FROM crm_receivable source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
WHERE source.deleted=b'0' AND source.audit_status=10 AND flow.ID_ IS NULL;

UPDATE crm_receivable source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
SET source.audit_status=40,source.process_instance_id=NULL,
    source.updater='crm-orphan-approval-repair',source.update_time=NOW()
WHERE source.deleted=b'0' AND source.audit_status=10 AND flow.ID_ IS NULL;

INSERT IGNORE INTO crm_approval_repair_record
  (biz_type,biz_id,tenant_id,old_process_instance_id,old_audit_status,new_audit_status,reason)
SELECT 'refund',source.id,source.tenant_id,COALESCE(source.process_instance_id,'missing-id'),
       source.audit_status,40,'审批中记录不存在对应 Flowable 历史实例，取消后允许修订重提'
FROM crm_receivable_refund source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
WHERE source.deleted=b'0' AND source.audit_status=10 AND flow.ID_ IS NULL;

UPDATE crm_receivable_refund source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
SET source.audit_status=40,source.process_instance_id=NULL,
    source.updater='crm-orphan-approval-repair',source.update_time=NOW()
WHERE source.deleted=b'0' AND source.audit_status=10 AND flow.ID_ IS NULL;

INSERT IGNORE INTO crm_approval_repair_record
  (biz_type,biz_id,tenant_id,old_process_instance_id,old_audit_status,new_audit_status,reason)
SELECT 'reimbursement',source.id,source.tenant_id,COALESCE(source.process_instance_id,'missing-id'),
       source.audit_status,40,'审批中记录不存在对应 Flowable 历史实例，取消后允许修订重提'
FROM crm_reimbursement source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
WHERE source.deleted=b'0' AND source.audit_status=10 AND flow.ID_ IS NULL;

UPDATE crm_reimbursement source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
SET source.audit_status=40,source.process_instance_id=NULL,
    source.updater='crm-orphan-approval-repair',source.update_time=NOW()
WHERE source.deleted=b'0' AND source.audit_status=10 AND flow.ID_ IS NULL;

INSERT IGNORE INTO crm_approval_repair_record
  (biz_type,biz_id,tenant_id,old_process_instance_id,old_audit_status,new_audit_status,reason)
SELECT 'contract-amendment',source.id,source.tenant_id,COALESCE(source.process_instance_id,'missing-id'),
       source.audit_status,40,'审批中记录不存在对应 Flowable 历史实例，取消后允许修订重提'
FROM crm_contract_amendment source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
WHERE source.deleted=b'0' AND source.audit_status=10 AND flow.ID_ IS NULL;

UPDATE crm_contract_amendment source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
SET source.audit_status=40,source.process_instance_id=NULL,
    source.updater='crm-orphan-approval-repair',source.update_time=NOW()
WHERE source.deleted=b'0' AND source.audit_status=10 AND flow.ID_ IS NULL;

-- 已结束审批保留业务结果，但失效的历史实例编号不能继续向用户暴露“查看审批”入口。
-- 原编号、状态和修复原因先写入审计表，再仅清空孤立编号；该处理覆盖所有 CRM 审批域。
INSERT IGNORE INTO crm_approval_repair_record
  (biz_type,biz_id,tenant_id,old_process_instance_id,old_audit_status,new_audit_status,reason)
SELECT 'contract',source.id,source.tenant_id,source.process_instance_id,
       source.audit_status,source.audit_status,'已结束审批不存在对应 Flowable 历史实例，保留审批结果并移除失效查看入口'
FROM crm_contract source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
WHERE source.deleted=b'0' AND source.audit_status IN (20,30,40)
  AND source.process_instance_id IS NOT NULL AND flow.ID_ IS NULL;

UPDATE crm_contract source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
SET source.process_instance_id=NULL,source.updater='crm-orphan-approval-repair',source.update_time=NOW()
WHERE source.deleted=b'0' AND source.audit_status IN (20,30,40)
  AND source.process_instance_id IS NOT NULL AND flow.ID_ IS NULL;

INSERT IGNORE INTO crm_approval_repair_record
  (biz_type,biz_id,tenant_id,old_process_instance_id,old_audit_status,new_audit_status,reason)
SELECT 'receivable',source.id,source.tenant_id,source.process_instance_id,
       source.audit_status,source.audit_status,'已结束审批不存在对应 Flowable 历史实例，保留审批结果并移除失效查看入口'
FROM crm_receivable source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
WHERE source.deleted=b'0' AND source.audit_status IN (20,30,40)
  AND source.process_instance_id IS NOT NULL AND flow.ID_ IS NULL;

UPDATE crm_receivable source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
SET source.process_instance_id=NULL,source.updater='crm-orphan-approval-repair',source.update_time=NOW()
WHERE source.deleted=b'0' AND source.audit_status IN (20,30,40)
  AND source.process_instance_id IS NOT NULL AND flow.ID_ IS NULL;

INSERT IGNORE INTO crm_approval_repair_record
  (biz_type,biz_id,tenant_id,old_process_instance_id,old_audit_status,new_audit_status,reason)
SELECT 'refund',source.id,source.tenant_id,source.process_instance_id,
       source.audit_status,source.audit_status,'已结束审批不存在对应 Flowable 历史实例，保留审批结果并移除失效查看入口'
FROM crm_receivable_refund source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
WHERE source.deleted=b'0' AND source.audit_status IN (20,30,40)
  AND source.process_instance_id IS NOT NULL AND flow.ID_ IS NULL;

UPDATE crm_receivable_refund source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
SET source.process_instance_id=NULL,source.updater='crm-orphan-approval-repair',source.update_time=NOW()
WHERE source.deleted=b'0' AND source.audit_status IN (20,30,40)
  AND source.process_instance_id IS NOT NULL AND flow.ID_ IS NULL;

INSERT IGNORE INTO crm_approval_repair_record
  (biz_type,biz_id,tenant_id,old_process_instance_id,old_audit_status,new_audit_status,reason)
SELECT 'reimbursement',source.id,source.tenant_id,source.process_instance_id,
       source.audit_status,source.audit_status,'已结束审批不存在对应 Flowable 历史实例，保留审批结果并移除失效查看入口'
FROM crm_reimbursement source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
WHERE source.deleted=b'0' AND source.audit_status IN (20,30,40)
  AND source.process_instance_id IS NOT NULL AND flow.ID_ IS NULL;

UPDATE crm_reimbursement source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
SET source.process_instance_id=NULL,source.updater='crm-orphan-approval-repair',source.update_time=NOW()
WHERE source.deleted=b'0' AND source.audit_status IN (20,30,40)
  AND source.process_instance_id IS NOT NULL AND flow.ID_ IS NULL;

INSERT IGNORE INTO crm_approval_repair_record
  (biz_type,biz_id,tenant_id,old_process_instance_id,old_audit_status,new_audit_status,reason)
SELECT 'contract-amendment',source.id,source.tenant_id,source.process_instance_id,
       source.audit_status,source.audit_status,'已结束审批不存在对应 Flowable 历史实例，保留审批结果并移除失效查看入口'
FROM crm_contract_amendment source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
WHERE source.deleted=b'0' AND source.audit_status IN (20,30,40)
  AND source.process_instance_id IS NOT NULL AND flow.ID_ IS NULL;

UPDATE crm_contract_amendment source
LEFT JOIN ACT_HI_PROCINST flow ON flow.ID_=source.process_instance_id
SET source.process_instance_id=NULL,source.updater='crm-orphan-approval-repair',source.update_time=NOW()
WHERE source.deleted=b'0' AND source.audit_status IN (20,30,40)
  AND source.process_instance_id IS NOT NULL AND flow.ID_ IS NULL;

END IF;
END$$

CALL `repair_crm_orphan_approval_state`()$$
DROP PROCEDURE `repair_crm_orphan_approval_state`$$

DELIMITER ;
