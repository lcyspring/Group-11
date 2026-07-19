package com.meession.etm.module.bpm.controller.admin.oa.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BpmOATripRespVO {
    private Long id;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal days;
    private String destination;
    private String reason;
    private BigDecimal estimatedExpense;
    private List<Long> companionUserIds;
    private List<String> attachmentUrls;
    private Integer status;
    private String processInstanceId;
    private LocalDateTime approvalTime;
    private LocalDateTime createTime;
}
