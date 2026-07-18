package com.meession.etm.module.crm.job.exporttask;

import com.meession.etm.module.crm.framework.exporttask.CrmExportTaskProperties;
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
@ConditionalOnProperty(prefix = "mitedtsm.crm.export-task", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class CrmExportTaskScheduler {
    private final CrmExportTaskJob job;
    private final CrmExportTaskProperties properties;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "${mitedtsm.crm.export-task.cron}", zone = "${mitedtsm.crm.export-task.zone}")
    public void execute() {
        RLock lock = redissonClient.getLock(properties.getLockKey());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, properties.getLockLeaseSeconds(), TimeUnit.SECONDS);
            if (acquired) {
                job.execute(null);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("CRM export scheduler interrupted", ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
