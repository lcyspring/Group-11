package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CRM 客户 360 只读聚合 Response VO")
@Data
@Accessors(chain = true)
public class CrmCustomer360SummaryRespVO {

    private Long customerId;
    private Long contactCount;
    private Long businessCount;
    /** MVP 的原型销售订单由 CRM 合同承接。 */
    private Long mappedOrderCount;
    private Long receivablePlanCount;
    private Long receivableCount;
    private Long invoiceCount;
    private Long workOrderCount;
    private Long contractAttachmentCount;

    /** 审批通过合同金额。 */
    private BigDecimal contractAmount;
    /** 审批通过回款金额。 */
    private BigDecimal approvedReceivableAmount;
    /** 已开具且未作废的蓝票减红票净额。 */
    private BigDecimal effectiveInvoiceAmount;
    private BigDecimal outstandingReceivableAmount;
    private BigDecimal uninvoicedAmount;

    /** OA 任务域尚未落地，避免用工单伪装任务。 */
    private Boolean taskSupported;
}
