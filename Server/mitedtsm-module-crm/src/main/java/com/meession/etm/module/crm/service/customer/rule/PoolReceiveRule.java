package com.meession.etm.module.crm.service.customer.rule;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerPoolReceiveMapper;
import com.meession.etm.module.crm.service.customer.bo.CrmCustomerPoolReceiveRuleConfig;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolRuleTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class PoolReceiveRule extends AbstractPoolRule {

    @Resource
    private CrmCustomerPoolReceiveMapper poolReceiveMapper;

    @Override
    public Integer getRuleType() {
        return CrmCustomerPoolRuleTypeEnum.RECEIVE.getType();
    }

    @Override
    public void execute(CrmCustomerPoolRuleDO rule) {
        log.info("公海领取规则检查: {}", rule.getName());
        CrmCustomerPoolReceiveRuleConfig config = parseConfig(rule, CrmCustomerPoolReceiveRuleConfig.class);
    }

    public boolean checkLimit(Long userId, int count, CrmCustomerPoolReceiveRuleConfig config) {
        if (config == null) {
            return true;
        }
        if (config.getDailyLimit() != null && config.getDailyLimit() > 0) {
            if (getDailyReceiveCount(userId) + count > config.getDailyLimit()) {
                log.warn("[checkLimit][用户({}) 日领取数量超过限制]", userId);
                return false;
            }
        }
        if (config.getWeeklyLimit() != null && config.getWeeklyLimit() > 0) {
            if (getWeeklyReceiveCount(userId) + count > config.getWeeklyLimit()) {
                log.warn("[checkLimit][用户({}) 周领取数量超过限制]", userId);
                return false;
            }
        }
        if (config.getMonthlyLimit() != null && config.getMonthlyLimit() > 0) {
            if (getMonthlyReceiveCount(userId) + count > config.getMonthlyLimit()) {
                log.warn("[checkLimit][用户({}) 月领取数量超过限制]", userId);
                return false;
            }
        }
        return true;
    }

    public long getDailyReceiveCount(Long userId) {
        LocalDateTime beginOfDay = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        LocalDateTime endOfDay = LocalDateTimeUtil.endOfDay(LocalDateTime.now());
        return poolReceiveMapper.selectCountByReceiveUserIdAndPeriod(userId, beginOfDay, endOfDay);
    }

    public long getWeeklyReceiveCount(Long userId) {
        LocalDateTime beginOfWeek = LocalDateTimeUtil.beginOfWeek(LocalDateTime.now());
        LocalDateTime endOfWeek = LocalDateTimeUtil.endOfWeek(LocalDateTime.now());
        return poolReceiveMapper.selectCountByReceiveUserIdAndPeriod(userId, beginOfWeek, endOfWeek);
    }

    public long getMonthlyReceiveCount(Long userId) {
        LocalDateTime beginOfMonth = LocalDateTimeUtil.beginOfMonth(LocalDateTime.now());
        LocalDateTime endOfMonth = LocalDateTimeUtil.endOfMonth(LocalDateTime.now());
        return poolReceiveMapper.selectCountByReceiveUserIdAndPeriod(userId, beginOfMonth, endOfMonth);
    }

}
