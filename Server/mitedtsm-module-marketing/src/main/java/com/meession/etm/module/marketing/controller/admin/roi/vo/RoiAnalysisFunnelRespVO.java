package com.meession.etm.module.marketing.controller.admin.roi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销 ROI 转化漏斗 Response VO")
@Data
public class RoiAnalysisFunnelRespVO {

    @Schema(description = "发送数量", example = "10000")
    private Long sendCount;

    @Schema(description = "触达数量", example = "9200")
    private Long deliveryCount;

    @Schema(description = "线索数量", example = "1200")
    private Long leadCount;

    @Schema(description = "客户数量", example = "300")
    private Long customerCount;

    @Schema(description = "商机数量", example = "100")
    private Long opportunityCount;

    @Schema(description = "成交数量", example = "80")
    private Long dealCount;

    @Schema(description = "触达率(%)", example = "92.00")
    private BigDecimal deliveryRate;

    @Schema(description = "线索转化率(%) = 线索数 / 触达数 * 100", example = "13.04")
    private BigDecimal leadConversionRate;

    @Schema(description = "客户转化率(%) = 客户数 / 线索数 * 100", example = "25.00")
    private BigDecimal customerConversionRate;

    @Schema(description = "成交转化率(%) = 成交数 / 客户数 * 100", example = "26.67")
    private BigDecimal dealConversionRate;

}
