package com.meession.etm.module.crm.controller.admin.statistics.vo.funnel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CRM 商机阶段漏斗明细 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmStatisticsBusinessStagePageReqVO extends CrmStatisticsBusinessStageReqVO {

    @Schema(description = "商机阶段编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "商机阶段不能为空")
    private Long statusId;

}
