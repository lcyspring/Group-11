package com.meession.etm.module.crm.job.workorder;

import com.meession.etm.module.crm.framework.workorder.CrmWorkOrderGovernanceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrmWorkOrderSlaScheduler {
    private final CrmWorkOrderSlaJob job;
    private final CrmWorkOrderGovernanceProperties properties;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "${mitedtsm.crm.work-order-governance.sla.cron}",
            zone = "${mitedtsm.crm.work-order-governance.sla.zone}")
    public void execute() {
        if (!properties.getSla().isEnabled()) return;
        RLock lock = redissonClient.getLock(properties.getSla().getLockKey());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, properties.getSla().getLockLeaseSeconds(), TimeUnit.SECONDS);
            if (!acquired) return;
            job.execute(null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("[execute][CRM work-order SLA scheduler interrupted]", ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
