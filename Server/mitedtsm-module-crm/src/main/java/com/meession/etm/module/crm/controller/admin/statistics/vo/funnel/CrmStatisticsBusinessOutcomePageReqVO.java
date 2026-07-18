package com.meession.etm.module.crm.controller.admin.statistics.vo.funnel;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.business.CrmBusinessEndStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CRM 商机终态结果明细 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmStatisticsBusinessOutcomePageReqVO extends CrmStatisticsBusinessStageReqVO {

    @Schema(description = "结束状态：1 赢单、2 输单、3 无效", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束状态不能为空")
    @InEnum(CrmBusinessEndStatusEnum.class)
    private Integer endStatus;
}
