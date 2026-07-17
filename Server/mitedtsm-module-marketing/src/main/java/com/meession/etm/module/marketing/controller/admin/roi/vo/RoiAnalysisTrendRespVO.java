package com.meession.etm.module.marketing.controller.admin.roi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销 ROI 分析趋势 Response VO")
@Data
public class RoiAnalysisTrendRespVO {

    @Schema(description = "日期", example = "2026-07-01")
    private String date;

    @Schema(description = "成本", example = "2000.00")
    private BigDecimal cost;

    @Schema(description = "收入", example = "8000.00")
    private BigDecimal revenue;

    @Schema(description = "ROI(%)", example = "300.00")
    private BigDecimal roi;

    @Schema(description = "ROAS", example = "4.00")
    private BigDecimal roas;

    @Schema(description = "线索数量", example = "50")
    private Long leadCount;

    @Schema(description = "成交数量", example = "4")
    private Long dealCount;

}
