package com.meession.etm.module.crm.controller.admin.statistics.vo.portrait;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户成交状态分布 Response VO")
@Data
public class CrmStatisticCustomerDealStatusRespVO {

    @Schema(description = "是否成交", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean dealStatus;

    @Schema(description = "客户数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Long customerCount;

}
