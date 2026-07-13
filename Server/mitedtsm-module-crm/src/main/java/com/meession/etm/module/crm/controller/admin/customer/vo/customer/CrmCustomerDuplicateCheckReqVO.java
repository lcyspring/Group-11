package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import com.meession.etm.framework.common.validation.Mobile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户查重 Request VO")
@Data
public class CrmCustomerDuplicateCheckReqVO {

    @Schema(description = "更新时排除的客户编号", example = "1024")
    private Long excludeId;

    @Schema(description = "客户名称", example = "示例客户")
    private String name;

    @Schema(description = "手机", example = "18000000000")
    @Mobile
    private String mobile;

    @AssertTrue(message = "客户名称和手机至少填写一项")
    public boolean isSearchConditionPresent() {
        return name != null && !name.isBlank() || mobile != null && !mobile.isBlank();
    }

}
