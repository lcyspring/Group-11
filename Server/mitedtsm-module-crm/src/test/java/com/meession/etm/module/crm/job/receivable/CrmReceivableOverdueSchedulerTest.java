package com.meession.etm.module.crm.job.receivable;

import com.meession.etm.module.crm.framework.activity.CrmActivityProperties;
import com.meession.etm.module.crm.service.receivable.CrmReceivableOverdueService;
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
class CrmReceivableOverdueSchedulerTest {
    @Mock private CrmReceivableOverdueJob job;
    @Mock private CrmActivityProperties properties;
    @Mock private CrmActivityProperties.ReceivableOverdue policy;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock lock;
    @InjectMocks private CrmReceivableOverdueScheduler scheduler;

    @Test
    void executesTenantJobAndReleasesClusterLock() throws Exception {
        arrange();
        when(lock.tryLock(0, 1800, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        scheduler.execute();
        verify(job).execute(null);
        verify(lock).unlock();
    }

    @Test
    void skipsWhenAnotherNodeOwnsLock() throws Exception {
        arrange();
        when(lock.tryLock(0, 1800, TimeUnit.SECONDS)).thenReturn(false);
        scheduler.execute();
        verifyNoInteractions(job);
    }

    @Test
    void interruptionRestoresFlag() throws Exception {
        arrange();
        when(lock.tryLock(0, 1800, TimeUnit.SECONDS)).thenThrow(new InterruptedException("stop"));
        scheduler.execute();
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
        verifyNoInteractions(job);
    }

    @Test
    void tenantJobReportsSentCount() {
        CrmReceivableOverdueService service = mock(CrmReceivableOverdueService.class);
        when(service.scanAndNotify()).thenReturn(3);
        assertEquals("发送回款逾期提醒 3 条", new CrmReceivableOverdueJob(service).execute(null));
    }

    private void arrange() {
        when(properties.getReceivableOverdue()).thenReturn(policy);
        when(policy.getLockKey()).thenReturn("crm:receivable:overdue");
        when(policy.getLockLeaseSeconds()).thenReturn(1800);
        when(redissonClient.getLock("crm:receivable:overdue")).thenReturn(lock);
    }
}
