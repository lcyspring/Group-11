package com.meession.etm.module.crm.controller.admin.statistics.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - CRM 统计指标目录 Response VO")
@Data
@Accessors(chain = true)
public class CrmStatisticsMetadataCatalogRespVO {

    @Schema(description = "目录生成时间")
    private LocalDateTime generatedAt;

    @Schema(description = "指标域", example = "funnel")
    private String scope;

    @Schema(description = "刷新模式", example = "REALTIME_QUERY")
    private String refreshMode;

    @Schema(description = "权限模式", example = "CRM_DATA_SCOPE")
    private String permissionMode;

    @Schema(description = "历史重算边界")
    private String historyRecalculation;

    @Schema(description = "对账边界")
    private String reconciliation;

    @Schema(description = "指标血缘")
    private List<CrmStatisticsMetricMetadataRespVO> metrics;
}
