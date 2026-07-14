package com.meession.etm.module.crm.controller.admin.statistics.vo.funnel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CRM 商机阶段漏斗 Response VO")
@Data
public class CrmStatisticsBusinessStageSummaryRespVO {

    @Schema(description = "商机阶段编号；赢单阶段为空", example = "1")
    private Long statusId;

    @Schema(description = "商机阶段名称；赢单阶段为空", example = "方案确认")
    private String statusName;

    @Schema(description = "阶段排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer sort;

    @Schema(description = "结束状态；仅赢单阶段为 1", example = "1")
    private Integer endStatus;

    @Schema(description = "到达本阶段的商机数", requiredMode = Schema.RequiredMode.REQUIRED, example = "12")
    private Long businessCount;

    @Schema(description = "到达本阶段的商机金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalPrice;

    @Schema(description = "相对上一阶段的转化率，百分比", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "75.00")
    private BigDecimal conversionRate;

}
