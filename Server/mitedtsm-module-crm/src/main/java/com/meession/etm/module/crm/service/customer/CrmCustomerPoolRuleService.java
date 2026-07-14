package com.meession.etm.module.crm.service.customer;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.customer.vo.poolrule.CrmCustomerPoolRulePageReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.poolrule.CrmCustomerPoolRuleSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;

import java.util.List;

public interface CrmCustomerPoolRuleService {

    Long createPoolRule(CrmCustomerPoolRuleSaveReqVO saveReqVO);

    void updatePoolRule(CrmCustomerPoolRuleSaveReqVO saveReqVO);

    void deletePoolRule(Long id);

    CrmCustomerPoolRuleDO getPoolRule(Long id);

    List<CrmCustomerPoolRuleDO> getPoolRuleList();

    PageResult<CrmCustomerPoolRuleDO> getPoolRulePage(CrmCustomerPoolRulePageReqVO pageReqVO);

    void enablePoolRule(Long id, Boolean enabled);

    void executeRule(Long id);

}
