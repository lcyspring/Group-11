package com.meession.etm.module.workorder.controller.admin.workorder.vo.type;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 工单类型创建/更新 Request VO")
@Data
public class WorkOrderTypeSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "故障报修")
    @NotBlank(message = "类型名称不能为空")
    private String name;

    @Schema(description = "类型编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "REPAIR")
    @NotBlank(message = "类型编码不能为空")
    private String code;

    @Schema(description = "类型描述", example = "系统故障相关的工单")
    private String description;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "状态: 0-启用, 1-禁用", example = "0")
    private Integer status;

}
