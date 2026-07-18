package com.meession.etm.module.trade.service.finance;

import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderPaymentReqVO;
import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderPaymentRespVO;
import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderRefundReqVO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.ErrorCodeConstants;
import com.meession.etm.module.trade.enums.order.TradeOrderStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;

@Slf4j
@Service
@Validated
public class TradeOrderFinanceServiceImpl implements TradeOrderFinanceService {

    private final ConcurrentHashMap<Long, TradeOrderPaymentRespVO> paymentRecords = new ConcurrentHashMap<>();

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long processPayment(Long userId, TradeOrderPaymentReqVO reqVO) {
        TradeOrderDO order = tradeOrderMapper.selectById(reqVO.getOrderId());
        if (order == null) {
            throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
        }

        if (order.getPayStatus()) {
            throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
        }

        if (reqVO.getPayAmount() < order.getPayPrice()) {
            throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
        }

        order.setPayStatus(true);
        order.setPayTime(LocalDateTime.now());
        order.setPayType(reqVO.getPayType());
        order.setPayChannel(reqVO.getPayChannel());
        order.setStatus(TradeOrderStatusEnum.PAID.getStatus());
        tradeOrderMapper.updateById(order);

        Long paymentId = System.currentTimeMillis();
        TradeOrderPaymentRespVO payment = new TradeOrderPaymentRespVO();
        payment.setId(paymentId);
        payment.setOrderId(reqVO.getOrderId());
        payment.setPayType(reqVO.getPayType());
        payment.setPayChannel(reqVO.getPayChannel());
        payment.setPayAmount(reqVO.getPayAmount());
        payment.setStatus(1);
        payment.setTransactionNo(reqVO.getTransactionNo());
        payment.setPayTime(LocalDateTime.now());
        payment.setRemark(reqVO.getRemark());
        paymentRecords.put(paymentId, payment);

        log.info("订单支付处理成功: orderId={}, paymentId={}, amount={}", reqVO.getOrderId(), paymentId, reqVO.getPayAmount());
        return paymentId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long processRefund(Long userId, TradeOrderRefundReqVO reqVO) {
        TradeOrderDO order = tradeOrderMapper.selectById(reqVO.getOrderId());
        if (order == null) {
            throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
        }

        if (!order.getPayStatus()) {
            throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
        }

        if (reqVO.getRefundAmount() > order.getPayPrice()) {
            throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
        }

        order.setRefundStatus(2);
        order.setRefundPrice(reqVO.getRefundAmount());
        order.setRefundTime(LocalDateTime.now());
        order.setStatus(TradeOrderStatusEnum.REFUND.getStatus());
        tradeOrderMapper.updateById(order);

        Long refundId = System.currentTimeMillis() + 1;
        log.info("订单退款处理成功: orderId={}, refundId={}, amount={}", reqVO.getOrderId(), refundId, reqVO.getRefundAmount());
        return refundId;
    }

    @Override
    public TradeOrderPaymentRespVO getPaymentRecord(Long paymentId) {
        return paymentRecords.get(paymentId);
    }

    @Override
    public List<TradeOrderPaymentRespVO> getPaymentRecordsByOrderId(Long orderId) {
        List<TradeOrderPaymentRespVO> result = new ArrayList<>();
        for (TradeOrderPaymentRespVO payment : paymentRecords.values()) {
            if (orderId.equals(payment.getOrderId())) {
                result.add(payment);
            }
        }
        return result;
    }

    @Override
    @Async
    public void syncPaymentStatus(Long orderId) {
        log.info("同步订单支付状态: orderId={}", orderId);
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order != null && order.getPayStatus()) {
            log.info("订单支付状态同步完成: orderId={}, status={}", orderId, order.getStatus());
        }
    }

}