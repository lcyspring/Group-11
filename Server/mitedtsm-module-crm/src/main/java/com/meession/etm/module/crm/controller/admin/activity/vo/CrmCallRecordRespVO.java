package com.meession.etm.module.crm.controller.admin.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmCallRecordRespVO {
    private Long id;
    private Integer bizType;
    private Long bizId;
    private Long sourceClueId;
    private Long contactId;
    private Integer direction;
    private Integer status;
    private String phone;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationSeconds;
    private String recordingUrl;
    private String summary;
    private Long operatorUserId;
    private String operatorUserName;
    private LocalDateTime createTime;
}
