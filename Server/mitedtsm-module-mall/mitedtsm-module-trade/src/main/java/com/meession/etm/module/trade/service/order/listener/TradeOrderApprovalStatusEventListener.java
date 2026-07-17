package com.meession.etm.module.trade.service.order.listener;

import com.meession.etm.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.meession.etm.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.meession.etm.module.trade.service.order.TradeOrderApprovalService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TradeOrderApprovalStatusEventListener {

    @Resource
    private TradeOrderApprovalService tradeOrderApprovalService;

    @EventListener
    public void onProcessInstanceStatusChange(BpmProcessInstanceStatusEvent event) {
        if (!"trade_order".equals(event.getProcessDefinitionKey())) {
            return;
        }

        log.info("订单审批流程状态变更: processDefinitionKey={}, businessKey={}, status={}",
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus());

        Integer approvalStatus = convertStatus(event.getStatus());
        tradeOrderApprovalService.updateApprovalStatus(event.getBusinessKey(), approvalStatus, event.getReason());
    }

    private Integer convertStatus(Integer bpmStatus) {
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(bpmStatus)) {
            return com.meession.etm.module.trade.enums.order.TradeOrderApprovalStatusEnum.APPROVED.getStatus();
        } else if (BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(bpmStatus)) {
            return com.meession.etm.module.trade.enums.order.TradeOrderApprovalStatusEnum.REJECTED.getStatus();
        } else if (BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(bpmStatus)) {
            return com.meession.etm.module.trade.enums.order.TradeOrderApprovalStatusEnum.CANCELLED.getStatus();
        }
        return com.meession.etm.module.trade.enums.order.TradeOrderApprovalStatusEnum.PENDING.getStatus();
    }

}