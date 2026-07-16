package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmCustomerCarePlanPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCarePlanDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmCustomerCarePlanMapper extends BaseMapperX<CrmCustomerCarePlanDO> {
    default CrmCustomerCarePlanDO selectByCode(String code) { return selectOne(CrmCustomerCarePlanDO::getCode, code); }
    default PageResult<CrmCustomerCarePlanDO> selectPage(CrmCustomerCarePlanPageReqVO request) {
        return selectPage(request, new LambdaQueryWrapperX<CrmCustomerCarePlanDO>()
                .likeIfPresent(CrmCustomerCarePlanDO::getCode, request.getCode())
                .likeIfPresent(CrmCustomerCarePlanDO::getName, request.getName())
                .eqIfPresent(CrmCustomerCarePlanDO::getRuleType, request.getRuleType())
                .eqIfPresent(CrmCustomerCarePlanDO::getChannel, request.getChannel())
                .eqIfPresent(CrmCustomerCarePlanDO::getEnabled, request.getEnabled())
                .orderByDesc(CrmCustomerCarePlanDO::getId));
    }
    default List<CrmCustomerCarePlanDO> selectEnabledByEventDay(String eventDay) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerCarePlanDO>()
                .eq(CrmCustomerCarePlanDO::getEnabled, true)
                .and(query -> query.eq(CrmCustomerCarePlanDO::getRuleType, 1)
                        .or(rule -> rule.eq(CrmCustomerCarePlanDO::getRuleType, 2)
                                .eq(CrmCustomerCarePlanDO::getEventMonthDay, eventDay))
                        .or().eq(CrmCustomerCarePlanDO::getRuleType, 3)));
    }

    default int updateEnabled(Long id, Boolean enabled) {
        return update(new LambdaUpdateWrapper<CrmCustomerCarePlanDO>()
                .eq(CrmCustomerCarePlanDO::getId, id)
                .set(CrmCustomerCarePlanDO::getEnabled, enabled));
    }

    default int deleteDisabled(Long id) {
        return delete(new LambdaUpdateWrapper<CrmCustomerCarePlanDO>()
                .eq(CrmCustomerCarePlanDO::getId, id)
                .eq(CrmCustomerCarePlanDO::getEnabled, false));
    }
}
