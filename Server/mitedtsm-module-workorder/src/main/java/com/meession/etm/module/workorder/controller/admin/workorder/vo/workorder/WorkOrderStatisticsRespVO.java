package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 工单统计 Response VO")
@Data
public class WorkOrderStatisticsRespVO {

    @Schema(description = "工单总数", example = "128")
    private Long totalCount;

    @Schema(description = "今日新增", example = "5")
    private Long todayNewCount;

    @Schema(description = "今日完结", example = "3")
    private Long todayCompletedCount;

    @Schema(description = "各状态工单数量统计", example = "{\"待处理\":10,\"处理中\":25,\"已完成\":80,\"已关闭\":8,\"已退回\":5}")
    private Map<String, Long> statusDistribution;

    @Schema(description = "各优先级工单数量统计", example = "{\"低\":20,\"中\":50,\"高\":40,\"紧急\":18}")
    private Map<String, Long> priorityDistribution;

    @Schema(description = "各工单类型数量统计", example = "{\"故障报修\":30,\"需求变更\":15}")
    private Map<String, Long> typeDistribution;

}
