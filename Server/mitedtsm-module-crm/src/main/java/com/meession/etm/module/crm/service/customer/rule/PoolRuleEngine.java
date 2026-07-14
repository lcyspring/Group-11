package com.meession.etm.module.crm.service.customer.rule;

import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerPoolRuleMapper;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolRuleTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PoolRuleEngine {

    @Resource
    private CrmCustomerPoolRuleMapper ruleMapper;

    @Resource
    private List<IPoolRule> rules;

    private Map<Integer, IPoolRule> ruleMap;

    private Map<Integer, IPoolRule> getRuleMap() {
        if (ruleMap == null) {
            ruleMap = rules.stream()
                    .collect(Collectors.toMap(IPoolRule::getRuleType, Function.identity()));
        }
        return ruleMap;
    }

    public void executeAllRecycleRules() {
        List<CrmCustomerPoolRuleDO> recycleRules = ruleMapper.selectRecycleRules();
        for (CrmCustomerPoolRuleDO rule : recycleRules) {
            executeRule(rule);
        }
    }

    public void executeAllDistributeRules() {
        List<CrmCustomerPoolRuleDO> distributeRules = ruleMapper.selectDistributeRules();
        for (CrmCustomerPoolRuleDO rule : distributeRules) {
            executeRule(rule);
        }
    }

    public void executeRule(CrmCustomerPoolRuleDO rule) {
        IPoolRule executor = getRuleMap().get(rule.getRuleType());
        if (executor != null && executor.supports(rule)) {
            executor.execute(rule);
        } else {
            log.warn("No rule executor found for rule type: {}", rule.getRuleType());
        }
    }

    public IPoolRule getRule(Integer ruleType) {
        return getRuleMap().get(ruleType);
    }

    public PoolReceiveRule getReceiveRule() {
        return (PoolReceiveRule) getRuleMap().get(CrmCustomerPoolRuleTypeEnum.RECEIVE.getType());
    }

}
