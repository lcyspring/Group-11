package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerOwnerRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CRM 客户公海归属变更记录 Mapper。
 */
@Mapper
public interface CrmCustomerOwnerRecordMapper extends BaseMapperX<CrmCustomerOwnerRecordDO> {
}
