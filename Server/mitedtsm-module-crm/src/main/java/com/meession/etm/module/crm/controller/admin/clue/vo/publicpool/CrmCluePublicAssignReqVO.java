package com.meession.etm.module.crm.controller.admin.clue.vo.publicpool;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 公共线索主管分配 Request VO")
@Data
public class CrmCluePublicAssignReqVO {

    @Schema(description = "线索编号集合", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "线索编号不能为空")
    private List<Long> clueIds;

    @Schema(description = "目标负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标负责人不能为空")
    private Long ownerUserId;
}
