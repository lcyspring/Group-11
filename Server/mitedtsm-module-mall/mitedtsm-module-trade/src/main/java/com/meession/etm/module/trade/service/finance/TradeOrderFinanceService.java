package com.meession.etm.module.trade.service.finance;

import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderPaymentReqVO;
import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderPaymentRespVO;
import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderRefundReqVO;

import java.util.List;

/**
 * 订单财务服务接口
 */
public interface TradeOrderFinanceService {

    Long processPayment(Long userId, TradeOrderPaymentReqVO reqVO);

    Long processRefund(Long userId, TradeOrderRefundReqVO reqVO);

    TradeOrderPaymentRespVO getPaymentRecord(Long paymentId);

    List<TradeOrderPaymentRespVO> getPaymentRecordsByOrderId(Long orderId);

    void syncPaymentStatus(Long orderId);

}