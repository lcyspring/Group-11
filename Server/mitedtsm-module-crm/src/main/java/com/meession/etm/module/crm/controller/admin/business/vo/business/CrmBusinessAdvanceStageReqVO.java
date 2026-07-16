package com.meession.etm.module.crm.controller.admin.business.vo.business;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.business.CrmBusinessEndStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 商机阶段推进请求 VO")
@Data
public class CrmBusinessAdvanceStageReqVO {

    @Schema(description = "商机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "32129")
    @NotNull(message = "商机编号不能为空")
    private Long id;

    @Schema(description = "流转备注")
    private String remark;

    @Schema(description = "结束状态（推进到结束时使用）", example = "1")
    @InEnum(value = CrmBusinessEndStatusEnum.class)
    private Integer endStatus;

    @Schema(description = "结束备注")
    private String endRemark;

}