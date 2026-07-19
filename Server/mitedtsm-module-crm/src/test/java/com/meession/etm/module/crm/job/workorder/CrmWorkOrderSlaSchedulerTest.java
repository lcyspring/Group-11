package com.meession.etm.module.crm.job.workorder;

import com.meession.etm.module.crm.framework.workorder.CrmWorkOrderGovernanceProperties;
import com.meession.etm.module.crm.service.workorder.CrmWorkOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmWorkOrderSlaSchedulerTest {
    @Mock CrmWorkOrderSlaJob job;
    @Mock CrmWorkOrderGovernanceProperties properties;
    @Mock CrmWorkOrderGovernanceProperties.Sla sla;
    @Mock RedissonClient redissonClient;
    @Mock RLock lock;

    @Test
    void delegatesThroughTenantAwareJob() throws Exception {
        when(properties.getSla()).thenReturn(sla);
        when(sla.isEnabled()).thenReturn(true);
        when(sla.getLockKey()).thenReturn("crm:work-order:sla");
        when(sla.getLockLeaseSeconds()).thenReturn(300);
        when(redissonClient.getLock("crm:work-order:sla")).thenReturn(lock);
        when(lock.tryLock(0, 300, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        new CrmWorkOrderSlaScheduler(job, properties, redissonClient).execute();

        verify(job).execute(null);
        verify(lock).unlock();
    }

    @Test
    void jobReportsChangedCount() {
        CrmWorkOrderService service = mock(CrmWorkOrderService.class);
        when(service.processDueSla()).thenReturn(2);
        assertEquals("CRM 工单 SLA 更新 2 条", new CrmWorkOrderSlaJob(service).execute(null));
    }
}
