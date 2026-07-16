package com.meession.etm.module.trade.service.order;

import com.meession.etm.module.trade.enums.TradeOrderStatusEnum;

public interface TradeOrderStatusService {

    void updateStatus(Long orderId, TradeOrderStatusEnum targetStatus);

    boolean canTransition(TradeOrderStatusEnum currentStatus, TradeOrderStatusEnum targetStatus);

}