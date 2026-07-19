package com.meession.etm.module.trade.service.optimize;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 订单分布式锁服务
 */
@Slf4j
@Service
public class TradeOrderLockService {

    private static final String LOCK_PREFIX = "trade:order:lock:";
    private static final long LOCK_WAIT_TIME = 0;
    private static final long LOCK_LEASE_TIME = 30;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取订单锁
     */
    public boolean tryLock(Long orderId) {
        String lockKey = LOCK_PREFIX + orderId;
        String lockValue = String.valueOf(System.currentTimeMillis());

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_LEASE_TIME, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(success)) {
            log.debug("获取订单锁成功: orderId={}", orderId);
            return true;
        }

        log.warn("获取订单锁失败: orderId={}", orderId);
        return false;
    }

    /**
     * 释放订单锁
     */
    public void unlock(Long orderId) {
        String lockKey = LOCK_PREFIX + orderId;
        stringRedisTemplate.delete(lockKey);
        log.debug("释放订单锁成功: orderId={}", orderId);
    }

    /**
     * 带锁执行任务
     */
    public <T> T executeWithLock(Long orderId, java.util.function.Supplier<T> supplier) {
        if (!tryLock(orderId)) {
            throw new RuntimeException("获取订单锁失败: " + orderId);
        }

        try {
            return supplier.get();
        } finally {
            unlock(orderId);
        }
    }

}