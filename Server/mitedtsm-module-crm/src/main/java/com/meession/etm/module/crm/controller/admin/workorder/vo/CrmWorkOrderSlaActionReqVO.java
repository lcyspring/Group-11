package com.meession.etm.module.crm.controller.admin.workorder.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmWorkOrderSlaActionReqVO {
    @NotNull(message = "工单编号不能为空")
    private Long id;
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
