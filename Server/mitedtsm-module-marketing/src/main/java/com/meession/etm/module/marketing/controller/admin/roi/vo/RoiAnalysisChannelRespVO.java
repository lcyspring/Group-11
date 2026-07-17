package com.meession.etm.module.marketing.controller.admin.roi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销渠道 ROI 分析 Response VO")
@Data
public class RoiAnalysisChannelRespVO {

    @Schema(description = "渠道", example = "SMS")
    private String channel;

    @Schema(description = "成本", example = "12000.00")
    private BigDecimal cost;

    @Schema(description = "收入", example = "50000.00")
    private BigDecimal revenue;

    @Schema(description = "ROI(%)", example = "316.67")
    private BigDecimal roi;

    @Schema(description = "ROAS", example = "4.17")
    private BigDecimal roas;

    @Schema(description = "线索数量", example = "400")
    private Long leadCount;

    @Schema(description = "成交数量", example = "30")
    private Long dealCount;

}
