package com.meession.etm.module.bpm.controller.admin.oa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - OA工作报告更新 Request VO")
@Data
public class BpmOAWorkReportUpdateReqVO {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "报告类型(daily-日报/weekly-周报/monthly-月报)", example = "daily")
    private String type;

    @Schema(description = "报告日期", example = "2026-07-16")
    private LocalDate reportDate;

    @Schema(description = "工作内容", example = "完成了客户拜访")
    private String content;

    @Schema(description = "明日计划", example = "继续跟进项目")
    private String tomorrowPlan;

    @Schema(description = "遇到的问题", example = "暂无")
    private String problems;

}