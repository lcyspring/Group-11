package com.meession.etm.module.crm.controller.admin.statistics.vo.performance;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetScopeTypeEnum;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CRM 业绩目标完成度 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmStatisticsTargetCompletionReqVO extends CrmStatisticsPerformanceReqVO {

    @Schema(description = "目标范围类型：1 公司、2 部门、3 个人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标范围类型不能为空")
    @InEnum(value = CrmPerformanceTargetScopeTypeEnum.class, message = "目标范围类型必须是 {value}")
    private Integer scopeType;

    @Schema(description = "目标类型：1 成交金额、2 回款金额、3 跟进次数、4 新增客户、5 新增商机",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标类型不能为空")
    @InEnum(value = CrmPerformanceTargetTypeEnum.class, message = "目标类型必须是 {value}")
    private Integer targetType;

}
