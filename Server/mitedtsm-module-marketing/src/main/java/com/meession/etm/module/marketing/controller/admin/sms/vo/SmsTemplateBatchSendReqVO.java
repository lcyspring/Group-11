package com.meession.etm.module.marketing.controller.admin.sms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 短信批量发送 Request VO")
@Data
public class SmsTemplateBatchSendReqVO {

    @Schema(description = "手机号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"13800138000\",\"13900139000\"]")
    @NotEmpty(message = "手机号列表不能为空")
    private List<String> mobiles;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "sms_marketing_01")
    @NotEmpty(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "模板参数", example = "{\"name\":\"张三\",\"product\":\"双十一特惠\"}")
    private Map<String, Object> templateParams;

}
