package com.meession.etm.module.crm.controller.admin.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmTaskActionRecordRespVO {
    private Long id;
    private Long taskId;
    private Integer actionType;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorUserId;
    private String operatorUserName;
    private String remark;
    private LocalDateTime createTime;
}
