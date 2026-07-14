package com.meession.etm.module.crm.controller.admin.marketing.vo.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - CRM 营销短信发送 Request VO")
@Data
public class CrmMarketingSmsTemplateSendReqVO {

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMOTION_NOTIFY")
    @NotNull(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "手机号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"13800138000\", \"13900139000\"]")
    @NotNull(message = "手机号列表不能为空")
    private List<String> mobiles;

    @Schema(description = "模板参数", example = "{\"name\": \"张三\"}")
    private Map<String, Object> templateParams;

}
