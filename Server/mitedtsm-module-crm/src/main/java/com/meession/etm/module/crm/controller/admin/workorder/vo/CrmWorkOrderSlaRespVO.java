package com.meession.etm.module.crm.controller.admin.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmWorkOrderSlaRespVO {
    private Long id;
    private Long workOrderId;
    private Long policyId;
    private String policyCode;
    private String policyName;
    private LocalDateTime responseDueTime;
    private LocalDateTime escalationDueTime;
    private LocalDateTime resolutionDueTime;
    private Long pausedSeconds;
    private LocalDateTime pausedAt;
    private Integer status;
    private LocalDateTime escalatedAt;
    private LocalDateTime completedAt;
    private boolean paused;
    private boolean overdue;
}
