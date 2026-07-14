package com.meession.etm.module.crm.controller.admin.marketing.vo.mail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - CRM 营销邮件发送 Request VO")
@Data
public class CrmMarketingMailTemplateSendReqVO {

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMOTION_MAIL")
    @NotNull(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "收件人邮箱列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"user1@example.com\", \"user2@example.com\"]")
    @NotNull(message = "收件人邮箱列表不能为空")
    private List<String> toMails;

    @Schema(description = "抄送邮箱列表", example = "[\"cc@example.com\"]")
    private List<String> ccMails;

    @Schema(description = "密送邮箱列表", example = "[\"bcc@example.com\"]")
    private List<String> bccMails;

    @Schema(description = "模板参数", example = "{\"name\": \"张三\"}")
    private Map<String, Object> templateParams;

}
