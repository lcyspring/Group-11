package com.meession.etm.module.crm.job.marketing;

import com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties;
import com.meession.etm.module.crm.service.marketing.CrmCustomerCareService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmCustomerCareSchedulerTest {
    @Mock CrmCustomerCareJob job;
    @Mock RedissonClient redissonClient;
    @Mock RLock lock;

    @Test
    void acquiredDistributedLockRunsTenantJobAndUnlocks() throws Exception {
        CrmMarketingProperties properties = properties();
        when(redissonClient.getLock("crm:marketing:care")).thenReturn(lock);
        when(lock.tryLock(0, 300, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        new CrmCustomerCareScheduler(job, properties, redissonClient).execute();

        verify(job).execute(null);
        verify(lock).unlock();
    }

    @Test
    void unavailableLockSkipsJob() throws Exception {
        when(redissonClient.getLock("crm:marketing:care")).thenReturn(lock);
        when(lock.tryLock(0, 300, TimeUnit.SECONDS)).thenReturn(false);

        new CrmCustomerCareScheduler(job, properties(), redissonClient).execute();

        verifyNoInteractions(job);
        verify(lock, never()).unlock();
    }

    @Test
    void interruptedLockRestoresInterruptStatus() throws Exception {
        when(redissonClient.getLock("crm:marketing:care")).thenReturn(lock);
        when(lock.tryLock(0, 300, TimeUnit.SECONDS)).thenThrow(new InterruptedException("stop"));
        try {
            new CrmCustomerCareScheduler(job, properties(), redissonClient).execute();
            assertTrue(Thread.currentThread().isInterrupted());
            verifyNoInteractions(job);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void tenantJobReportsGeneratedRecordCount() {
        CrmCustomerCareService service = mock(CrmCustomerCareService.class);
        when(service.generateAndSendToday()).thenReturn(3);
        assertEquals("生成并处理 CRM 客户关怀记录 3 条", new CrmCustomerCareJob(service).execute(null));
    }

    private static CrmMarketingProperties properties() {
        CrmMarketingProperties properties = new CrmMarketingProperties();
        properties.setLockKey("crm:marketing:care");
        properties.setLockLeaseSeconds(300);
        return properties;
    }
}
