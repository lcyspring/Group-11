package com.meession.etm.module.infra.controller.admin.cache.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - Redis 统计信息 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatsRespVO {

    @Schema(description = "已使用内存", requiredMode = Schema.RequiredMode.REQUIRED, example = "100M")
    private String usedMemory;

    @Schema(description = "连接客户端数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer connectedClients;

    @Schema(description = "已处理命令总数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000000")
    private Long totalCommandsProcessed;

    @Schema(description = "keyspace 命中数", requiredMode = Schema.RequiredMode.REQUIRED, example = "500000")
    private Long keyspaceHits;

    @Schema(description = "keyspace 未命中数", requiredMode = Schema.RequiredMode.REQUIRED, example = "100000")
    private Long keyspaceMisses;

    @Schema(description = "运行时间（秒）", requiredMode = Schema.RequiredMode.REQUIRED, example = "86400")
    private Long uptimeInSeconds;

    @Schema(description = "Redis 版本", requiredMode = Schema.RequiredMode.REQUIRED, example = "7.0.0")
    private String version;

}
