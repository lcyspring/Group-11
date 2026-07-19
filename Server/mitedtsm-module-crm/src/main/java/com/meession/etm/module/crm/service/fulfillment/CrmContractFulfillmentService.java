package com.meession.etm.module.crm.service.fulfillment;

import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmContractFulfillmentRespVO;

public interface CrmContractFulfillmentService {

    CrmContractFulfillmentRespVO getFulfillment(Long contractId);

    CrmContractFulfillmentRespVO createOrRetry(Long contractId);

    CrmContractFulfillmentRespVO refresh(Long contractId);
}
