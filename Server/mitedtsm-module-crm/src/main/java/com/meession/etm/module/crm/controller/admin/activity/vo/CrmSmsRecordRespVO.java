package com.meession.etm.module.crm.controller.admin.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmSmsRecordRespVO {
    private Long id;
    private Integer bizType;
    private Long bizId;
    private Long sourceClueId;
    private Long contactId;
    private Integer direction;
    private Integer status;
    private String mobile;
    private String content;
    private Long systemSmsLogId;
    private String externalMessageId;
    private String failureReason;
    private LocalDateTime occurredTime;
    private Long operatorUserId;
    private String operatorUserName;
    private LocalDateTime createTime;
}
