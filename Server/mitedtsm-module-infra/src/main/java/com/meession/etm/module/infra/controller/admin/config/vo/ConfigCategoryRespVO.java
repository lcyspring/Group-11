package com.meession.etm.module.infra.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 参数配置分类 Response VO")
@Data
public class ConfigCategoryRespVO {

    @Schema(description = "参数分类", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer category;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "系统配置")
    private String categoryName;

    @Schema(description = "配置数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer configCount;

}
