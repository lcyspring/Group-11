package com.meession.etm.module.trade.service;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderPageReqVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderSaveReqVO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderItemDO;

import java.util.List;

public interface TradeOrderService {

    Long createOrder(TradeOrderSaveReqVO createReqVO);

    void updateOrder(TradeOrderSaveReqVO updateReqVO);

    void deleteOrder(Long id);

    void deleteOrderList(List<Long> ids);

    PageResult<TradeOrderDO> getOrderPage(TradeOrderPageReqVO reqVO);

    TradeOrderDO getOrder(Long id);

    TradeOrderDO getOrderWithItems(Long id);

    List<TradeOrderDO> getOrdersByUserId(Long userId);

    List<TradeOrderItemDO> getOrderItems(Long orderId);

    TradeOrderItemDO getOrderItem(Long id);

    void updateOrderStatus(Long id, Integer status);

}