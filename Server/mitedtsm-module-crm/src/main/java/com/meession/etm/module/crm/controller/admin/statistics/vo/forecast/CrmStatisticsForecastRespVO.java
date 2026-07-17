package com.meession.etm.module.crm.controller.admin.statistics.vo.forecast;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - CRM 销售预测 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmStatisticsForecastRespVO {

    @Schema(description = "预测数据列表")
    private List<ForecastItem> forecastData;

    @Schema(description = "总预测金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalForecastAmount;

    @Schema(description = "总实际金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalActualAmount;

    @Schema(description = "预测数据项")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastItem {

        @Schema(description = "时间", requiredMode = Schema.RequiredMode.REQUIRED)
        private String time;

        @Schema(description = "预测金额", requiredMode = Schema.RequiredMode.REQUIRED)
        private BigDecimal forecastAmount;

        @Schema(description = "实际金额", requiredMode = Schema.RequiredMode.REQUIRED)
        private BigDecimal actualAmount;

    }

}