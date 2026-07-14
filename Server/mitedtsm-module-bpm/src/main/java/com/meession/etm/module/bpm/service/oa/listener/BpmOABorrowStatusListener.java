package com.meession.etm.module.bpm.service.oa.listener;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.meession.etm.module.bpm.service.oa.BpmOABorrowService;
import com.meession.etm.module.bpm.service.oa.BpmOABorrowServiceImpl;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
public class BpmOABorrowStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private BpmOABorrowService borrowService;

    @Override
    protected String getProcessDefinitionKey() {
        return BpmOABorrowServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        borrowService.updateBorrowStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }

}