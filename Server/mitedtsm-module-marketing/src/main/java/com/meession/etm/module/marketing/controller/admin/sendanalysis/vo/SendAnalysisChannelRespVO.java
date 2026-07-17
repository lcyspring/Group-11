package com.meession.etm.module.marketing.controller.admin.sendanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销发送渠道分析 Response VO")
@Data
public class SendAnalysisChannelRespVO {

    @Schema(description = "发送渠道", example = "SMS")
    private String channel;

    @Schema(description = "发送总数", example = "5000")
    private Long totalCount;

    @Schema(description = "成功数量", example = "4600")
    private Long successCount;

    @Schema(description = "失败数量", example = "400")
    private Long failCount;

    @Schema(description = "到达率/成功率(%)", example = "92.00")
    private BigDecimal deliveryRate;

}
