package com.meession.etm.module.bpm.service.oa.listener;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.meession.etm.module.bpm.service.oa.BpmOATripService;
import com.meession.etm.module.bpm.service.oa.BpmOATripServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class BpmOATripStatusListener extends BpmProcessInstanceStatusEventListener {
    @Resource
    private BpmOATripService tripService;

    @Override
    protected String getProcessDefinitionKey() {
        return BpmOATripServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        tripService.updateTripStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }
}
