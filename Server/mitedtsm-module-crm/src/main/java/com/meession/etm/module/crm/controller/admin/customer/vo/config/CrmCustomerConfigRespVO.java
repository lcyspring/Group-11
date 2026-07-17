package com.meession.etm.module.crm.controller.admin.customer.vo.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户配置 Response VO")
@Data
public class CrmCustomerConfigRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "配置类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "level")
    private String configType;

    @Schema(description = "配置值", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer configValue;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "A（重点客户）")
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