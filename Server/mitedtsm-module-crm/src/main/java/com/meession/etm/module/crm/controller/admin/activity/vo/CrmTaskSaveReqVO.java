package com.meession.etm.module.crm.controller.admin.activity.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmTaskSaveReqVO {
    private Long id;
    @NotNull(message = "CRM 对象类型不能为空")
    private Integer bizType;
    @NotNull(message = "CRM 对象编号不能为空")
    private Long bizId;
    @NotNull(message = "任务类型不能为空")
    private Integer type;
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 256, message = "任务标题不能超过 256 个字符")
    private String title;
    @Size(max = 10000, message = "任务描述不能超过 10000 个字符")
    private String description;
    @NotNull(message = "任务优先级不能为空")
    private Integer priority;
    @NotNull(message = "任务负责人不能为空")
    private Long assigneeUserId;
    @NotNull(message = "任务截止时间不能为空")
    private LocalDateTime dueTime;
    private LocalDateTime remindTime;
    private Boolean notifySystem = true;
    private Boolean notifyEmail = false;
    private Boolean notifySms = false;
}
