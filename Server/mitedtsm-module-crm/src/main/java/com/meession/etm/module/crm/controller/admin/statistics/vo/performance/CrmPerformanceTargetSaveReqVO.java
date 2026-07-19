package com.meession.etm.module.crm.controller.admin.statistics.vo.performance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - CRM 年度业绩目标新增/修改 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmPerformanceTargetSaveReqVO extends CrmPerformanceTargetBaseReqVO {

    @Schema(description = "1 至 12 月目标值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "月度目标不能为空")
    @Size(min = 12, max = 12, message = "月度目标必须正好包含 12 项")
    @Valid
    private List<@NotNull(message = "月度目标项不能为空")
            @DecimalMin(value = "0", message = "月度目标不能小于 0")
            @Digits(integer = 18, fraction = 2, message = "月度目标最多 18 位整数和 2 位小数") BigDecimal> monthlyTargets;

}
