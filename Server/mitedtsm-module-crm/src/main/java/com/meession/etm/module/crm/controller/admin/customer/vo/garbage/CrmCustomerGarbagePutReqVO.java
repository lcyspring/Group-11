package com.meession.etm.module.crm.controller.admin.customer.vo.garbage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户转入垃圾池 Request VO")
@Data
public class CrmCustomerGarbagePutReqVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "转入原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "转入垃圾池原因不能为空")
    @Size(max = 500, message = "转入垃圾池原因不能超过 500 个字符")
    private String reason;
}
