package com.meession.etm.module.crm.service.customer;

import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.customer.vo.poolrule.CrmCustomerPoolRulePageReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.poolrule.CrmCustomerPoolRuleSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerPoolRuleMapper;
import com.meession.etm.module.crm.service.customer.rule.PoolRuleEngine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_LIMIT_CONFIG_NOT_EXISTS;

@Service
@Slf4j
@Validated
public class CrmCustomerPoolRuleServiceImpl implements CrmCustomerPoolRuleService {

    @Resource
    private CrmCustomerPoolRuleMapper ruleMapper;

    @Resource
    private PoolRuleEngine ruleEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPoolRule(CrmCustomerPoolRuleSaveReqVO saveReqVO) {
        CrmCustomerPoolRuleDO rule = BeanUtils.toBean(saveReqVO, CrmCustomerPoolRuleDO.class);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePoolRule(CrmCustomerPoolRuleSaveReqVO saveReqVO) {
        CrmCustomerPoolRuleDO rule = validatePoolRuleExists(saveReqVO.getId());
        BeanUtils.copyProperties(saveReqVO, rule);
        ruleMapper.updateById(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePoolRule(Long id) {
        validatePoolRuleExists(id);
        ruleMapper.deleteById(id);
    }

    @Override
    public CrmCustomerPoolRuleDO getPoolRule(Long id) {
        return ruleMapper.selectById(id);
    }

    @Override
    public List<CrmCustomerPoolRuleDO> getPoolRuleList() {
        return ruleMapper.selectList();
    }

    @Override
    public PageResult<CrmCustomerPoolRuleDO> getPoolRulePage(CrmCustomerPoolRulePageReqVO pageReqVO) {
        return ruleMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enablePoolRule(Long id, Boolean enabled) {
        CrmCustomerPoolRuleDO rule = validatePoolRuleExists(id);
        rule.setEnabled(enabled);
        ruleMapper.updateById(rule);
    }

    @Override
    public void executeRule(Long id) {
        CrmCustomerPoolRuleDO rule = validatePoolRuleExists(id);
        ruleEngine.executeRule(rule);
    }

    private CrmCustomerPoolRuleDO validatePoolRuleExists(Long id) {
        CrmCustomerPoolRuleDO rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw exception(CUSTOMER_LIMIT_CONFIG_NOT_EXISTS);
        }
        return rule;
    }

}
