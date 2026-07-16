package com.meession.etm.module.bpm.controller.admin.oa.vo;

import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAWorkReportDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA工作报告 Response VO")
@Data
public class BpmOAWorkReportRespVO {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
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

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2026-07-16 10:00:00")
    private LocalDateTime createTime;

    public static BpmOAWorkReportRespVO build(BpmOAWorkReportDO report) {
        return BeanUtils.toBean(report, BpmOAWorkReportRespVO.class);
    }

}