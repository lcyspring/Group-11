package com.meession.etm.module.bpm.service.oa.listener;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.meession.etm.module.bpm.service.oa.BpmOABusinessTripService;
import com.meession.etm.module.bpm.service.oa.BpmOABusinessTripServiceImpl;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
public class BpmOABusinessTripStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private BpmOABusinessTripService businessTripService;

    @Override
    protected String getProcessDefinitionKey() {
        return BpmOABusinessTripServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        businessTripService.updateBusinessTripStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }

}