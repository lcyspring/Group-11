package com.meession.etm.module.crm.controller.admin.statistics.vo.portrait;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户状态分析 VO")
@Data
public class CrmStatisticCustomerStatusRespVO {

    @Schema(description = "客户状态编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "客户个数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer customerCount;

    @Schema(description = "成交个数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer dealCount;

}