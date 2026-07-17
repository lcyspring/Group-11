package com.meession.etm.module.crm.job.receivable;

import com.meession.etm.module.crm.framework.activity.CrmActivityProperties;
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
@ConditionalOnProperty(prefix = "mitedtsm.crm.activity.receivable-overdue", name = "enabled", havingValue = "true")
public class CrmReceivableOverdueScheduler {
    private final CrmReceivableOverdueJob job;
    private final CrmActivityProperties properties;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "${mitedtsm.crm.activity.receivable-overdue.cron}",
            zone = "${mitedtsm.crm.activity.receivable-overdue.zone}")
    public void execute() {
        var policy = properties.getReceivableOverdue();
        RLock lock = redissonClient.getLock(policy.getLockKey());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, policy.getLockLeaseSeconds(), TimeUnit.SECONDS);
            if (acquired) job.execute(null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("[execute][receivable overdue scheduler interrupted]", ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
