package com.meession.etm.module.marketing.controller.admin.sendanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销发送模板分析 Response VO")
@Data
public class SendAnalysisTemplateRespVO {

    @Schema(description = "模板编号", example = "1001")
    private Long templateId;

    @Schema(description = "发送渠道", example = "SMS")
    private String channel;

    @Schema(description = "发送总数", example = "1200")
    private Long totalCount;

    @Schema(description = "成功数量", example = "1150")
    private Long successCount;

    @Schema(description = "失败数量", example = "50")
    private Long failCount;

    @Schema(description = "到达率/成功率(%)", example = "95.83")
    private BigDecimal deliveryRate;

}
