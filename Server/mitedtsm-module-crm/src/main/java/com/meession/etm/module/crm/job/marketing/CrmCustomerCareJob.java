package com.meession.etm.module.crm.job.marketing;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.job.TenantJob;
import com.meession.etm.module.crm.service.marketing.CrmCustomerCareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrmCustomerCareJob implements JobHandler {
    private final CrmCustomerCareService careService;

    @Override
    @TenantJob
    public String execute(String param) {
        return "生成并处理 CRM 客户关怀记录 " + careService.generateAndSendToday() + " 条";
    }
}
