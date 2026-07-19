package com.meession.etm.module.crm.service.fulfillment.listener;

import com.meession.etm.module.crm.service.fulfillment.CrmContractFulfillmentStateService;
import com.meession.etm.module.erp.api.sale.event.ErpSaleOrderChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CrmErpSaleOrderChangedListener {

    private final CrmContractFulfillmentStateService stateService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSaleOrderChanged(ErpSaleOrderChangedEvent event) {
        stateService.syncFromErp(event.order());
    }
}
