package com.meession.etm.module.bpm.service.oa.listener;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.meession.etm.module.bpm.service.oa.BpmOALoanService;
import com.meession.etm.module.bpm.service.oa.BpmOALoanServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class BpmOALoanStatusListener extends BpmProcessInstanceStatusEventListener {
    @Resource private BpmOALoanService loanService;
    @Override protected String getProcessDefinitionKey() { return BpmOALoanServiceImpl.PROCESS_KEY; }
    @Override protected void onEvent(BpmProcessInstanceStatusEvent event) {
        loanService.updateLoanStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }
}
