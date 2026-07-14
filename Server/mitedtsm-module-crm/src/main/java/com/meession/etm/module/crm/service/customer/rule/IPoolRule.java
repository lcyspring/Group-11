package com.meession.etm.module.crm.service.customer.rule;

import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;

public interface IPoolRule {

    Integer getRuleType();

    void execute(CrmCustomerPoolRuleDO rule);

    boolean supports(CrmCustomerPoolRuleDO rule);

}
