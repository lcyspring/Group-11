package com.meession.etm.module.marketing.controller.admin.effect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销活动效果排行 Response VO")
@Data
public class CampaignEffectRankingRespVO {

    @Schema(description = "活动编号", example = "1")
    private Long campaignId;

    @Schema(description = "活动名称", example = "暑期促销")
    private String campaignName;

    @Schema(description = "活动类型", example = "1")
    private Integer type;

    @Schema(description = "发送渠道", example = "SMS")
    private String channel;

    @Schema(description = "发送数量", example = "3000")
    private Long sendCount;

    @Schema(description = "成功触达数量", example = "2800")
    private Long deliveryCount;

    @Schema(description = "失败数量", example = "200")
    private Long failCount;

    @Schema(description = "触达率(%)", example = "93.33")
    private BigDecimal deliveryRate;

    @Schema(description = "线索数量，当前预留", example = "0")
    private Long leadCount;

    @Schema(description = "成交数量，当前预留", example = "0")
    private Long dealCount;

    @Schema(description = "成交转化率(%)，当前预留", example = "0.00")
    private BigDecimal conversionRate;

}
