package com.meession.etm.module.marketing.controller.admin.effect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销活动渠道效果 Response VO")
@Data
public class CampaignEffectChannelRespVO {

    @Schema(description = "发送渠道", example = "SMS")
    private String channel;

    @Schema(description = "活动数量", example = "5")
    private Long campaignCount;

    @Schema(description = "发送数量", example = "5000")
    private Long sendCount;

    @Schema(description = "成功触达数量", example = "4600")
    private Long deliveryCount;

    @Schema(description = "失败数量", example = "400")
    private Long failCount;

    @Schema(description = "触达率(%)", example = "92.00")
    private BigDecimal deliveryRate;

    @Schema(description = "线索数量，当前预留", example = "0")
    private Long leadCount;

    @Schema(description = "成交数量，当前预留", example = "0")
    private Long dealCount;

}
