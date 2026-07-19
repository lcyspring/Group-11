package com.meession.etm.module.crm.controller.admin.activity.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmCallRecordSaveReqVO {
    @NotNull(message = "CRM 对象类型不能为空")
    private Integer bizType;
    @NotNull(message = "CRM 对象编号不能为空")
    private Long bizId;
    private Long contactId;
    @NotNull(message = "通话方向不能为空")
    private Integer direction;
    @NotNull(message = "通话状态不能为空")
    private Integer status;
    @NotBlank(message = "电话号码不能为空")
    @Size(max = 32, message = "电话号码不能超过 32 个字符")
    private String phone;
    @NotNull(message = "通话开始时间不能为空")
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @Size(max = 1024, message = "录音地址不能超过 1024 个字符")
    private String recordingUrl;
    @Size(max = 2000, message = "通话摘要不能超过 2000 个字符")
    private String summary;
}
