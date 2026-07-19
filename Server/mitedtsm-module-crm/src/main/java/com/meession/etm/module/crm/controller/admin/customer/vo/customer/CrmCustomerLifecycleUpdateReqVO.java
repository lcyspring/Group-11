package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerLifecycleStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户生命周期状态更新 Request VO")
@Data
public class CrmCustomerLifecycleUpdateReqVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "客户编号不能为空")
    private Long id;

    @Schema(description = "生命周期状态：10 潜在、20 意向、30 成交、40 流失",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "客户生命周期状态不能为空")
    @InEnum(value = CrmCustomerLifecycleStatusEnum.class, message = "客户生命周期状态必须是 {value}")
    private Integer lifecycleStatus;

    @Schema(description = "变更原因；转为流失客户时必填", example = "客户已选择其他供应商")
    @Size(max = 500, message = "生命周期变更原因不能超过 500 个字符")
    private String reason;

}
