package com.meession.etm.module.trade.service.order;

import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.TradeOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.trade.enums.ErrorCodeConstants.ORDER_NOT_EXISTS;
import static com.meession.etm.module.trade.enums.ErrorCodeConstants.ORDER_STATUS_CANNOT_TRANSITION;

@Service
public class TradeOrderStatusServiceImpl implements TradeOrderStatusService {

    private static final Map<TradeOrderStatusEnum, Set<TradeOrderStatusEnum>> ALLOWED_TRANSITIONS = Map.of(
            TradeOrderStatusEnum.CREATE, EnumSet.of(TradeOrderStatusEnum.PAID, TradeOrderStatusEnum.CANCEL),
            TradeOrderStatusEnum.PAID, EnumSet.of(TradeOrderStatusEnum.DELIVERY, TradeOrderStatusEnum.CANCEL, TradeOrderStatusEnum.REFUND),
            TradeOrderStatusEnum.DELIVERY, EnumSet.of(TradeOrderStatusEnum.RECEIVE, TradeOrderStatusEnum.REFUND),
            TradeOrderStatusEnum.RECEIVE, EnumSet.of(TradeOrderStatusEnum.REFUND),
            TradeOrderStatusEnum.CANCEL, Collections.emptySet(),
            TradeOrderStatusEnum.REFUND, EnumSet.of(TradeOrderStatusEnum.REFUNDED),
            TradeOrderStatusEnum.REFUNDED, Collections.emptySet()
    );

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long orderId, TradeOrderStatusEnum targetStatus) {
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw exception(ORDER_NOT_EXISTS);
        }

        TradeOrderStatusEnum currentStatus = TradeOrderStatusEnum.valueOf(order.getStatus());
        if (!canTransition(currentStatus, targetStatus)) {
            throw exception(ORDER_STATUS_CANNOT_TRANSITION);
        }

        TradeOrderDO updateObj = new TradeOrderDO();
        updateObj.setId(orderId);
        updateObj.setStatus(targetStatus.getStatus());
        tradeOrderMapper.updateById(updateObj);
    }

    @Override
    public boolean canTransition(TradeOrderStatusEnum currentStatus, TradeOrderStatusEnum targetStatus) {
        Set<TradeOrderStatusEnum> allowedTargets = ALLOWED_TRANSITIONS.get(currentStatus);
        return allowedTargets != null && allowedTargets.contains(targetStatus);
    }

}