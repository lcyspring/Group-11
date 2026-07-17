package com.meession.etm.module.infra.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 通知配置 Response VO")
@Data
public class NotificationConfigRespVO {

    @Schema(description = "是否启用邮件通知", example = "true")
    private Boolean emailEnabled;

    @Schema(description = "是否启用短信通知", example = "true")
    private Boolean smsEnabled;

    @Schema(description = "是否启用站内信通知", example = "true")
    private Boolean inAppEnabled;

    @Schema(description = "邮件 SMTP 主机", example = "smtp.example.com")
    private String emailSmtpHost;

    @Schema(description = "邮件 SMTP 端口", example = "465")
    private Integer emailSmtpPort;

    @Schema(description = "邮件用户名", example = "user@example.com")
    private String emailUsername;

    @Schema(description = "短信服务商", example = "aliyun")
    private String smsProvider;

    @Schema(description = "短信 API Key", example = "xxxxxxxx")
    private String smsApiKey;

}
