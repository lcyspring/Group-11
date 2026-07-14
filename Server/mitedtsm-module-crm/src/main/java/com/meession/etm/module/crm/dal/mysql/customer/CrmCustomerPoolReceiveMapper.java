package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolReceiveDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrmCustomerPoolReceiveMapper extends BaseMapperX<CrmCustomerPoolReceiveDO> {

    default List<CrmCustomerPoolReceiveDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerPoolReceiveDO>()
                .eq(CrmCustomerPoolReceiveDO::getCustomerId, customerId)
                .orderByDesc(CrmCustomerPoolReceiveDO::getReceiveTime));
    }

    default List<CrmCustomerPoolReceiveDO> selectListByReceiveUserId(Long receiveUserId) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerPoolReceiveDO>()
                .eq(CrmCustomerPoolReceiveDO::getReceiveUserId, receiveUserId)
                .orderByDesc(CrmCustomerPoolReceiveDO::getReceiveTime));
    }

    default Long selectCountByReceiveUserIdAndPeriod(Long receiveUserId, LocalDateTime startTime, LocalDateTime endTime) {
        return selectCount(new LambdaQueryWrapperX<CrmCustomerPoolReceiveDO>()
                .eq(CrmCustomerPoolReceiveDO::getReceiveUserId, receiveUserId)
                .between(CrmCustomerPoolReceiveDO::getReceiveTime, startTime, endTime));
    }

    default CrmCustomerPoolReceiveDO selectLatestByCustomerId(Long customerId) {
        return selectOne(new LambdaQueryWrapperX<CrmCustomerPoolReceiveDO>()
                .eq(CrmCustomerPoolReceiveDO::getCustomerId, customerId)
                .orderByDesc(CrmCustomerPoolReceiveDO::getReceiveTime)
                .last("LIMIT 1"));
    }

}
