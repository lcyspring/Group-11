package com.meession.etm.module.crm.service.reimbursement.listener;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.meession.etm.module.crm.framework.reimbursement.CrmReimbursementProperties;
import com.meession.etm.module.crm.service.reimbursement.CrmReimbursementService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CrmReimbursementStatusListener extends BpmProcessInstanceStatusEventListener {
    @Resource private CrmReimbursementService reimbursementService;
    @Resource private CrmReimbursementProperties properties;

    @Override
    public String getProcessDefinitionKey() {
        return properties.getProcessDefinitionKey();
    }

    @Override
    public void onEvent(BpmProcessInstanceStatusEvent event) {
        reimbursementService.updateAuditStatus(Long.parseLong(event.getBusinessKey()), event.getId(), event.getStatus());
    }
}
