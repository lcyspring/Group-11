package com.meession.etm.module.bpm.controller.admin.oa.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Schema(description = "管理后台 - OA工作报告分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmOAWorkReportPageReqVO extends PageParam {

    @Schema(description = "报告类型", example = "daily")
    private String type;

    @Schema(description = "报告日期", example = "2026-07-16")
    private LocalDate reportDate;

    @Schema(description = "状态", example = "1")
    private Integer status;

}