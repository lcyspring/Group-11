package com.meession.etm.module.crm.controller.admin.marketing.vo.mail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 营销邮件模板创建/更新 Request VO")
@Data
public class CrmMarketingMailTemplateSaveReqVO {

    @Schema(description = "主键", example = "1024")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "促销邮件模板")
    @NotNull(message = "模板名称不能为空")
    private String name;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMOTION_MAIL")
    @NotNull(message = "模板编码不能为空")
    private String code;

    @Schema(description = "邮件标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "尊敬的{name}，您有一份专属优惠！")
    @NotNull(message = "邮件标题不能为空")
    private String title;

    @Schema(description = "邮件内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "<p>尊敬的{name}：</p><p>您好！您有一份专属优惠待领取...</p>")
    @NotNull(message = "邮件内容不能为空")
    private String content;

    @Schema(description = "启用状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "启用状态不能为空")
    private Integer status;

    @Schema(description = "关联的营销活动编号", example = "1024")
    private Long campaignId;

    @Schema(description = "发送邮箱账号编号", example = "1")
    private Long accountId;

    @Schema(description = "发送人名称", example = "密讯科技")
    private String nickname;

    @Schema(description = "备注", example = "促销活动邮件模板")
    private String remark;

}
