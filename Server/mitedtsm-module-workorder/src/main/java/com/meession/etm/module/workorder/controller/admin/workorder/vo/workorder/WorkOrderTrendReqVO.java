package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工单趋势分析 Request VO")
@Data
public class WorkOrderTrendReqVO {

    @Schema(description = "开始时间", example = "2026-07-01T00:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-07-31T23:59:59")
    private LocalDateTime endTime;

}
