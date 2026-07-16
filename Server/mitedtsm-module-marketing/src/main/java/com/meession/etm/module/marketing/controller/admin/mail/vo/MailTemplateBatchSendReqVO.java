package com.meession.etm.module.marketing.controller.admin.mail.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 邮件批量发送 Request VO")
@Data
public class MailTemplateBatchSendReqVO {

    @Schema(description = "收件邮箱列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"user1@example.com\",\"user2@example.com\"]")
    @NotEmpty(message = "收件邮箱列表不能为空")
    private List<String> toMails;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "mail_marketing_01")
    @NotEmpty(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "模板参数", example = "{\"name\":\"张三\",\"product\":\"双十一特惠\"}")
    private Map<String, Object> templateParams;

}
