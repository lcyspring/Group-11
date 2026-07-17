package com.meession.etm.module.crm.service.visit.listener;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.meession.etm.module.crm.service.visit.CrmCustomerVisitService;
import com.meession.etm.module.crm.service.visit.CrmCustomerVisitServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CrmCustomerVisitStatusListener extends BpmProcessInstanceStatusEventListener {
    @Resource private CrmCustomerVisitService visitService;
    @Override public String getProcessDefinitionKey() { return CrmCustomerVisitServiceImpl.PROCESS_KEY; }
    @Override public void onEvent(BpmProcessInstanceStatusEvent event) {
        visitService.updateAuditStatus(Long.parseLong(event.getBusinessKey()), event.getId(), event.getStatus());
    }
}
