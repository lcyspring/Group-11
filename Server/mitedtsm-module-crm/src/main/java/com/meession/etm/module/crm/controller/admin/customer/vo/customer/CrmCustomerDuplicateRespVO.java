package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - CRM 疑似重复客户 Response VO")
@Data
public class CrmCustomerDuplicateRespVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "示例客户")
    private String name;

    @Schema(description = "手机", example = "18000000000")
    private String mobile;

}
