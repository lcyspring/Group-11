package com.meession.etm.module.crm.job.workorder;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.job.TenantJob;
import com.meession.etm.module.crm.service.workorder.CrmWorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Tenant-aware execution boundary for the scheduled CRM work-order SLA sweep. */
@Component
@RequiredArgsConstructor
public class CrmWorkOrderSlaJob implements JobHandler {

    private final CrmWorkOrderService workOrderService;

    @Override
    @TenantJob
    public String execute(String param) {
        int changed = workOrderService.processDueSla();
        return String.format("CRM 工单 SLA 更新 %s 条", changed);
    }
}
