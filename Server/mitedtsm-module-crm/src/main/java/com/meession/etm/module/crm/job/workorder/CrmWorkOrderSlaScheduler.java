package com.meession.etm.module.crm.job.workorder;

import com.meession.etm.module.crm.framework.workorder.CrmWorkOrderGovernanceProperties;
import com.meession.etm.module.crm.service.workorder.CrmWorkOrderService;
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
    private final CrmWorkOrderService workOrderService;
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
            int changed = workOrderService.processDueSla();
            if (changed > 0) log.info("[execute][CRM work-order SLA changed {} records]", changed);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("[execute][CRM work-order SLA scheduler interrupted]", ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
