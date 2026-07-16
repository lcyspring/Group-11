package com.meession.etm.module.bpm.controller.admin.oa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - OA工作报告创建 Request VO")
@Data
public class BpmOAWorkReportCreateReqVO {

    @Schema(description = "报告类型(daily-日报/weekly-周报/monthly-月报)", requiredMode = Schema.RequiredMode.REQUIRED, example = "daily")
    @NotBlank(message = "报告类型不能为空")
    private String type;

    @Schema(description = "报告日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-16")
    @NotNull(message = "报告日期不能为空")
    private LocalDate reportDate;

    @Schema(description = "工作内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "完成了客户拜访")
    @NotBlank(message = "工作内容不能为空")
    private String content;

    @Schema(description = "明日计划", example = "继续跟进项目")
    private String tomorrowPlan;

    @Schema(description = "遇到的问题", example = "暂无")
    private String problems;

}