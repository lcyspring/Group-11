package com.meession.etm.module.crm.service.customer.rule;

import cn.hutool.json.JSONUtil;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractPoolRule implements IPoolRule {

    @Override
    public boolean supports(CrmCustomerPoolRuleDO rule) {
        return getRuleType().equals(rule.getRuleType());
    }

    protected <T> T parseConfig(CrmCustomerPoolRuleDO rule, Class<T> clazz) {
        if (rule.getConfig() == null) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.error("parseConfig error", e);
                return null;
            }
        }
        return JSONUtil.toBean(rule.getConfig(), clazz);
    }

}
