package com.meession.etm.module.crm.controller.admin.workorder.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CrmWorkOrderCheckInRespVO {
    private Long id;
    private Long workOrderId;
    private Long userId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracyMeters;
    private BigDecimal distanceMeters;
    private LocalDateTime createTime;
}
