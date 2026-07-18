package com.meession.etm.module.trade.service.finance;

import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderPaymentReqVO;
import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderPaymentRespVO;
import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderRefundReqVO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.TradeOrderStatusEnum;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TradeOrderFinanceServiceTest extends BaseDbUnitTest {

    @Resource
    private TradeOrderFinanceService tradeOrderFinanceService;

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Test
    public void testProcessPayment() {
        TradeOrderDO order = TradeOrderDO.builder()
                .no("O202401010001")
                .type(0)
                .terminal(0)
                .userId(1L)
                .userIp("127.0.0.1")
                .status(TradeOrderStatusEnum.CREATE.getStatus())
                .productCount(1)
                .payStatus(false)
                .totalPrice(10000)
                .discountPrice(0)
                .deliveryPrice(0)
                .adjustPrice(0)
                .payPrice(10000)
                .deliveryType(0)
                .receiverName("test")
                .receiverMobile("13800138000")
                .receiverAreaId(1)
                .receiverDetailAddress("address")
                .couponId(0L)
                .couponPrice(0)
                .pointPrice(0)
                .build();
        tradeOrderMapper.insert(order);

        TradeOrderPaymentReqVO reqVO = new TradeOrderPaymentReqVO();
        reqVO.setOrderId(order.getId());
        reqVO.setPayType(1);
        reqVO.setPayAmount(10000L);
        reqVO.setPayChannel("wechat");
        reqVO.setTransactionNo("T123456789");

        Long paymentId = tradeOrderFinanceService.processPayment(1L, reqVO);
        assertNotNull(paymentId);

        TradeOrderDO updatedOrder = tradeOrderMapper.selectById(order.getId());
        assertTrue(updatedOrder.getPayStatus());
        assertEquals(TradeOrderStatusEnum.PAID.getStatus(), updatedOrder.getStatus());
    }

    @Test
    public void testProcessRefund() {
        TradeOrderDO order = TradeOrderDO.builder()
                .no("O202401010002")
                .type(0)
                .terminal(0)
                .userId(1L)
                .userIp("127.0.0.1")
                .status(TradeOrderStatusEnum.PAID.getStatus())
                .productCount(1)
                .payStatus(true)
                .payTime(LocalDateTime.now())
                .totalPrice(10000)
                .discountPrice(0)
                .deliveryPrice(0)
                .adjustPrice(0)
                .payPrice(10000)
                .deliveryType(0)
                .receiverName("test")
                .receiverMobile("13800138000")
                .receiverAreaId(1)
                .receiverDetailAddress("address")
                .couponId(0L)
                .couponPrice(0)
                .pointPrice(0)
                .build();
        tradeOrderMapper.insert(order);

        TradeOrderRefundReqVO reqVO = new TradeOrderRefundReqVO();
        reqVO.setOrderId(order.getId());
        reqVO.setRefundAmount(10000L);
        reqVO.setRefundReason("测试退款");

        Long refundId = tradeOrderFinanceService.processRefund(1L, reqVO);
        assertNotNull(refundId);

        TradeOrderDO updatedOrder = tradeOrderMapper.selectById(order.getId());
        assertEquals(TradeOrderStatusEnum.REFUND.getStatus(), updatedOrder.getStatus());
    }

    @Test
    public void testGetPaymentRecord() {
        TradeOrderDO order = TradeOrderDO.builder()
                .no("O202401010003")
                .type(0)
                .terminal(0)
                .userId(1L)
                .userIp("127.0.0.1")
                .status(TradeOrderStatusEnum.CREATE.getStatus())
                .productCount(1)
                .payStatus(false)
                .totalPrice(10000)
                .discountPrice(0)
                .deliveryPrice(0)
                .adjustPrice(0)
                .payPrice(10000)
                .deliveryType(0)
                .receiverName("test")
                .receiverMobile("13800138000")
                .receiverAreaId(1)
                .receiverDetailAddress("address")
                .couponId(0L)
                .couponPrice(0)
                .pointPrice(0)
                .build();
        tradeOrderMapper.insert(order);

        TradeOrderPaymentReqVO reqVO = new TradeOrderPaymentReqVO();
        reqVO.setOrderId(order.getId());
        reqVO.setPayType(1);
        reqVO.setPayAmount(10000L);

        Long paymentId = tradeOrderFinanceService.processPayment(1L, reqVO);

        TradeOrderPaymentRespVO payment = tradeOrderFinanceService.getPaymentRecord(paymentId);
        assertNotNull(payment);
        assertEquals(order.getId(), payment.getOrderId());
    }

}