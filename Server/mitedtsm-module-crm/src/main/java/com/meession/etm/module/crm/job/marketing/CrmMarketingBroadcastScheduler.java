package com.meession.etm.module.crm.job.marketing;

import com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "mitedtsm.crm.marketing", name = "broadcast-enabled",
        havingValue = "true", matchIfMissing = true)
public class CrmMarketingBroadcastScheduler {
    private final CrmMarketingBroadcastJob job;
    private final CrmMarketingProperties properties;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "${mitedtsm.crm.marketing.broadcast-cron}",
            zone = "${mitedtsm.crm.marketing.broadcast-zone}")
    public void execute() {
        RLock lock = redissonClient.getLock(properties.getBroadcastLockKey());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, properties.getLockLeaseSeconds(), TimeUnit.SECONDS);
            if (acquired) {
                job.execute(null);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("CRM broadcast scheduler interrupted", ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
