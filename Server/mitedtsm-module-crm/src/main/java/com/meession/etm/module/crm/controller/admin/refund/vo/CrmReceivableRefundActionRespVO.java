package com.meession.etm.module.crm.controller.admin.refund.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmReceivableRefundActionRespVO {
    private Long id;
    private Integer actionType;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorUserId;
    private String operatorUserName;
    private LocalDateTime actionTime;
    private String processInstanceId;
    private String remark;
}
