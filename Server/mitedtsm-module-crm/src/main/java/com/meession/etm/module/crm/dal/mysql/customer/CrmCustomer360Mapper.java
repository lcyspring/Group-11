package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomer360SummaryRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CrmCustomer360Mapper {

    CrmCustomer360SummaryRespVO selectSummary(@Param("customerId") Long customerId,
                                               @Param("userId") Long userId,
                                               @Param("queryAllWorkOrders") boolean queryAllWorkOrders);
}
