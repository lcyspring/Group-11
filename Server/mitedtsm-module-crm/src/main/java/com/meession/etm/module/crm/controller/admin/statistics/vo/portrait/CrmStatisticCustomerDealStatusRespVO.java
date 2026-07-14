package com.meession.etm.module.crm.controller.admin.statistics.vo.portrait;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户生命周期状态分布 Response VO")
@Data
public class CrmStatisticCustomerDealStatusRespVO {

    @Schema(description = "生命周期状态：10 潜在、20 意向、30 成交、40 流失",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer lifecycleStatus;

    @Schema(description = "客户数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Long customerCount;

}
