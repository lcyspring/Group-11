-- Repair BPM custom detail metadata that incorrectly stores browser/list routes.
-- Idempotent and audited so existing process instances load one exact detail component.

CREATE TABLE IF NOT EXISTS `bpm_custom_view_path_repair_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `process_definition_id` varchar(64) NOT NULL,
  `old_view_path` varchar(255) NOT NULL,
  `new_view_path` varchar(255) NOT NULL,
  `repair_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_custom_view_path_repair_definition` (`process_definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='BPM 自定义详情组件路径修复审计';

INSERT IGNORE INTO `bpm_custom_view_path_repair_record`
  (`process_definition_id`, `old_view_path`, `new_view_path`)
SELECT `process_definition_id`, `form_custom_view_path`,
       CASE
         WHEN `form_custom_view_path` IN ('/crm/contract', '/crm/contract/index',
                                          '/crm/contract/detail', '/crm/contract/detail2',
                                          '/crm/contract/detail/index', '/crm/business/detail/index')
           THEN '/crm/contract/detail/index.vue'
         WHEN `form_custom_view_path` IN ('/crm/receivable', '/crm/receivable/index',
                                          '/crm/receivable/detail/index')
           THEN '/crm/receivable/detail/index.vue'
         WHEN `form_custom_view_path` IN ('/crm/reimbursement', '/crm/reimbursement/index')
           THEN '/crm/reimbursement/ReimbursementApprovalDetail.vue'
         WHEN `form_custom_view_path` IN ('/crm/receivable-refund', '/crm/receivable-refund/index',
                                          '/crm/refund', '/crm/refund/index')
           THEN '/crm/refund/RefundApprovalDetail.vue'
         WHEN `form_custom_view_path` = '/bpm/oa/leave/detail'
           THEN '/bpm/oa/leave/detail.vue'
         WHEN `form_custom_view_path` = '/bpm/oa/trip/detail'
           THEN '/bpm/oa/trip/detail.vue'
         WHEN `form_custom_view_path` = '/bpm/oa/loan/detail'
           THEN '/bpm/oa/loan/detail.vue'
         WHEN `form_custom_view_path` = '/crm/customer-visit/detail'
           THEN '/crm/customerVisit/detail.vue'
         WHEN `form_custom_view_path` IN ('/bpm/collaboration/work-request', '/bpm/oa/work-request/index')
           THEN '/bpm/oa/work-request/detail.vue'
       END
FROM `bpm_process_definition_info`
WHERE `deleted` = b'0'
  AND `form_custom_view_path` IN (
    '/crm/contract', '/crm/contract/index', '/crm/contract/detail', '/crm/contract/detail2',
    '/crm/contract/detail/index', '/crm/business/detail/index',
    '/crm/receivable', '/crm/receivable/index', '/crm/receivable/detail/index',
    '/crm/reimbursement', '/crm/reimbursement/index',
    '/crm/receivable-refund', '/crm/receivable-refund/index', '/crm/refund', '/crm/refund/index',
    '/bpm/oa/leave/detail', '/bpm/oa/trip/detail', '/bpm/oa/loan/detail',
    '/crm/customer-visit/detail', '/bpm/collaboration/work-request', '/bpm/oa/work-request/index'
  );

UPDATE `bpm_process_definition_info`
SET `form_custom_view_path` = CASE
      WHEN `form_custom_view_path` IN ('/crm/contract', '/crm/contract/index',
                                       '/crm/contract/detail', '/crm/contract/detail2',
                                       '/crm/contract/detail/index', '/crm/business/detail/index')
        THEN '/crm/contract/detail/index.vue'
      WHEN `form_custom_view_path` IN ('/crm/receivable', '/crm/receivable/index',
                                       '/crm/receivable/detail/index')
        THEN '/crm/receivable/detail/index.vue'
      WHEN `form_custom_view_path` IN ('/crm/reimbursement', '/crm/reimbursement/index')
        THEN '/crm/reimbursement/ReimbursementApprovalDetail.vue'
      WHEN `form_custom_view_path` IN ('/crm/receivable-refund', '/crm/receivable-refund/index',
                                       '/crm/refund', '/crm/refund/index')
        THEN '/crm/refund/RefundApprovalDetail.vue'
      WHEN `form_custom_view_path` = '/bpm/oa/leave/detail'
        THEN '/bpm/oa/leave/detail.vue'
      WHEN `form_custom_view_path` = '/bpm/oa/trip/detail'
        THEN '/bpm/oa/trip/detail.vue'
      WHEN `form_custom_view_path` = '/bpm/oa/loan/detail'
        THEN '/bpm/oa/loan/detail.vue'
      WHEN `form_custom_view_path` = '/crm/customer-visit/detail'
        THEN '/crm/customerVisit/detail.vue'
      WHEN `form_custom_view_path` IN ('/bpm/collaboration/work-request', '/bpm/oa/work-request/index')
        THEN '/bpm/oa/work-request/detail.vue'
    END,
    `updater` = 'bpm-view-path-repair',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `form_custom_view_path` IN (
    '/crm/contract', '/crm/contract/index', '/crm/contract/detail', '/crm/contract/detail2',
    '/crm/contract/detail/index', '/crm/business/detail/index',
    '/crm/receivable', '/crm/receivable/index', '/crm/receivable/detail/index',
    '/crm/reimbursement', '/crm/reimbursement/index',
    '/crm/receivable-refund', '/crm/receivable-refund/index', '/crm/refund', '/crm/refund/index',
    '/bpm/oa/leave/detail', '/bpm/oa/trip/detail', '/bpm/oa/loan/detail',
    '/crm/customer-visit/detail', '/bpm/collaboration/work-request', '/bpm/oa/work-request/index'
  );
