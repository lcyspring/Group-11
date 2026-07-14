package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolReceiveLimitDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrmCustomerPoolReceiveLimitMapper extends BaseMapperX<CrmCustomerPoolReceiveLimitDO> {

    default CrmCustomerPoolReceiveLimitDO selectByUserIdAndLimitType(Long userId, Integer limitType) {
        return selectOne(new LambdaQueryWrapperX<CrmCustomerPoolReceiveLimitDO>()
                .eq(CrmCustomerPoolReceiveLimitDO::getUserId, userId)
                .eq(CrmCustomerPoolReceiveLimitDO::getLimitType, limitType));
    }

    default List<CrmCustomerPoolReceiveLimitDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerPoolReceiveLimitDO>()
                .eq(CrmCustomerPoolReceiveLimitDO::getUserId, userId));
    }

    default int updateUsedCount(Long id, Integer increment) {
        return update(new LambdaUpdateWrapper<CrmCustomerPoolReceiveLimitDO>()
                .eq(CrmCustomerPoolReceiveLimitDO::getId, id)
                .setSql("used_count = used_count + " + increment));
    }

    default int resetUsedCount(Long userId, Integer limitType, LocalDateTime newPeriodStartTime, LocalDateTime newPeriodEndTime) {
        return update(new LambdaUpdateWrapper<CrmCustomerPoolReceiveLimitDO>()
                .eq(CrmCustomerPoolReceiveLimitDO::getUserId, userId)
                .eq(CrmCustomerPoolReceiveLimitDO::getLimitType, limitType)
                .set(CrmCustomerPoolReceiveLimitDO::getUsedCount, 0)
                .set(CrmCustomerPoolReceiveLimitDO::getPeriodStartTime, newPeriodStartTime)
                .set(CrmCustomerPoolReceiveLimitDO::getPeriodEndTime, newPeriodEndTime));
    }

}
