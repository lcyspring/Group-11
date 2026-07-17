package com.meession.etm.module.marketing.controller.admin.effect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销活动转化漏斗 Response VO")
@Data
public class CampaignEffectFunnelRespVO {

    @Schema(description = "发送数量", example = "10000")
    private Long sendCount;

    @Schema(description = "成功触达数量", example = "9200")
    private Long deliveryCount;

    @Schema(description = "互动/响应数量，当前预留", example = "0")
    private Long responseCount;

    @Schema(description = "线索数量，当前预留", example = "0")
    private Long leadCount;

    @Schema(description = "客户数量，当前预留", example = "0")
    private Long customerCount;

    @Schema(description = "商机数量，当前预留", example = "0")
    private Long opportunityCount;

    @Schema(description = "成交数量，当前预留", example = "0")
    private Long dealCount;

    @Schema(description = "触达率(%)", example = "92.00")
    private BigDecimal deliveryRate;

    @Schema(description = "线索转化率(%)，当前预留", example = "0.00")
    private BigDecimal leadConversionRate;

    @Schema(description = "成交转化率(%)，当前预留", example = "0.00")
    private BigDecimal dealConversionRate;

}
