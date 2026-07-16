package com.meession.etm.module.marketing.job;

import com.meession.etm.framework.quartz.core.handler.JobHandler;
import com.meession.etm.framework.tenant.core.job.TenantJob;
import com.meession.etm.module.marketing.service.care.CustomerCareService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 客户关怀定时任务
 * <p>
 * 建议 Cron 表达式：每日 08:00 执行
 * 注意：节日场景暂仅支持公历固定日期。农历节日需手动配置公历日期。
 *
 * @author MITEDTSM
 */
@Slf4j
@Component
public class CustomerCareJob implements JobHandler {

    @Resource
    private CustomerCareService customerCareService;

    @Override
    @TenantJob
    public String execute(String param) {
        log.info("[CustomerCareJob][开始执行客户关怀定时任务]");

        int birthdaySent = customerCareService.executeBirthdayCare();
        log.info("[CustomerCareJob][生日关怀完成，发送({})]", birthdaySent);

        int holidaySent = customerCareService.executeHolidayCare();
        log.info("[CustomerCareJob][节日关怀完成，发送({})]", holidaySent);

        int total = birthdaySent + holidaySent;
        log.info("[CustomerCareJob][客户关怀任务完成，总计发送({})]", total);
        return "生日关怀:" + birthdaySent + ", 节日关怀:" + holidaySent + ", 合计:" + total;
    }

}
