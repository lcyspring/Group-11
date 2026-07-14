package com.meession.etm.module.crm.controller.admin.invoice.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmInvoiceIssueReqVO {

    @NotNull(message = "发票编号不能为空")
    private Long id;
    @NotBlank(message = "税务发票号码不能为空")
    @Size(max = 50, message = "税务发票号码不能超过 50 个字符")
    private String invoiceNo;
    @NotNull(message = "开票日期不能为空")
    private LocalDateTime invoiceDate;
    @NotNull(message = "经手人不能为空")
    private Long handlerUserId;
    @Size(max = 500, message = "开票说明不能超过 500 个字符")
    private String remark;
}
