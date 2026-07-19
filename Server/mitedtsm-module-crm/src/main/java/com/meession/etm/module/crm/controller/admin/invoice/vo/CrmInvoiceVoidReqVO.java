package com.meession.etm.module.crm.controller.admin.invoice.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmInvoiceVoidReqVO {

    @NotNull(message = "发票编号不能为空")
    private Long id;
    @NotBlank(message = "作废原因不能为空")
    @Size(max = 500, message = "作废原因不能超过 500 个字符")
    private String reason;
}
