package com.meession.etm.module.crm.service.fulfillment;

import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmContractFulfillmentDO;
import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderCreateReqDTO;
import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderRespDTO;

public interface CrmContractFulfillmentStateService {

    Preparation prepare(Long contractId);

    void markCreated(Long fulfillmentId, String requestHash, ErpSaleOrderRespDTO order);

    void markFailed(Long fulfillmentId, String errorCode, String errorMessage);

    void syncFromErp(ErpSaleOrderRespDTO order);

    record Preparation(CrmContractFulfillmentDO fulfillment, ErpSaleOrderCreateReqDTO request,
                       boolean alreadyCreated) {
    }
}
