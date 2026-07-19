package com.meession.etm.module.crm.controller.admin.clue.vo.publicpool;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 公共线索领取 Request VO")
@Data
public class CrmCluePublicClaimReqVO {

    @Schema(description = "线索编号集合", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "线索编号不能为空")
    private List<Long> clueIds;
}
