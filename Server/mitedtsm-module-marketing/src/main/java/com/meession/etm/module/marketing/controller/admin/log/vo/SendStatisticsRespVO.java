package com.meession.etm.module.marketing.controller.admin.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 营销发送统计 Response VO")
@Data
public class SendStatisticsRespVO {

    @Schema(description = "活动编号", example = "1")
    private Long campaignId;

    @Schema(description = "活动名称", example = "双十一促销")
    private String campaignName;

    @Schema(description = "总发送数", example = "10000")
    private Long totalSent;

    @Schema(description = "短信发送数", example = "6000")
    private Long smsSent;

    @Schema(description = "邮件发送数", example = "4000")
    private Long mailSent;

}
