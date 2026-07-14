package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerLifecycleRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmCustomerLifecycleRecordMapper extends BaseMapperX<CrmCustomerLifecycleRecordDO> {

    default List<CrmCustomerLifecycleRecordDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerLifecycleRecordDO>()
                .eq(CrmCustomerLifecycleRecordDO::getCustomerId, customerId)
                .orderByDesc(CrmCustomerLifecycleRecordDO::getId));
    }

}
