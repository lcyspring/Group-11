package com.meession.etm.module.marketing.controller.admin.sendanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销发送分析趋势 Response VO")
@Data
public class SendAnalysisTrendRespVO {

    @Schema(description = "日期", example = "2026-07-01")
    private String date;

    @Schema(description = "发送总数", example = "1000")
    private Long totalCount;

    @Schema(description = "成功数量", example = "930")
    private Long successCount;

    @Schema(description = "失败数量", example = "70")
    private Long failCount;

    @Schema(description = "待回执数量", example = "0")
    private Long pendingCount;

    @Schema(description = "到达率/成功率(%)", example = "93.00")
    private BigDecimal deliveryRate;

}
