package com.meession.etm.module.crm.controller.admin.statistics.vo.performance;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetScopeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 年度业绩目标列表 Request VO")
@Data
public class CrmPerformanceTargetListReqVO {

    @Schema(description = "范围类型：1 公司、2 部门、3 个人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标范围类型不能为空")
    @InEnum(value = CrmPerformanceTargetScopeTypeEnum.class, message = "目标范围类型必须是 {value}")
    private Integer scopeType;

    @Schema(description = "范围编号；公司范围固定为 0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标范围编号不能为空")
    @Min(value = 0, message = "目标范围编号不能小于 0")
    private Long scopeId;

    @Schema(description = "目标年度", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标年度不能为空")
    @Min(value = 2000, message = "目标年度不能早于 2000 年")
    @Max(value = 2100, message = "目标年度不能晚于 2100 年")
    private Integer targetYear;

}
