package com.meession.etm.module.crm.controller.admin.statistics.vo.funnel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CRM 销售预测按日期 Response VO")
@Data
public class CrmStatisticsBusinessForecastByDateRespVO {

    @Schema(description = "时间轴", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08")
    private String time;

    @Schema(description = "预测商机数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long forecastBusinessCount;

    @Schema(description = "实际赢单商机数", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    private Long actualBusinessCount;

    @Schema(description = "预测金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100000.00")
    private BigDecimal forecastAmount;

    @Schema(description = "实际赢单金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "45000.00")
    private BigDecimal actualAmount;

}
