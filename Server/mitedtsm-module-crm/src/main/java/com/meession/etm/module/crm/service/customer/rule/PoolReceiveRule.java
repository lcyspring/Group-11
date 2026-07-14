package com.meession.etm.module.crm.service.customer.rule;

import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import com.meession.etm.module.crm.service.customer.bo.CrmCustomerPoolReceiveRuleConfig;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolRuleTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PoolReceiveRule extends AbstractPoolRule {

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
        if (config.getDailyLimit() != null) {
            if (getDailyReceiveCount(userId) + count > config.getDailyLimit()) {
                return false;
            }
        }
        if (config.getWeeklyLimit() != null) {
            if (getWeeklyReceiveCount(userId) + count > config.getWeeklyLimit()) {
                return false;
            }
        }
        if (config.getMonthlyLimit() != null) {
            if (getMonthlyReceiveCount(userId) + count > config.getMonthlyLimit()) {
                return false;
            }
        }
        return true;
    }

    private long getDailyReceiveCount(Long userId) {
        return 0;
    }

    private long getWeeklyReceiveCount(Long userId) {
        return 0;
    }

    private long getMonthlyReceiveCount(Long userId) {
        return 0;
    }

}
