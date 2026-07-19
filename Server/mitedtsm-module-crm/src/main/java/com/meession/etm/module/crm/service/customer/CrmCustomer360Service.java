package com.meession.etm.module.crm.service.customer;

import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomer360SummaryRespVO;

public interface CrmCustomer360Service {

    CrmCustomer360SummaryRespVO getSummary(Long customerId, Long userId, boolean queryAllWorkOrders);
}
