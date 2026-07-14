package com.meession.etm.module.crm.controller.admin.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrmWorkOrderRespVO {
    private Long id;
    private String no;
    private String title;
    private Integer type;
    private Integer priority;
    private Integer status;
    private Long customerId;
    private String customerName;
    private Integer sourceType;
    private Long sourceId;
    private Long handlerUserId;
    private String handlerUserName;
    private String description;
    private String solution;
    private List<String> attachmentUrls;
    private LocalDateTime processTime;
    private LocalDateTime completeTime;
    private String returnReason;
    private String creator;
    private String creatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<CrmWorkOrderRecordRespVO> records;
}
