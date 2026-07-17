package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 工单效率分析 Response VO")
@Data
public class WorkOrderEfficiencyRespVO {

    @Schema(description = "平均处理时长（小时）", example = "12.5")
    private Double avgProcessingHours;

    @Schema(description = "中位处理时长（小时）", example = "8.0")
    private Double medianProcessingHours;

    @Schema(description = "最短处理时长（小时）", example = "0.5")
    private Double minProcessingHours;

    @Schema(description = "最长处理时长（小时）", example = "72.0")
    private Double maxProcessingHours;

    @Schema(description = "已完结工单数", example = "80")
    private Long completedCount;

    @Schema(description = "按时完成数（在预计时间内完成）", example = "60")
    private Long onTimeCount;

    @Schema(description = "超时完成数", example = "20")
    private Long delayedCount;

    @Schema(description = "按时完成率", example = "75.0")
    private Double onTimeRate;

    @Schema(description = "各处理人平均处理时长（小时）", example = "{\"张三\":10.0,\"李四\":15.5}")
    private Map<String, Double> avgProcessingHoursByHandler;

    @Schema(description = "各优先级平均处理时长（小时）", example = "{\"低\":5.0,\"中\":10.0,\"高\":18.0,\"紧急\":24.0}")
    private Map<String, Double> avgProcessingHoursByPriority;

}
