package com.meession.etm.module.crm.controller.admin.statistics.vo.funnel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CRM 销售预测按日期 Response VO")
@Data
public class CrmStatisticsBusinessForecastByDateRespVO {

    @Schema(description = "时间轴", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08")
    private String time;

    @Schema(description = "活跃商机数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long businessCount;

    @Schema(description = "预计成交金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100000.00")
    private BigDecimal expectedAmount;

    @Schema(description = "概率金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "35000.00")
    private BigDecimal weightedAmount;

}
