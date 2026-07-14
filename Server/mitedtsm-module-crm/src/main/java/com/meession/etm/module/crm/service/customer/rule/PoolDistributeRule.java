package com.meession.etm.module.crm.service.customer.rule;

import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import com.meession.etm.module.crm.service.customer.bo.CrmCustomerPoolDistributeRuleConfig;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolRuleTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PoolDistributeRule extends AbstractPoolRule {

    @Override
    public Integer getRuleType() {
        return CrmCustomerPoolRuleTypeEnum.DISTRIBUTE.getType();
    }

    @Override
    public void execute(CrmCustomerPoolRuleDO rule) {
        log.info("开始执行公海分配规则: {}", rule.getName());
        CrmCustomerPoolDistributeRuleConfig config = parseConfig(rule, CrmCustomerPoolDistributeRuleConfig.class);
    }

}
