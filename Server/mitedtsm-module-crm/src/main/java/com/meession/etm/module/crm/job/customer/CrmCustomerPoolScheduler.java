package com.meession.etm.module.crm.job.customer;

import com.meession.etm.module.crm.framework.pool.CrmPoolPolicyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** YAML-governed cluster-safe trigger for the tenant-aware pool job handler. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "mitedtsm.crm.pool-policy.scheduler", name = "enabled", havingValue = "true")
public class CrmCustomerPoolScheduler {

    private final CrmCustomerAutoPutPoolJob job;
    private final CrmPoolPolicyProperties properties;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "${mitedtsm.crm.pool-policy.scheduler.cron}",
            zone = "${mitedtsm.crm.pool-policy.scheduler.zone}")
    public void execute() {
        CrmPoolPolicyProperties.Scheduler scheduler = properties.getScheduler();
        RLock lock = redissonClient.getLock(scheduler.getLockKey());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, scheduler.getLockLeaseSeconds(), TimeUnit.SECONDS);
            if (!acquired) {
                log.info("[execute][CRM pool scheduler skipped because another node owns the lock]");
                return;
            }
            job.execute(null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("[execute][CRM pool scheduler interrupted before lock acquisition]", ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
