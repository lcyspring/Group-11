package com.meession.etm.module.crm.job.receivable;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.job.TenantJob;
import com.meession.etm.module.crm.service.receivable.CrmReceivableOverdueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrmReceivableOverdueJob implements JobHandler {
    private final CrmReceivableOverdueService service;

    @Override
    @TenantJob
    public String execute(String param) {
        return String.format("发送回款逾期提醒 %s 条", service.scanAndNotify());
    }
}
