package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerOwnerRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CRM 客户归属变更记录 Mapper。
 */
@Mapper
public interface CrmCustomerOwnerRecordMapper extends BaseMapperX<CrmCustomerOwnerRecordDO> {

    default List<CrmCustomerOwnerRecordDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerOwnerRecordDO>()
                .eq(CrmCustomerOwnerRecordDO::getCustomerId, customerId)
                .orderByDesc(CrmCustomerOwnerRecordDO::getId));
    }

}
