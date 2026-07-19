package com.meession.etm.module.crm.job.exporttask;

import com.meession.etm.module.crm.framework.exporttask.CrmExportTaskProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmExportTaskSchedulerTest {

    @Mock private CrmExportTaskJob job;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock lock;

    private CrmExportTaskScheduler scheduler;
    private CrmExportTaskProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CrmExportTaskProperties();
        properties.setLockKey("crm:export:test");
        properties.setLockLeaseSeconds(120);
        scheduler = new CrmExportTaskScheduler(job, properties, redissonClient);
        when(redissonClient.getLock("crm:export:test")).thenReturn(lock);
    }

    @Test
    void onlyLockOwnerRunsTenantJobAndReleasesLock() throws Exception {
        when(lock.tryLock(0, 120, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        scheduler.execute();

        verify(job).execute(null);
        verify(lock).unlock();
    }

    @Test
    void competingNodeSkipsBatch() throws Exception {
        when(lock.tryLock(0, 120, TimeUnit.SECONDS)).thenReturn(false);

        scheduler.execute();

        verify(job, never()).execute(null);
        verify(lock, never()).unlock();
    }
}
