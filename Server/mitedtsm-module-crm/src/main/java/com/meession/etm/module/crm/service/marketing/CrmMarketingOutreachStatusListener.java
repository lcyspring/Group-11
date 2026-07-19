package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CrmMarketingOutreachStatusListener extends BpmProcessInstanceStatusEventListener {
    @Resource private CrmMarketingOutreachService service;
    @Resource private com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties properties;

    @Override
    public String getProcessDefinitionKey() {
        return properties.getProcessDefinitionKey();
    }

    @Override
    public void onEvent(BpmProcessInstanceStatusEvent event) {
        service.updateApprovalStatus(Long.parseLong(event.getBusinessKey()), event.getId(), event.getStatus());
    }
}
