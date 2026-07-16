package com.meession.etm.module.crm.job.marketing;

import com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties;
import com.meession.etm.module.crm.service.marketing.CrmMarketingOutreachService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmMarketingBroadcastSchedulerTest {
    @Mock CrmMarketingBroadcastJob job;
    @Mock RedissonClient redissonClient;
    @Mock RLock lock;

    @Test
    void schedulerUsesDedicatedDistributedLock() throws Exception {
        CrmMarketingProperties properties = new CrmMarketingProperties();
        properties.setBroadcastLockKey("crm:marketing:broadcast");
        properties.setLockLeaseSeconds(300);
        when(redissonClient.getLock("crm:marketing:broadcast")).thenReturn(lock);
        when(lock.tryLock(0, 300, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        new CrmMarketingBroadcastScheduler(job, properties, redissonClient).execute();

        verify(job).execute(null);
        verify(lock).unlock();
    }

    @Test
    void tenantJobContinuesAfterOneScheduledBroadcastFails() {
        CrmMarketingOutreachService service = mock(CrmMarketingOutreachService.class);
        when(service.getDueScheduledBroadcastIds()).thenReturn(List.of(1L, 2L, 3L));
        doNothing().when(service).send(1L);
        doThrow(new IllegalStateException("provider unavailable")).when(service).send(2L);
        doNothing().when(service).send(3L);

        String result = new CrmMarketingBroadcastJob(service).execute(null);

        assertEquals("CRM scheduled broadcasts sent=2, failed=1", result);
        verify(service).send(1L);
        verify(service).send(2L);
        verify(service).send(3L);
    }
}
