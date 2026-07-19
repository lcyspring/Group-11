package com.meession.etm.module.crm.controller.admin.reimbursement.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CrmReimbursementItemRespVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private LocalDate occurredDate;
    private BigDecimal amount;
    private String description;
    private String invoiceNo;
    private List<String> attachmentUrls;
    private Integer sort;
}
