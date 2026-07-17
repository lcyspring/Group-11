package com.meession.etm.module.infra.controller.admin.cache.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 缓存信息 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheInfoRespVO {

    @Schema(description = "缓存名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "user")
    private String cacheName;

    @Schema(description = "key 数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer keyCount;

    @Schema(description = "key 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> keys;

}
