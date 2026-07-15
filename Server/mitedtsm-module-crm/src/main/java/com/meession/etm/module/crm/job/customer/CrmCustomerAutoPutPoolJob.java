package com.meession.etm.module.crm.job.customer;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.job.TenantJob;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.customer.CrmCustomerGarbageService;
import com.meession.etm.module.crm.service.clue.CrmCluePublicPoolService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 客户自动掉入公海 Job
 *
 * @author 密讯
 */
@Component
public class CrmCustomerAutoPutPoolJob implements JobHandler {

    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmCustomerGarbageService customerGarbageService;
    @Resource
    private CrmCluePublicPoolService cluePublicPoolService;

    @Override
    @TenantJob
    public String execute(String param) {
        int count = customerService.autoPutCustomerPool();
        int garbageCount = customerGarbageService.autoPutCustomerGarbage();
        int clueCount = cluePublicPoolService.autoPutCluePublicPool();
        return String.format("掉入公海客户 %s 个，转入垃圾池客户 %s 个，进入公共池线索 %s 个",
                count, garbageCount, clueCount);
    }

}
