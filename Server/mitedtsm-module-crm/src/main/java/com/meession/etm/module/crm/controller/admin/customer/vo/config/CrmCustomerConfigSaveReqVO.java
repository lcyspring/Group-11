package com.meession.etm.module.crm.controller.admin.customer.vo.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户配置新增/修改 Request VO")
@Data
public class CrmCustomerConfigSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "配置类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "level")
    @NotBlank(message = "配置类型不能为空")
    private String configType;

    @Schema(description = "配置值", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "配置值不能为空")
    private Integer configValue;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "A（重点客户）")
    @NotBlank(message = "配置名称不能为空")
    private String configName;

    @Schema(description = "颜色", example = "#FF0000")
    private String color;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

    @Schema(description = "状态", example = "true")
    private Boolean status;

}