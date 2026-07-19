package com.meession.etm.module.crm.controller.admin.workorder.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmWorkOrderCompleteReqVO {
    @NotNull(message = "工单编号不能为空")
    private Long id;
    @NotBlank(message = "解决方案不能为空")
    @Size(max = 5000, message = "解决方案不能超过 5000 个字符")
    private String solution;
}
