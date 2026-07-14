package com.meession.etm.module.crm.controller.admin.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmWorkOrderRecordRespVO {
    private Long id;
    private Integer actionType;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorUserId;
    private String operatorUserName;
    private Long handlerUserId;
    private String handlerUserName;
    private String remark;
    private LocalDateTime createTime;
}
