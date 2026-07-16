package com.meession.etm.module.crm.service.contract.listener;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.meession.etm.module.crm.framework.contract.CrmContractAmendmentProperties;
import com.meession.etm.module.crm.service.contract.CrmContractAmendmentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CrmContractAmendmentStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource private CrmContractAmendmentService service;
    @Resource private CrmContractAmendmentProperties properties;

    @Override
    public String getProcessDefinitionKey() {
        return properties.getProcessDefinitionKey();
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        service.updateAuditStatus(Long.parseLong(event.getBusinessKey()), event.getId(), event.getStatus());
    }
}
