package com.meession.etm.module.crm.controller.admin.statistics.vo.performance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CRM 月度业绩目标完成度 Response VO")
@Data
public class CrmStatisticsTargetCompletionRespVO {

    @Schema(description = "月份", example = "2026-01")
    private String time;
    @Schema(description = "目标值")
    private String targetValue;
    @Schema(description = "实际值")
    private String actualValue;
    @Schema(description = "完成率；目标为 0 时为空")
    private BigDecimal completionRate;

}
