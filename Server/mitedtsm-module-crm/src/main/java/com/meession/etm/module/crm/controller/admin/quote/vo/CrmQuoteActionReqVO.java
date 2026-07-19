package com.meession.etm.module.crm.controller.admin.quote.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmQuoteActionReqVO {
    @NotNull(message = "商机编号不能为空")
    private Long businessId;
    @NotBlank(message = "报价操作原因不能为空")
    @Size(max = 500, message = "报价操作原因不能超过 500 个字符")
    private String remark;
}
