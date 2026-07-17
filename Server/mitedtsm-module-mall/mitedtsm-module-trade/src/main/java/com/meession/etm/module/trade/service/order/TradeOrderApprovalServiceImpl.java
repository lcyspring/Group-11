package com.meession.etm.module.trade.service.order;

import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.ErrorCodeConstants;
import com.meession.etm.module.trade.enums.order.TradeOrderApprovalStatusEnum;
import com.meession.etm.module.trade.enums.order.TradeOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class TradeOrderApprovalServiceImpl implements TradeOrderApprovalService {

    public static final String PROCESS_KEY = "trade_order";

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long userId, Long orderId) {
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
        }

        if (!TradeOrderStatusEnum.PAID.getStatus().equals(order.getStatus())) {
            throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
        }

        if (order.getApprovalStatus() != null && !TradeOrderApprovalStatusEnum.PENDING.getStatus().equals(order.getApprovalStatus())) {
            throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
        }

        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("orderNo", order.getNo());
        processInstanceVariables.put("payPrice", order.getPayPrice());
        processInstanceVariables.put("userId", order.getUserId());

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables)
                        .setBusinessKey(String.valueOf(orderId)));

        tradeOrderMapper.updateById(new TradeOrderDO()
                .setId(orderId)
                .setApprovalStatus(TradeOrderApprovalStatusEnum.PENDING.getStatus())
                .setProcessInstanceId(processInstanceId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelApproval(Long orderId) {
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
        }

        if (!TradeOrderApprovalStatusEnum.PENDING.getStatus().equals(order.getApprovalStatus())) {
            throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
        }

        tradeOrderMapper.updateById(new TradeOrderDO()
                .setId(orderId)
                .setApprovalStatus(TradeOrderApprovalStatusEnum.CANCELLED.getStatus())
                .setProcessInstanceId(null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalStatus(String businessKey, Integer approvalStatus, String comment) {
        Long orderId = Long.parseLong(businessKey);
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            return;
        }

        TradeOrderDO updateObj = new TradeOrderDO()
                .setId(orderId)
                .setApprovalStatus(approvalStatus)
                .setApprovalComment(comment)
                .setApprovalTime(LocalDateTime.now());

        if (TradeOrderApprovalStatusEnum.APPROVED.getStatus().equals(approvalStatus)) {
            updateObj.setStatus(TradeOrderStatusEnum.DELIVERY.getStatus());
        } else if (TradeOrderApprovalStatusEnum.REJECTED.getStatus().equals(approvalStatus)) {
            updateObj.setStatus(TradeOrderStatusEnum.CANCEL.getStatus());
        }

        tradeOrderMapper.updateById(updateObj);
    }

}