package com.meession.etm.module.crm.controller.admin.statistics.vo.funnel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CRM 商机阶段漏斗 Response VO")
@Data
public class CrmStatisticsBusinessStageSummaryRespVO {

    @Schema(description = "商机阶段编号；终态结果为空", example = "1")
    private Long statusId;

    @Schema(description = "商机阶段名称；终态结果为空", example = "方案确认")
    private String statusName;

    @Schema(description = "阶段排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer sort;

    @Schema(description = "结束状态；阶段为空，结果为 1 赢单、2 输单、3 无效", example = "2")
    private Integer endStatus;

    @Schema(description = "当前处于本状态的商机数", requiredMode = Schema.RequiredMode.REQUIRED, example = "12")
    private Long businessCount;

    @Schema(description = "当前处于本状态的商机金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalPrice;

    @Schema(description = "阶段相对上一阶段的转化率，或终态在全部结单中的占比，百分比", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "75.00")
    private BigDecimal conversionRate;

}
