package com.meession.etm.module.crm.controller.admin.activity.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmSmsRecordSaveReqVO {
    @NotNull(message = "CRM 对象类型不能为空")
    private Integer bizType;
    @NotNull(message = "CRM 对象编号不能为空")
    private Long bizId;
    private Long contactId;
    @NotNull(message = "短信方向不能为空")
    private Integer direction;
    @NotNull(message = "短信状态不能为空")
    private Integer status;
    @NotBlank(message = "手机号码不能为空")
    @Size(max = 32, message = "手机号码不能超过 32 个字符")
    private String mobile;
    @NotBlank(message = "短信内容不能为空")
    @Size(max = 2000, message = "短信内容不能超过 2000 个字符")
    private String content;
    private Long systemSmsLogId;
    @Size(max = 128, message = "渠道消息编号不能超过 128 个字符")
    private String externalMessageId;
    @Size(max = 1000, message = "失败原因不能超过 1000 个字符")
    private String failureReason;
    @NotNull(message = "短信发生时间不能为空")
    private LocalDateTime occurredTime;
}
