package com.meession.etm.module.marketing.controller.admin.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 营销发送记录 Response VO")
@Data
public class SendLogRespVO {

    @Schema(description = "记录编号", example = "1024")
    private Long id;

    @Schema(description = "营销活动编号", example = "1")
    private Long campaignId;

    @Schema(description = "发送渠道", example = "SMS")
    private String channel;

    @Schema(description = "系统日志编号(SmsLog.id 或 MailLog.id)", example = "1549")
    private Long systemLogId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
