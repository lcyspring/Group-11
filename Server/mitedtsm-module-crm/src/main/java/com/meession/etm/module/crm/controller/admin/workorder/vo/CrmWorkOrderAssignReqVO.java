package com.meession.etm.module.crm.controller.admin.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客服工单分派 Request VO")
@Data
public class CrmWorkOrderAssignReqVO {

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工单编号不能为空")
    private Long id;

    @Schema(description = "新处理人编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "处理人不能为空")
    private Long handlerUserId;

    @Schema(description = "目标处理组编号；不传时保留原处理组")
    private Long groupId;

    @Schema(description = "分派说明")
    @Size(max = 1000, message = "分派说明不能超过 1000 个字符")
    private String remark;
}
