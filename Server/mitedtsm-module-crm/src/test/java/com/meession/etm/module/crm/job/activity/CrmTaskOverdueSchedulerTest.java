package com.meession.etm.module.crm.job.activity;

import com.meession.etm.module.crm.framework.activity.CrmActivityProperties;
import com.meession.etm.module.crm.service.activity.CrmActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmTaskOverdueSchedulerTest {

    @Mock private CrmTaskOverdueJob job;
    @Mock private CrmActivityProperties properties;
    @Mock private CrmActivityProperties.TaskOverdue policy;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock lock;
    @InjectMocks private CrmTaskOverdueScheduler scheduler;

    @Test
    void executesOnceAndReleasesClusterLock() throws Exception {
        arrangePolicy();
        when(lock.tryLock(0, 1800, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        scheduler.execute();

        verify(job).execute(null);
        verify(lock).unlock();
    }

    @Test
    void skipsWhenAnotherNodeOwnsLock() throws Exception {
        arrangePolicy();
        when(lock.tryLock(0, 1800, TimeUnit.SECONDS)).thenReturn(false);

        scheduler.execute();

        verifyNoInteractions(job);
        verify(lock, never()).unlock();
    }

    @Test
    void interruptionRestoresThreadFlag() throws Exception {
        arrangePolicy();
        when(lock.tryLock(0, 1800, TimeUnit.SECONDS)).thenThrow(new InterruptedException("stop"));

        scheduler.execute();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
        verifyNoInteractions(job);
    }

    @Test
    void tenantJobReportsUpdatedCount() {
        CrmActivityService service = mock(CrmActivityService.class);
        when(service.markOverdueTasks()).thenReturn(3);
        assertEquals("标记超时 CRM 任务 3 个", new CrmTaskOverdueJob(service).execute(null));
    }

    private void arrangePolicy() {
        when(properties.getTaskOverdue()).thenReturn(policy);
        when(policy.getLockKey()).thenReturn("crm:activity:task-overdue");
        when(policy.getLockLeaseSeconds()).thenReturn(1800);
        when(redissonClient.getLock("crm:activity:task-overdue")).thenReturn(lock);
    }
}
