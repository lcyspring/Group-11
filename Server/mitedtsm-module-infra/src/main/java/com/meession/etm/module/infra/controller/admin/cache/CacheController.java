package com.meession.etm.module.infra.controller.admin.cache;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.infra.controller.admin.cache.vo.CacheInfoRespVO;
import com.meession.etm.module.infra.controller.admin.cache.vo.CacheSetReqVO;
import com.meession.etm.module.infra.controller.admin.cache.vo.CacheStatsRespVO;
import com.meession.etm.module.infra.service.cache.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 缓存管理")
@RestController
@RequestMapping("/infra/cache")
@Validated
public class CacheController {

    @Resource
    private CacheService cacheService;

    @GetMapping("/info")
    @Operation(summary = "获取缓存信息列表")
    @PreAuthorize("@ss.hasPermission('infra:cache:query')")
    public CommonResult<List<CacheInfoRespVO>> getCacheInfoList() {
        List<Map<String, Object>> list = cacheService.getCacheInfoList();
        List<CacheInfoRespVO> result = list.stream().map(item -> CacheInfoRespVO.builder()
                .cacheName((String) item.get("cacheName"))
                .keyCount((Integer) item.get("keyCount"))
                .keys((List<String>) item.get("keys"))
                .build()).collect(Collectors.toList());
        return success(result);
    }

    @GetMapping("/get")
    @Operation(summary = "获取缓存值")
    @Parameter(name = "key", description = "缓存 key", required = true, example = "user:1001")
    @PreAuthorize("@ss.hasPermission('infra:cache:query')")
    public CommonResult<String> getCacheValue(@RequestParam("key") String key) {
        return success(cacheService.getCacheValue(key));
    }

    @PostMapping("/set")
    @Operation(summary = "设置缓存值")
    @PreAuthorize("@ss.hasPermission('infra:cache:update')")
    public CommonResult<Boolean> setCacheValue(@Valid @RequestBody CacheSetReqVO reqVO) {
        cacheService.setCacheValue(reqVO.getKey(), reqVO.getValue(), reqVO.getExpireSeconds());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除缓存")
    @Parameter(name = "key", description = "缓存 key", required = true, example = "user:1001")
    @PreAuthorize("@ss.hasPermission('infra:cache:delete')")
    public CommonResult<Boolean> deleteCacheKey(@RequestParam("key") String key) {
        cacheService.deleteCacheKey(key);
        return success(true);
    }

    @DeleteMapping("/delete-pattern")
    @Operation(summary = "批量删除匹配模式的缓存")
    @Parameter(name = "pattern", description = "匹配模式", required = true, example = "user:*")
    @PreAuthorize("@ss.hasPermission('infra:cache:delete')")
    public CommonResult<Long> deleteCacheKeysByPattern(@RequestParam("pattern") String pattern) {
        return success(cacheService.deleteCacheKeysByPattern(pattern));
    }

    @GetMapping("/server-info")
    @Operation(summary = "获取 Redis 服务器信息")
    @PreAuthorize("@ss.hasPermission('infra:cache:query')")
    public CommonResult<Properties> getRedisServerInfo() {
        return success(cacheService.getRedisServerInfo());
    }

    @GetMapping("/stats")
    @Operation(summary = "获取 Redis 统计信息")
    @PreAuthorize("@ss.hasPermission('infra:cache:query')")
    public CommonResult<CacheStatsRespVO> getRedisStats() {
        Map<String, Object> stats = cacheService.getRedisStats();
        CacheStatsRespVO respVO = CacheStatsRespVO.builder()
                .usedMemory((String) stats.get("usedMemory"))
                .connectedClients((Integer) stats.get("connectedClients"))
                .totalCommandsProcessed((Long) stats.get("totalCommandsProcessed"))
                .keyspaceHits((Long) stats.get("keyspaceHits"))
                .keyspaceMisses((Long) stats.get("keyspaceMisses"))
                .uptimeInSeconds((Long) stats.get("uptimeInSeconds"))
                .version((String) stats.get("version"))
                .build();
        return success(respVO);
    }

    @GetMapping("/ttl")
    @Operation(summary = "获取 key 过期时间")
    @Parameter(name = "key", description = "缓存 key", required = true, example = "user:1001")
    @PreAuthorize("@ss.hasPermission('infra:cache:query')")
    public CommonResult<Long> getKeyTtl(@RequestParam("key") String key) {
        return success(cacheService.getKeyTtl(key));
    }

    @GetMapping("/exists")
    @Operation(summary = "判断 key 是否存在")
    @Parameter(name = "key", description = "缓存 key", required = true, example = "user:1001")
    @PreAuthorize("@ss.hasPermission('infra:cache:query')")
    public CommonResult<Boolean> hasKey(@RequestParam("key") String key) {
        return success(cacheService.hasKey(key));
    }

    @GetMapping("/keys")
    @Operation(summary = "获取匹配的 keys")
    @Parameter(name = "pattern", description = "匹配模式", required = true, example = "user:*")
    @Parameter(name = "limit", description = "返回数量限制", example = "100")
    @PreAuthorize("@ss.hasPermission('infra:cache:query')")
    public CommonResult<List<String>> getKeysByPattern(
            @RequestParam("pattern") String pattern,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return success(cacheService.getKeysByPattern(pattern, limit));
    }

}
