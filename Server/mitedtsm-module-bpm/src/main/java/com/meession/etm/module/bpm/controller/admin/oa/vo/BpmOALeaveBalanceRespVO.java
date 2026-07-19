package com.meession.etm.module.bpm.controller.admin.oa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "管理后台 - 请假余额 Response VO")
@Data
@AllArgsConstructor
public class BpmOALeaveBalanceRespVO {
    private Integer leaveType;
    private Integer year;
    private Long totalDays;
    private Long reservedDays;
    private Long usedDays;
    private Long availableDays;
    private Boolean balanceRequired;
}
