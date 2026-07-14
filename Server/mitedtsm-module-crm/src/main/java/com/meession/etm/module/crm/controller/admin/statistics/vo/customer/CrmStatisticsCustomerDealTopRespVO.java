package com.meession.etm.module.crm.controller.admin.statistics.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CRM 客户成交金额排行 Response VO")
@Data
public class CrmStatisticsCustomerDealTopRespVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long customerId;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "重点客户")
    private String customerName;

    @Schema(description = "审批通过合同数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Long contractCount;

    @Schema(description = "成交金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100000.00")
    private BigDecimal contractAmount;

}
