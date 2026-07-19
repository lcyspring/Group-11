package com.meession.etm.module.crm.controller.admin.statistics.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - CRM 单项统计指标血缘 Response VO")
@Data
@Accessors(chain = true)
public class CrmStatisticsMetricMetadataRespVO {

    @Schema(description = "指标编码", example = "funnel.business.new-count")
    private String code;

    @Schema(description = "指标名称", example = "新增商机数")
    private String name;

    @Schema(description = "来源表")
    private List<String> sourceTables;

    @Schema(description = "来源字段")
    private List<String> sourceFields;

    @Schema(description = "业务时间字段", example = "crm_business.create_time")
    private String businessTime;

    @Schema(description = "聚合公式")
    private String formula;

    @Schema(description = "过滤规则")
    private List<String> filters;

    @Schema(description = "数据权限规则")
    private String permission;
}
