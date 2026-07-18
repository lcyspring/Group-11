package com.meession.etm.module.trade.service.batch;

import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 订单批量查询服务
 */
@Slf4j
@Service
public class TradeOrderBatchService {

    private static final int BATCH_SIZE = 1000;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    public List<TradeOrderDO> batchSelectByIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<TradeOrderDO> result = new ArrayList<>();
        int totalSize = orderIds.size();
        int batchCount = (totalSize + BATCH_SIZE - 1) / BATCH_SIZE;

        for (int i = 0; i < batchCount; i++) {
            int fromIndex = i * BATCH_SIZE;
            int toIndex = Math.min(fromIndex + BATCH_SIZE, totalSize);
            List<Long> batchIds = orderIds.subList(fromIndex, toIndex);

            List<TradeOrderDO> batchOrders = tradeOrderMapper.selectBatchIds(batchIds);
            result.addAll(batchOrders);
        }

        log.info("批量查询订单完成: totalIds={}, batchSize={}, resultSize={}", totalSize, batchCount, result.size());
        return result;
    }

    @Async
    public Future<List<TradeOrderDO>> asyncBatchSelectByIds(List<Long> orderIds) {
        return CompletableFuture.completedFuture(batchSelectByIds(orderIds));
    }

    public int batchUpdateStatus(List<Long> orderIds, Integer status) {
        if (orderIds == null || orderIds.isEmpty()) {
            return 0;
        }

        int updateCount = 0;
        for (Long orderId : orderIds) {
            TradeOrderDO updateObj = new TradeOrderDO();
            updateObj.setId(orderId);
            updateObj.setStatus(status);
            tradeOrderMapper.updateById(updateObj);
            updateCount++;
        }

        log.info("批量更新订单状态完成: orderIds.size={}, status={}, updateCount={}", orderIds.size(), status, updateCount);
        return updateCount;
    }

    public List<TradeOrderDO> parallelQueryByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<CompletableFuture<List<TradeOrderDO>>> futures = new ArrayList<>();
        for (Long userId : userIds) {
            CompletableFuture<List<TradeOrderDO>> future = CompletableFuture.supplyAsync(() -> {
                LambdaQueryWrapperX<TradeOrderDO> wrapper = new LambdaQueryWrapperX<TradeOrderDO>()
                        .eq(TradeOrderDO::getUserId, userId);
                return tradeOrderMapper.selectList(wrapper);
            }, executorService);
            futures.add(future);
        }

        List<TradeOrderDO> result = new ArrayList<>();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        for (CompletableFuture<List<TradeOrderDO>> future : futures) {
            try {
                result.addAll(future.get());
            } catch (Exception e) {
                log.error("并行查询订单失败", e);
            }
        }

        log.info("并行查询订单完成: userIds.size={}, resultSize={}", userIds.size(), result.size());
        return result;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

}