package com.meession.etm.module.infra.controller.admin.cache.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 缓存设置 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheSetReqVO {

    @Schema(description = "缓存 key", requiredMode = Schema.RequiredMode.REQUIRED, example = "user:1001")
    @NotBlank(message = "缓存 key 不能为空")
    private String key;

    @Schema(description = "缓存值", example = "test value")
    private String value;

    @Schema(description = "过期时间（秒）", example = "3600")
    private Long expireSeconds;

}
