package com.meession.etm.module.marketing.controller.admin.roi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销 ROI 分析概览 Response VO")
@Data
public class RoiAnalysisSummaryRespVO {

    @Schema(description = "活动数量", example = "10")
    private Long campaignCount;

    @Schema(description = "总成本", example = "50000.00")
    private BigDecimal totalCost;

    @Schema(description = "总收入", example = "180000.00")
    private BigDecimal totalRevenue;

    @Schema(description = "毛收益", example = "130000.00")
    private BigDecimal grossProfit;

    @Schema(description = "ROI(%) = (收入 - 成本) / 成本 * 100", example = "260.00")
    private BigDecimal roi;

    @Schema(description = "ROAS = 收入 / 成本", example = "3.60")
    private BigDecimal roas;

    @Schema(description = "线索数量", example = "1200")
    private Long leadCount;

    @Schema(description = "客户数量", example = "300")
    private Long customerCount;

    @Schema(description = "商机数量", example = "100")
    private Long opportunityCount;

    @Schema(description = "成交数量", example = "80")
    private Long dealCount;

    @Schema(description = "成交转化率(%) = 成交数 / 线索数 * 100", example = "6.67")
    private BigDecimal conversionRate;

    @Schema(description = "单线索成本", example = "41.67")
    private BigDecimal costPerLead;

    @Schema(description = "单成交成本", example = "625.00")
    private BigDecimal costPerDeal;

}
