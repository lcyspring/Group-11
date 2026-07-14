package com.meession.etm.module.crm.controller.admin.statistics.vo.performance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - CRM 年度业绩目标完成度汇总 Response VO")
@Data
public class CrmStatisticsTargetCompletionSummaryRespVO {

    private Integer targetType;
    private String annualTarget;
    private String annualActual;
    @Schema(description = "年度完成率；年度目标为 0 时为空")
    private BigDecimal annualCompletionRate;
    private List<CrmStatisticsTargetCompletionRespVO> monthlyList;

}
