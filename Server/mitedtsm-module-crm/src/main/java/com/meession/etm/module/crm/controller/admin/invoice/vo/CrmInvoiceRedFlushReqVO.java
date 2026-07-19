package com.meession.etm.module.crm.controller.admin.invoice.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CrmInvoiceRedFlushReqVO {

    @NotNull(message = "原发票编号不能为空")
    private Long originalInvoiceId;
    @NotNull(message = "红冲金额不能为空")
    @DecimalMin(value = "0.01", message = "红冲金额必须大于 0")
    private BigDecimal amount;
    @NotBlank(message = "红票号码不能为空")
    @Size(max = 50, message = "红票号码不能超过 50 个字符")
    private String invoiceNo;
    @NotNull(message = "红票日期不能为空")
    private LocalDateTime invoiceDate;
    @NotNull(message = "经手人不能为空")
    private Long handlerUserId;
    @NotBlank(message = "红冲原因不能为空")
    @Size(max = 500, message = "红冲原因不能超过 500 个字符")
    private String reason;
}
