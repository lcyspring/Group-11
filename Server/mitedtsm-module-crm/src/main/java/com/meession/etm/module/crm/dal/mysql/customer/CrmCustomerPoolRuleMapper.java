package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.customer.vo.poolrule.CrmCustomerPoolRulePageReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmCustomerPoolRuleMapper extends BaseMapperX<CrmCustomerPoolRuleDO> {

    default List<CrmCustomerPoolRuleDO> selectListByRuleType(Integer ruleType) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerPoolRuleDO>()
                .eq(CrmCustomerPoolRuleDO::getRuleType, ruleType)
                .eq(CrmCustomerPoolRuleDO::getEnabled, true)
                .orderByAsc(CrmCustomerPoolRuleDO::getSort));
    }

    default List<CrmCustomerPoolRuleDO> selectListByExecuteType(Integer executeType) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerPoolRuleDO>()
                .eq(CrmCustomerPoolRuleDO::getExecuteType, executeType)
                .eq(CrmCustomerPoolRuleDO::getEnabled, true));
    }

    default List<CrmCustomerPoolRuleDO> selectRecycleRules() {
        return selectListByRuleType(1);
    }

    default List<CrmCustomerPoolRuleDO> selectReceiveRules() {
        return selectListByRuleType(2);
    }

    default List<CrmCustomerPoolRuleDO> selectDistributeRules() {
        return selectListByRuleType(3);
    }

    default PageResult<CrmCustomerPoolRuleDO> selectPage(CrmCustomerPoolRulePageReqVO pageReqVO) {
        LambdaQueryWrapperX<CrmCustomerPoolRuleDO> query = new LambdaQueryWrapperX<>();
        query.likeIfPresent(CrmCustomerPoolRuleDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmCustomerPoolRuleDO::getRuleType, pageReqVO.getRuleType())
                .eqIfPresent(CrmCustomerPoolRuleDO::getEnabled, pageReqVO.getEnabled())
                .orderByAsc(CrmCustomerPoolRuleDO::getSort)
                .orderByDesc(CrmCustomerPoolRuleDO::getCreateTime);
        return selectPage(pageReqVO, query);
    }

}
