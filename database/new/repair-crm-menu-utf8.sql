-- Repair CRM display metadata written through a MySQL client that negotiated latin1.
-- Stable paths, permissions, dictionary keys, and notification codes make this repeatable.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE `system_menu`
SET `name` = CASE
  WHEN `component`='crm/workorder/index' THEN '客服工单'
  WHEN `component`='crm/statistics/workorder/index' THEN '工单统计'
  WHEN `component`='crm/invoice/index' THEN '发票管理'
  WHEN `component`='crm/refund/index' THEN '退款/冲销管理'
  ELSE `name`
END
WHERE `component` IN ('crm/workorder/index','crm/statistics/workorder/index','crm/invoice/index','crm/refund/index')
  AND `deleted`=b'0';

UPDATE `system_menu`
SET `name` = CASE `permission`
  WHEN 'crm:work-order:query' THEN '工单查询'
  WHEN 'crm:work-order:query-all' THEN '工单查询全部'
  WHEN 'crm:work-order:create' THEN '工单创建'
  WHEN 'crm:work-order:update' THEN '工单修改'
  WHEN 'crm:work-order:delete' THEN '工单删除'
  WHEN 'crm:work-order:process' THEN '工单处理'
  WHEN 'crm:work-order:assign' THEN '工单分派'
  WHEN 'crm:invoice:query' THEN '发票查询'
  WHEN 'crm:invoice:create' THEN '发票创建'
  WHEN 'crm:invoice:update' THEN '发票修改'
  WHEN 'crm:invoice:delete' THEN '发票删除'
  WHEN 'crm:invoice:issue' THEN '正式开票'
  WHEN 'crm:invoice:red-flush' THEN '发票红冲'
  WHEN 'crm:invoice:void' THEN '发票作废'
  WHEN 'crm:invoice:export' THEN '发票导出'
  WHEN 'crm:contract:attachment' THEN '合同附件维护'
  WHEN 'crm:contract:sign' THEN '合同签署'
  WHEN 'crm:contract:sign-void' THEN '合同签署作废'
  WHEN 'crm:receivable-refund:query' THEN '退款/冲销查询'
  WHEN 'crm:receivable-refund:create' THEN '退款/冲销创建'
  WHEN 'crm:receivable-refund:update' THEN '退款/冲销修改'
  WHEN 'crm:receivable-refund:delete' THEN '退款/冲销删除'
  ELSE `name`
END
WHERE `permission` IN (
  'crm:work-order:query','crm:work-order:query-all','crm:work-order:create',
  'crm:work-order:update','crm:work-order:delete','crm:work-order:process','crm:work-order:assign',
  'crm:invoice:query','crm:invoice:create','crm:invoice:update','crm:invoice:delete',
  'crm:invoice:issue','crm:invoice:red-flush','crm:invoice:void','crm:invoice:export',
  'crm:contract:attachment','crm:contract:sign','crm:contract:sign-void',
  'crm:receivable-refund:query','crm:receivable-refund:create',
  'crm:receivable-refund:update','crm:receivable-refund:delete'
) AND `deleted`=b'0';

UPDATE `system_dict_type`
SET `name` = CASE `type`
      WHEN 'crm_work_order_status' THEN 'CRM 客服工单状态'
      WHEN 'crm_work_order_type' THEN 'CRM 客服工单类型'
      WHEN 'crm_work_order_priority' THEN 'CRM 客服工单优先级'
      WHEN 'crm_invoice_status' THEN 'CRM 发票状态'
      WHEN 'crm_invoice_type' THEN 'CRM 发票类型'
      WHEN 'crm_invoice_direction' THEN 'CRM 发票方向'
      WHEN 'crm_receivable_refund_type' THEN 'CRM 退款/冲销类型'
    END,
    `remark` = CASE `type`
      WHEN 'crm_work_order_status' THEN 'CRM 客服工单状态'
      WHEN 'crm_work_order_type' THEN 'CRM 客服工单类型'
      WHEN 'crm_work_order_priority' THEN 'CRM 客服工单优先级'
      WHEN 'crm_invoice_status' THEN 'CRM 发票生命周期状态'
      WHEN 'crm_invoice_type' THEN 'CRM 发票类型'
      WHEN 'crm_invoice_direction' THEN '蓝票与红票方向'
      WHEN 'crm_receivable_refund_type' THEN '客户退款与业务冲销'
    END
WHERE `type` IN (
  'crm_work_order_status','crm_work_order_type','crm_work_order_priority',
  'crm_invoice_status','crm_invoice_type','crm_invoice_direction','crm_receivable_refund_type'
) AND `deleted`=b'0';

UPDATE `system_dict_data`
SET `label` = CASE CONCAT(`dict_type`, ':', `value`)
  WHEN 'crm_work_order_status:10' THEN '待处理'
  WHEN 'crm_work_order_status:20' THEN '处理中'
  WHEN 'crm_work_order_status:30' THEN '已完结'
  WHEN 'crm_work_order_status:40' THEN '已退回'
  WHEN 'crm_work_order_type:1' THEN '问题'
  WHEN 'crm_work_order_type:2' THEN '需求'
  WHEN 'crm_work_order_type:3' THEN '投诉'
  WHEN 'crm_work_order_type:4' THEN '咨询'
  WHEN 'crm_work_order_priority:1' THEN '低'
  WHEN 'crm_work_order_priority:2' THEN '中'
  WHEN 'crm_work_order_priority:3' THEN '高'
  WHEN 'crm_invoice_status:0' THEN '草稿'
  WHEN 'crm_invoice_status:10' THEN '已开具'
  WHEN 'crm_invoice_status:20' THEN '部分红冲'
  WHEN 'crm_invoice_status:30' THEN '已全部红冲'
  WHEN 'crm_invoice_status:40' THEN '已作废'
  WHEN 'crm_invoice_type:1' THEN '增值税普通发票'
  WHEN 'crm_invoice_type:2' THEN '增值税专用发票'
  WHEN 'crm_invoice_direction:1' THEN '蓝票'
  WHEN 'crm_invoice_direction:-1' THEN '红票'
  WHEN 'crm_receivable_refund_type:1' THEN '客户退款'
  WHEN 'crm_receivable_refund_type:2' THEN '业务冲销'
  ELSE `label`
END
WHERE `dict_type` IN (
  'crm_work_order_status','crm_work_order_type','crm_work_order_priority',
  'crm_invoice_status','crm_invoice_type','crm_invoice_direction','crm_receivable_refund_type'
) AND `deleted`=b'0';

UPDATE `system_notify_template`
SET `name` = CASE `code`
      WHEN 'crm-work-order-assigned' THEN '客服工单已分派'
      WHEN 'crm-work-order-returned' THEN '客服工单已退回'
      WHEN 'crm-work-order-completed' THEN '客服工单已完结'
    END,
    `content` = CASE `code`
      WHEN 'crm-work-order-assigned' THEN '客服工单 {no}「{title}」已分派给你，请及时处理。'
      WHEN 'crm-work-order-returned' THEN '客服工单 {no}「{title}」已退回，原因：{reason}。'
      WHEN 'crm-work-order-completed' THEN '客服工单 {no}「{title}」已完结，请查看处理结果。'
    END
WHERE `code` IN ('crm-work-order-assigned','crm-work-order-returned','crm-work-order-completed')
  AND `deleted`=b'0';

-- Sent messages contain a mixture of the formerly corrupted template text and
-- correctly encoded JSON parameters. Rebuild them from stable template codes;
-- a whole-string charset conversion would destroy the already-correct title.
UPDATE `system_notify_message`
SET `template_content` = CASE `template_code`
  WHEN 'crm-work-order-assigned' THEN CONCAT(
    '客服工单 ', JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.no')), '「',
    JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.title')), '」已分派给你，请及时处理。')
  WHEN 'crm-work-order-returned' THEN CONCAT(
    '客服工单 ', JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.no')), '「',
    JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.title')), '」已退回，原因：',
    JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.reason')), '。')
  WHEN 'crm-work-order-completed' THEN CONCAT(
    '客服工单 ', JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.no')), '「',
    JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.title')), '」已完结，请查看处理结果。')
END
WHERE `template_code` IN ('crm-work-order-assigned','crm-work-order-returned','crm-work-order-completed')
  AND `deleted`=b'0'
  AND JSON_VALID(`template_params`)
  AND JSON_EXTRACT(`template_params`, '$.no') IS NOT NULL
  AND JSON_EXTRACT(`template_params`, '$.title') IS NOT NULL
  AND (`template_code` <> 'crm-work-order-returned'
    OR JSON_EXTRACT(`template_params`, '$.reason') IS NOT NULL)
  AND (HEX(`template_content`) LIKE '%C383%' OR HEX(`template_content`) LIKE '%C2%');

-- The source migrations upsert all available translations. The statements below
-- additionally make this repair safe to run on its own for the four visible pages.
INSERT INTO `system_menu_i18n` (`menu_id`,`language`,`name`)
SELECT m.id,l.language,CASE m.component
  WHEN 'crm/workorder/index' THEN CASE l.language WHEN 'zh-CN' THEN '客服工单' WHEN 'en' THEN 'Service Work Orders' ELSE 'أوامر خدمة العملاء' END
  WHEN 'crm/statistics/workorder/index' THEN CASE l.language WHEN 'zh-CN' THEN '工单统计' WHEN 'en' THEN 'Work Order Analytics' ELSE 'تحليلات أوامر العمل' END
  WHEN 'crm/invoice/index' THEN CASE l.language WHEN 'zh-CN' THEN '发票管理' WHEN 'en' THEN 'Invoice Management' ELSE 'إدارة الفواتير' END
  ELSE CASE l.language WHEN 'zh-CN' THEN '退款/冲销管理' WHEN 'en' THEN 'Refund / Reversal' ELSE 'الاسترداد / العكس' END
END
FROM `system_menu` m
JOIN (SELECT 'zh-CN' language UNION ALL SELECT 'en' UNION ALL SELECT 'ar') l
WHERE m.component IN ('crm/workorder/index','crm/statistics/workorder/index','crm/invoice/index','crm/refund/index')
  AND m.deleted=b'0'
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`),`deleted`=b'0';
