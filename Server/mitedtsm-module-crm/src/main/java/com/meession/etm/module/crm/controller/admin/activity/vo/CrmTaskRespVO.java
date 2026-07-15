package com.meession.etm.module.crm.controller.admin.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmTaskRespVO {
    private Long id;
    private Integer bizType;
    private Long bizId;
    private Long sourceClueId;
    private Integer type;
    private String title;
    private String description;
    private Integer priority;
    private Integer status;
    private Long assigneeUserId;
    private String assigneeUserName;
    private LocalDateTime dueTime;
    private LocalDateTime remindTime;
    private Boolean notifySystem;
    private Boolean notifyEmail;
    private Boolean notifySms;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private String result;
    private String creator;
    private String creatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
