package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 工单完结 Request VO")
@Data
public class WorkOrderCompleteReqVO {

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工单编号不能为空")
    private Long id;

    @Schema(description = "处理结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "问题已修复，系统恢复正常")
    @NotBlank(message = "处理结果不能为空")
    private String result;

}
