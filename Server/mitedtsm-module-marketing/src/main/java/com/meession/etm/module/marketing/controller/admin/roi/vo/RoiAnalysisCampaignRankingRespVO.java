package com.meession.etm.module.marketing.controller.admin.roi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销活动 ROI 排行 Response VO")
@Data
public class RoiAnalysisCampaignRankingRespVO {

    @Schema(description = "活动编号", example = "1001")
    private Long campaignId;

    @Schema(description = "活动名称", example = "暑期促销活动")
    private String campaignName;

    @Schema(description = "成本", example = "10000.00")
    private BigDecimal cost;

    @Schema(description = "收入", example = "60000.00")
    private BigDecimal revenue;

    @Schema(description = "ROI(%)", example = "500.00")
    private BigDecimal roi;

    @Schema(description = "ROAS", example = "6.00")
    private BigDecimal roas;

    @Schema(description = "线索数量", example = "300")
    private Long leadCount;

    @Schema(description = "成交数量", example = "20")
    private Long dealCount;

}
