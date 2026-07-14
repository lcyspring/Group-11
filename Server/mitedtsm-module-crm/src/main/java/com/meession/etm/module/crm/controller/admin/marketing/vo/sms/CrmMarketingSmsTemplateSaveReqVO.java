package com.meession.etm.module.crm.controller.admin.marketing.vo.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 营销短信模板创建/更新 Request VO")
@Data
public class CrmMarketingSmsTemplateSaveReqVO {

    @Schema(description = "主键", example = "1024")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "促销通知模板")
    @NotNull(message = "模板名称不能为空")
    private String name;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMOTION_NOTIFY")
    @NotNull(message = "模板编码不能为空")
    private String code;

    @Schema(description = "模板内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "尊敬的{name}，您有一份优惠待领取！")
    @NotNull(message = "模板内容不能为空")
    private String content;

    @Schema(description = "启用状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "启用状态不能为空")
    private Integer status;

    @Schema(description = "关联的营销活动编号", example = "1024")
    private Long campaignId;

    @Schema(description = "短信渠道编号", example = "1")
    private Long channelId;

    @Schema(description = "短信渠道编码", example = "ALIYUN")
    private String channelCode;

    @Schema(description = "短信 API 的模板编号", example = "SMS_123456789")
    private String apiTemplateId;

    @Schema(description = "备注", example = "促销活动短信模板")
    private String remark;

}
