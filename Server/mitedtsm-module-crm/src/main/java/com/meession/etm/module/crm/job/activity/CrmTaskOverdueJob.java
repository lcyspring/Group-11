package com.meession.etm.module.crm.job.activity;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.job.TenantJob;
import com.meession.etm.module.crm.service.activity.CrmActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrmTaskOverdueJob implements JobHandler {

    private final CrmActivityService activityService;

    @Override
    @TenantJob
    public String execute(String param) {
        int count = activityService.markOverdueTasks();
        return String.format("标记超时 CRM 任务 %s 个", count);
    }
}
