package com.meession.etm.module.crm.controller.admin.activity.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmTaskActionReqVO {
    @NotNull(message = "任务编号不能为空")
    private Long id;
    @Size(max = 1000, message = "任务处理说明不能超过 1000 个字符")
    private String remark;
}
