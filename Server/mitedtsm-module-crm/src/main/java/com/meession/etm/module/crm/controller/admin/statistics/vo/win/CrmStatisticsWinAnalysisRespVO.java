package com.meession.etm.module.crm.controller.admin.statistics.vo.win;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - CRM 商机赢单分析 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmStatisticsWinAnalysisRespVO {

    @Schema(description = "赢单率", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal winRate;

    @Schema(description = "总商机数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long totalBusinessCount;

    @Schema(description = "赢单商机数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long winBusinessCount;

    @Schema(description = "赢单总金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalWinAmount;

    @Schema(description = "平均赢单金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal avgWinAmount;

    @Schema(description = "按日期统计的赢单率趋势")
    private List<WinRateByDateItem> winRateByDate;

    @Schema(description = "按行业统计的赢单金额分布")
    private List<WinAmountByIndustryItem> winAmountByIndustry;

    @Schema(description = "按客户等级统计的赢单金额分布")
    private List<WinAmountByCustomerLevelItem> winAmountByCustomerLevel;

    @Schema(description = "按日期统计的赢单率")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WinRateByDateItem {

        @Schema(description = "时间", requiredMode = Schema.RequiredMode.REQUIRED)
        private String time;

        @Schema(description = "商机数", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long businessCount;

        @Schema(description = "赢单数", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long winCount;

        @Schema(description = "赢单率", requiredMode = Schema.RequiredMode.REQUIRED)
        private BigDecimal winRate;

    }

    @Schema(description = "按行业统计的赢单金额")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WinAmountByIndustryItem {

        @Schema(description = "行业", requiredMode = Schema.RequiredMode.REQUIRED)
        private String industry;

        @Schema(description = "赢单金额", requiredMode = Schema.RequiredMode.REQUIRED)
        private BigDecimal winAmount;

        @Schema(description = "赢单数", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long winCount;

    }

    @Schema(description = "按客户等级统计的赢单金额")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WinAmountByCustomerLevelItem {

        @Schema(description = "客户等级", requiredMode = Schema.RequiredMode.REQUIRED)
        private String customerLevel;

        @Schema(description = "赢单金额", requiredMode = Schema.RequiredMode.REQUIRED)
        private BigDecimal winAmount;

        @Schema(description = "赢单数", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long winCount;

    }

}