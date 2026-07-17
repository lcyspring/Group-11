package com.meession.etm.module.infra.service.cache;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存管理服务实现类
 */
@Service
@Slf4j
@Validated
public class CacheServiceImpl implements CacheService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<Map<String, Object>> getCacheInfoList() {
        Set<String> keys = stringRedisTemplate.keys("*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        // 按前缀分组统计
        Map<String, List<String>> prefixMap = new HashMap<>();
        for (String key : keys) {
            String prefix = extractPrefix(key);
            prefixMap.computeIfAbsent(prefix, k -> new ArrayList<>()).add(key);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        prefixMap.forEach((prefix, keyList) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("cacheName", prefix);
            item.put("keyCount", keyList.size());
            item.put("keys", keyList);
            result.add(item);
        });

        return result;
    }

    @Override
    public String getCacheValue(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void setCacheValue(String key, String value, Long expireSeconds) {
        if (expireSeconds != null && expireSeconds > 0) {
            stringRedisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
        } else {
            stringRedisTemplate.opsForValue().set(key, value);
        }
    }

    @Override
    public void deleteCacheKey(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public Long deleteCacheKeysByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return stringRedisTemplate.delete(keys);
    }

    @Override
    public Properties getRedisServerInfo() {
        return stringRedisTemplate.execute((RedisCallback<Properties>) RedisServerCommands::info);
    }

    @Override
    public Map<String, Object> getRedisStats() {
        Properties info = getRedisServerInfo();
        Map<String, Object> stats = new HashMap<>();

        stats.put("usedMemory", info.getProperty("used_memory_human", "0K"));
        stats.put("connectedClients", Integer.valueOf(info.getProperty("connected_clients", "0")));
        stats.put("totalCommandsProcessed", Long.valueOf(info.getProperty("total_commands_processed", "0")));
        stats.put("keyspaceHits", Long.valueOf(info.getProperty("keyspace_hits", "0")));
        stats.put("keyspaceMisses", Long.valueOf(info.getProperty("keyspace_misses", "0")));
        stats.put("uptimeInSeconds", Long.valueOf(info.getProperty("uptime_in_seconds", "0")));
        stats.put("version", info.getProperty("redis_version", "unknown"));

        return stats;
    }

    @Override
    public Long getKeyTtl(String key) {
        return stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public List<String> getKeysByPattern(String pattern, int limit) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        return keys.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 提取 key 的前缀（以 : 分隔的第一部分）
     */
    private String extractPrefix(String key) {
        if (StrUtil.isEmpty(key)) {
            return "default";
        }
        int index = key.indexOf(":");
        if (index > 0) {
            return key.substring(0, index);
        }
        return "default";
    }
}
