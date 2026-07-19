package com.meession.etm.module.crm.controller.admin.invoice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrmInvoiceSummaryRespVO {
    private BigDecimal contractAmount;
    private BigDecimal blueAmount;
    private BigDecimal redAmount;
    private BigDecimal netAmount;
    private BigDecimal availableAmount;
}
