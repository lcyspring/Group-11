package com.meession.etm.module.crm.controller.admin.clue.vo;

import com.meession.etm.framework.common.validation.Mobile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - CRM 线索转客户 Request VO")
@Data
public class CrmClueTransformReqVO {

    @Schema(description = "线索编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "线索编号不能为空")
    private Long id;

    @Schema(description = "首联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "首联系人姓名不能为空")
    @Size(max = 255, message = "首联系人姓名长度不能超过 255 个字符")
    private String contactName;

    @Schema(description = "首联系人手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "首联系人手机号不能为空")
    @Mobile
    private String contactMobile;

}
