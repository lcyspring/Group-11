package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 工单趋势分析 Response VO")
@Data
public class WorkOrderTrendRespVO {

    @Schema(description = "每日趋势数据")
    private List<TrendItem> dailyTrends;

    @Schema(description = "趋势数据项")
    @Data
    public static class TrendItem {

        @Schema(description = "日期", example = "2026-07-01")
        private String date;

        @Schema(description = "新增工单数", example = "5")
        private Long newCount;

        @Schema(description = "处理中工单数", example = "3")
        private Long processingCount;

        @Schema(description = "完结工单数", example = "4")
        private Long completedCount;

        @Schema(description = "退回工单数", example = "1")
        private Long returnedCount;

    }

}
