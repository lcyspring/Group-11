package com.meession.etm.module.crm.controller.admin.statistics.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.Accessors;
import lombok.Data;

@Schema(description = "CRM 工单统计汇总 Response VO")
@Data
@Accessors(chain = true)
public class CrmStatisticsWorkOrderSummaryRespVO {
    private Long totalCount;
    private Long pendingCount;
    private Long processingCount;
    private Long completedCount;
    private Long returnedCount;
    private String completionRate;
}
