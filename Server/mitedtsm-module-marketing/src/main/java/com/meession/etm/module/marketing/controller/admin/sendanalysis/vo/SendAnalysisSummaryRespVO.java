package com.meession.etm.module.marketing.controller.admin.sendanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销发送分析概览 Response VO")
@Data
public class SendAnalysisSummaryRespVO {

    @Schema(description = "活动数量", example = "10")
    private Long campaignCount;

    @Schema(description = "发送总数", example = "10000")
    private Long totalCount;

    @Schema(description = "成功数量", example = "9200")
    private Long successCount;

    @Schema(description = "失败数量", example = "600")
    private Long failCount;

    @Schema(description = "待回执数量", example = "200")
    private Long pendingCount;

    @Schema(description = "到达率/成功率(%)", example = "92.00")
    private BigDecimal deliveryRate;

    @Schema(description = "失败率(%)", example = "6.00")
    private BigDecimal failRate;

}
