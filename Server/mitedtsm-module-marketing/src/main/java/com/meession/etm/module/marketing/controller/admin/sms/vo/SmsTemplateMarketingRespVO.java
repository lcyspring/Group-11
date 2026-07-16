package com.meession.etm.module.marketing.controller.admin.sms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 营销短信模板 Response VO")
@Data
public class SmsTemplateMarketingRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "短信类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer type;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "sms_marketing_01")
    private String code;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "双十一促销模板")
    private String name;

    @Schema(description = "模板内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "你好，{name}。双十一大促！")
    private String content;

    @Schema(description = "参数数组", example = "[\"name\",\"product\"]")
    private List<String> params;

    @Schema(description = "备注", example = "营销用模板")
    private String remark;

    @Schema(description = "短信 API 的模板编号", example = "4383920")
    private String apiTemplateId;

    @Schema(description = "短信渠道编号", example = "10")
    private Long channelId;

    @Schema(description = "短信渠道编码", example = "ALIYUN")
    private String channelCode;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
