package com.meession.etm.module.crm.controller.admin.statistics.vo.performance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - CRM 年度业绩目标 Response VO")
@Data
public class CrmPerformanceTargetRespVO {

    private Integer scopeType;
    private Long scopeId;
    private Integer targetYear;
    private Integer targetType;
    private List<String> monthlyTargets;
    private List<String> quarterlyTargets;
    private String annualTarget;

}
