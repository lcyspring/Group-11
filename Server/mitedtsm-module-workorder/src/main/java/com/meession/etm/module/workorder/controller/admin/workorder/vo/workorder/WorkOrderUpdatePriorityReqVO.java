package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.workorder.enums.WorkOrderPriorityEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 工单更新优先级 Request VO")
@Data
public class WorkOrderUpdatePriorityReqVO {

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工单编号不能为空")
    private Long id;

    @Schema(description = "优先级: 0-低, 1-中, 2-高, 3-紧急",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "优先级不能为空")
    @InEnum(value = WorkOrderPriorityEnum.class)
    private Integer priority;

}
