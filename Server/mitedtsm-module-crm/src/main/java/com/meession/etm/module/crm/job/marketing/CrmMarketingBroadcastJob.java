package com.meession.etm.module.crm.job.marketing;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.job.TenantJob;
import com.meession.etm.module.crm.service.marketing.CrmMarketingOutreachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrmMarketingBroadcastJob implements JobHandler {
    private final CrmMarketingOutreachService outreachService;

    @Override
    @TenantJob
    public String execute(String param) {
        int succeeded = 0;
        int failed = 0;
        for (Long id : outreachService.getDueScheduledBroadcastIds()) {
            try {
                outreachService.send(id);
                succeeded++;
            } catch (RuntimeException ex) {
                failed++;
                log.error("Failed to send scheduled CRM marketing broadcast {}", id, ex);
            }
        }
        int deliveryResults = outreachService.syncPendingDeliveryResults();
        return "CRM scheduled broadcasts sent=" + succeeded + ", failed=" + failed
                + ", delivery-results=" + deliveryResults;
    }
}
