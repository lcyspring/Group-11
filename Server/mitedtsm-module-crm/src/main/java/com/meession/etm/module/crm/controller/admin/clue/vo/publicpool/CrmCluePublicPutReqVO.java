package com.meession.etm.module.crm.controller.admin.clue.vo.publicpool;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 线索放入公共池 Request VO")
@Data
public class CrmCluePublicPutReqVO {

    @Schema(description = "线索编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "线索编号不能为空")
    private Long clueId;

    @Schema(description = "放入原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "放入公共线索的原因不能为空")
    @Size(max = 500, message = "放入公共线索的原因不能超过 500 个字符")
    private String reason;
}
