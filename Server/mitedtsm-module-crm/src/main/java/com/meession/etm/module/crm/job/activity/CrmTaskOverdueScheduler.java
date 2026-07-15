package com.meession.etm.module.crm.job.activity;

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
@ConditionalOnProperty(prefix = "mitedtsm.crm.activity.task-overdue", name = "enabled", havingValue = "true")
public class CrmTaskOverdueScheduler {

    private final CrmTaskOverdueJob job;
    private final CrmActivityProperties properties;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "${mitedtsm.crm.activity.task-overdue.cron}",
            zone = "${mitedtsm.crm.activity.task-overdue.zone}")
    public void execute() {
        CrmActivityProperties.TaskOverdue policy = properties.getTaskOverdue();
        RLock lock = redissonClient.getLock(policy.getLockKey());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, policy.getLockLeaseSeconds(), TimeUnit.SECONDS);
            if (!acquired) {
                log.info("[execute][CRM task overdue scheduler skipped because another node owns the lock]");
                return;
            }
            job.execute(null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("[execute][CRM task overdue scheduler interrupted before lock acquisition]", ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
