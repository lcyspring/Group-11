package com.meession.etm.module.crm.controller.admin.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

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
    private BigDecimal serviceLatitude;
    private BigDecimal serviceLongitude;
    private Integer geofenceRadiusMeters;
    private Boolean checkInRequired;
    private Long groupId;
    private String groupName;
    private Long handlerUserId;
    private String handlerUserName;
    private Integer dispatchMode;
    private LocalDateTime assignTime;
    private List<Long> ccUserIds;
    private List<String> ccUserNames;
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
    private CrmWorkOrderCheckInRespVO latestCheckIn;
    private CrmWorkOrderSlaRespVO sla;
}
