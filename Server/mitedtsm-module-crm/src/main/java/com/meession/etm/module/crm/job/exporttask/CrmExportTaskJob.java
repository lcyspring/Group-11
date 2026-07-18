package com.meession.etm.module.crm.job.exporttask;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.job.TenantJob;
import com.meession.etm.module.crm.service.exporttask.CrmExportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrmExportTaskJob implements JobHandler {
    private final CrmExportTaskService service;

    @Override
    @TenantJob
    public String execute(String param) {
        return "CRM export tasks processed=" + service.processTenantBatch();
    }
}
