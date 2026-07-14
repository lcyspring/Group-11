package com.meession.etm.module.crm.controller.admin.workorder.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrmWorkOrderActionReqVO {
    @NotNull(message = "工单编号不能为空")
    private Long id;
    private String remark;
}
