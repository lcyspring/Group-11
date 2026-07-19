package com.meession.etm.module.crm.controller.admin.refund.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class CrmReceivableRefundSourceSummaryRespVO {
    private Long receivableId;
    private String receivableNo;
    private BigDecimal receivableAmount;
    private BigDecimal reservedRefundAmount;
    private BigDecimal remainingRefundableAmount;
}
