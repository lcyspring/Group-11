package com.meession.etm.module.bpm.job.oa;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.job.TenantJob;
import com.meession.etm.module.bpm.service.oa.BpmOAEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BpmOAEventReminderJob implements JobHandler {
    private final BpmOAEventService service;

    @Override
    @TenantJob
    public String execute(String param) {
        int limit = 100;
        if (param != null && !param.isBlank()) {
            try { limit = Integer.parseInt(param.trim()); } catch (NumberFormatException ignored) { }
        }
        return String.format("发送 OA 日程提醒 %s 条", service.remindDue(limit));
    }
}
