package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 工单退回 Request VO")
@Data
public class WorkOrderReturnReqVO {

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工单编号不能为空")
    private Long id;

    @Schema(description = "退回原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "处理方案不符合要求，需重新处理")
    @NotBlank(message = "退回原因不能为空")
    private String result;

}
