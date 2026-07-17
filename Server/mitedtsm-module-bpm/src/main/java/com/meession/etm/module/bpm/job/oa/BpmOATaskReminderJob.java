package com.meession.etm.module.bpm.job.oa;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.job.TenantJob;
import com.meession.etm.module.bpm.service.oa.BpmOATaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class BpmOATaskReminderJob implements JobHandler {
    private final BpmOATaskService service;
    @Override @TenantJob public String execute(String param) {
        int limit = 100;
        if (param != null && !param.isBlank()) try { limit = Integer.parseInt(param.trim()); } catch (NumberFormatException ignored) { }
        return String.format("发送 OA 任务提醒 %s 条", service.remindDue(limit));
    }
}
