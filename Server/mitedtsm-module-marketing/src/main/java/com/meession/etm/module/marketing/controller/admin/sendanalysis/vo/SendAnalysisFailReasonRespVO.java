package com.meession.etm.module.marketing.controller.admin.sendanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 营销发送失败原因分析 Response VO")
@Data
public class SendAnalysisFailReasonRespVO {

    @Schema(description = "失败原因", example = "UNKNOWN")
    private String reason;

    @Schema(description = "数量", example = "120")
    private Long count;

    @Schema(description = "占比(%)", example = "20.00")
    private BigDecimal rate;

}
