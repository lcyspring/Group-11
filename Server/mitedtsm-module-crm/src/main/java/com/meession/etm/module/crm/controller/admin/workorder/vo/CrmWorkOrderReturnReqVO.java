package com.meession.etm.module.crm.controller.admin.workorder.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmWorkOrderReturnReqVO {
    @NotNull(message = "工单编号不能为空")
    private Long id;
    @NotBlank(message = "退回原因不能为空")
    @Size(max = 1000, message = "退回原因不能超过 1000 个字符")
    private String reason;
}
