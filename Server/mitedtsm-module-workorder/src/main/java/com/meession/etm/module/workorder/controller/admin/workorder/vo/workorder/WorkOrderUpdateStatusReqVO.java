package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.workorder.enums.WorkOrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 工单更新状态 Request VO")
@Data
public class WorkOrderUpdateStatusReqVO {

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工单编号不能为空")
    private Long id;

    @Schema(description = "目标状态: 0-待处理, 1-处理中, 2-已完成, 3-已关闭, 4-已退回",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "目标状态不能为空")
    @InEnum(value = WorkOrderStatusEnum.class)
    private Integer status;

    @Schema(description = "处理结果/备注", example = "问题已修复")
    private String result;

}
