package com.meession.etm.module.crm.service.refund.listener;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.meession.etm.module.crm.framework.refund.CrmReceivableRefundProperties;
import com.meession.etm.module.crm.service.refund.CrmReceivableRefundService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CrmReceivableRefundStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource private CrmReceivableRefundService refundService;
    @Resource private CrmReceivableRefundProperties properties;

    @Override
    public String getProcessDefinitionKey() {
        return properties.getProcessDefinitionKey();
    }

    @Override
    public void onEvent(BpmProcessInstanceStatusEvent event) {
        refundService.updateRefundAuditStatus(Long.parseLong(event.getBusinessKey()), event.getId(), event.getStatus());
    }
}
