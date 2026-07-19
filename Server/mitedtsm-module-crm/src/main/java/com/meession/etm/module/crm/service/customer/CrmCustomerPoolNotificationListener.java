package com.meession.etm.module.crm.service.customer;

import com.meession.etm.module.system.api.notify.NotifyMessageSendApi;
import com.meession.etm.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/** Sends public-pool notifications after the customer transaction has committed. */
@Component
@Slf4j
public class CrmCustomerPoolNotificationListener {

    public static final String PUT_POOL_TEMPLATE = "crm-customer-put-pool";

    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPutPool(CrmCustomerPutPoolEvent event) {
        if (event.previousOwnerUserId() == null) {
            return;
        }
        try {
            notifyMessageSendApi.sendSingleMessageToAdmin(new NotifySendSingleToUserReqDTO()
                    .setUserId(event.previousOwnerUserId())
                    .setTemplateCode(PUT_POOL_TEMPLATE)
                    .setTemplateParams(Map.of("customerName", event.customerName(),
                            "reason", event.reason())));
        } catch (RuntimeException ex) {
            // The ownership transaction is already committed. Notification failure must be retriable/auditable,
            // but must not report the customer as still owned.
            log.error("[onPutPool][customerId({}) ownerUserId({}) source({}) notification failed]",
                    event.customerId(), event.previousOwnerUserId(), event.source(), ex);
        }
    }
}
