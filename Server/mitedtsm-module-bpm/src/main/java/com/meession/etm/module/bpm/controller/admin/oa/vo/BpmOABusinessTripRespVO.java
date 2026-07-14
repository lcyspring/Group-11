package com.meession.etm.module.bpm.controller.admin.oa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 出差申请 Response VO")
@Data
public class BpmOABusinessTripRespVO {

    @Schema(description = "出差表单主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "出差目的地", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京")
    private String destination;

    @Schema(description = "出差原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户拜访")
    private String reason;

    @Schema(description = "申请时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "出差开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @Schema(description = "出差结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;

    @Schema(description = "出差天数", example = "3")
    private Long days;

    @Schema(description = "预算金额", example = "1000.00")
    private BigDecimal budget;

    @Schema(description = "流程编号")
    private String processInstanceId;

    @Schema(description = "审批结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

}