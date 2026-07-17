package com.meession.etm.module.marketing.controller.admin.effect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销活动效果趋势 Response VO")
@Data
public class CampaignEffectTrendRespVO {

    @Schema(description = "日期", example = "2026-07-01")
    private String date;

    @Schema(description = "活动数量", example = "3")
    private Long campaignCount;

    @Schema(description = "发送数量", example = "1000")
    private Long sendCount;

    @Schema(description = "成功触达数量", example = "930")
    private Long deliveryCount;

    @Schema(description = "失败数量", example = "70")
    private Long failCount;

    @Schema(description = "触达率(%)", example = "93.00")
    private BigDecimal deliveryRate;

}
