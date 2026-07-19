package com.meession.etm.module.crm.controller.admin.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmClueConversionRecordRespVO {
    private Long id;
    private Long clueId;
    private Long customerId;
    private Long primaryContactId;
    private Integer followUpCount;
    private Integer taskCount;
    private Integer callCount;
    private Integer smsCount;
    private Long operatorUserId;
    private String operatorUserName;
    private LocalDateTime convertedAt;
}
