package com.meession.etm.module.marketing.controller.admin.roi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 营销 ROI 分析 Request VO")
@Data
public class RoiAnalysisReqVO {

    @Schema(description = "营销活动编号", example = "1")
    private Long campaignId;

    @Schema(description = "发送渠道", example = "SMS", allowableValues = {"SMS", "MAIL", "OTHER"})
    private String channel;

    @Schema(description = "统计日期", example = "[2026-07-01, 2026-07-31]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] statDate;

}
