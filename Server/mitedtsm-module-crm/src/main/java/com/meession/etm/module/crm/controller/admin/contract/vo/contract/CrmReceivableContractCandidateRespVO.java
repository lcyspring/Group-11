package com.meession.etm.module.crm.controller.admin.contract.vo.contract;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrmReceivableContractCandidateRespVO {

    private Long id;
    private String no;
    private String name;
    private Long customerId;
    private String customerName;
    private BigDecimal totalPrice;
    private BigDecimal totalReceivablePrice;
    private BigDecimal remainingReceivablePrice;
}
