package com.meession.etm.module.infra.service.cache;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 缓存管理服务接口
 * 提供 Redis 缓存的管理、监控能力
 */
public interface CacheService {

    /** 获取缓存信息列表（所有缓存名称和大小） */
    List<Map<String, Object>> getCacheInfoList();

    /** 获取指定 key 的值 */
    String getCacheValue(String key);

    /** 设置指定 key 的值 */
    void setCacheValue(String key, String value, Long expireSeconds);

    /** 删除指定 key */
    void deleteCacheKey(String key);

    /** 批量删除匹配模式的 key */
    Long deleteCacheKeysByPattern(String pattern);

    /** 获取 Redis 服务器信息 */
    Properties getRedisServerInfo();

    /** 获取 Redis 服务器统计信息 */
    Map<String, Object> getRedisStats();

    /** 获取 key 的剩余过期时间（秒） */
    Long getKeyTtl(String key);

    /** 判断 key 是否存在 */
    Boolean hasKey(String key);

    /** 获取匹配模式的所有 key */
    List<String> getKeysByPattern(String pattern, int limit);
}
