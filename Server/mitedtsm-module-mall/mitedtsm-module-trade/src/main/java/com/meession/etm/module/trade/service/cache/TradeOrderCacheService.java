package com.meession.etm.module.trade.service.cache;

import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 订单缓存服务
 */
@Slf4j
@Service
public class TradeOrderCacheService {

    private static final String CACHE_NAME = "trade_order";

    private final Map<Long, TradeOrderDO> localCache = new ConcurrentHashMap<>();

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Cacheable(value = CACHE_NAME, key = "#orderId")
    public TradeOrderDO getOrderById(Long orderId) {
        log.debug("从数据库查询订单: orderId={}", orderId);
        return tradeOrderMapper.selectById(orderId);
    }

    public TradeOrderDO getOrderByIdWithLocalCache(Long orderId) {
        TradeOrderDO order = localCache.get(orderId);
        if (order != null) {
            return order;
        }

        order = tradeOrderMapper.selectById(orderId);
        if (order != null) {
            localCache.put(orderId, order);
        }
        return order;
    }

    @CacheEvict(value = CACHE_NAME, key = "#orderId")
    public void evictOrderCache(Long orderId) {
        log.debug("清除订单缓存: orderId={}", orderId);
        localCache.remove(orderId);
    }

    public void evictAllCache() {
        log.debug("清除所有订单缓存");
        localCache.clear();
    }

    public void preloadOrders(List<Long> orderIds) {
        log.info("预加载订单缓存: orderIds.size={}", orderIds.size());
        for (Long orderId : orderIds) {
            TradeOrderDO order = tradeOrderMapper.selectById(orderId);
            if (order != null) {
                localCache.put(orderId, order);
            }
        }
    }

}