package com.meession.etm.module.infra.controller.admin.job.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 定时任务统计 Response VO")
@Data
@Builder
public class JobStatisticsRespVO {

    @Schema(description = "任务总数", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long totalCount;

    @Schema(description = "正常运行的任务数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "80")
    private Long normalCount;

    @Schema(description = "暂停的任务数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "15")
    private Long stopCount;

    @Schema(description = "异常的任务数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Long errorCount;

}
