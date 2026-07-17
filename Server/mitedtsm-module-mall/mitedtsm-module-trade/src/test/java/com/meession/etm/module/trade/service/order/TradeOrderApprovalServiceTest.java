package com.meession.etm.module.trade.service.order;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.order.TradeOrderApprovalStatusEnum;
import com.meession.etm.module.trade.enums.order.TradeOrderStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class TradeOrderApprovalServiceTest extends BaseDbUnitTest {

    @Resource
    private TradeOrderApprovalService tradeOrderApprovalService;

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @MockBean
    private BpmProcessInstanceApi processInstanceApi;

    @Test
    public void testSubmitForApproval() {
        TradeOrderDO order = TradeOrderDO.builder()
                .no("O202401010001")
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

        Mockito.when(processInstanceApi.createProcessInstance(eq(1L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-123");

        tradeOrderApprovalService.submitForApproval(1L, order.getId());

        TradeOrderDO updatedOrder = tradeOrderMapper.selectById(order.getId());
        assertNotNull(updatedOrder);
        assertEquals(TradeOrderApprovalStatusEnum.PENDING.getStatus(), updatedOrder.getApprovalStatus());
        assertEquals("process-123", updatedOrder.getProcessInstanceId());
    }

    @Test
    public void testCancelApproval() {
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
                .approvalStatus(TradeOrderApprovalStatusEnum.PENDING.getStatus())
                .processInstanceId("process-123")
                .build();
        tradeOrderMapper.insert(order);

        tradeOrderApprovalService.cancelApproval(order.getId());

        TradeOrderDO updatedOrder = tradeOrderMapper.selectById(order.getId());
        assertNotNull(updatedOrder);
        assertEquals(TradeOrderApprovalStatusEnum.CANCELLED.getStatus(), updatedOrder.getApprovalStatus());
        assertNull(updatedOrder.getProcessInstanceId());
    }

    @Test
    public void testUpdateApprovalStatus_Approved() {
        TradeOrderDO order = TradeOrderDO.builder()
                .no("O202401010003")
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
                .approvalStatus(TradeOrderApprovalStatusEnum.PENDING.getStatus())
                .build();
        tradeOrderMapper.insert(order);

        tradeOrderApprovalService.updateApprovalStatus(String.valueOf(order.getId()),
                TradeOrderApprovalStatusEnum.APPROVED.getStatus(), "同意");

        TradeOrderDO updatedOrder = tradeOrderMapper.selectById(order.getId());
        assertNotNull(updatedOrder);
        assertEquals(TradeOrderApprovalStatusEnum.APPROVED.getStatus(), updatedOrder.getApprovalStatus());
        assertEquals(TradeOrderStatusEnum.DELIVERY.getStatus(), updatedOrder.getStatus());
        assertEquals("同意", updatedOrder.getApprovalComment());
        assertNotNull(updatedOrder.getApprovalTime());
    }

    @Test
    public void testUpdateApprovalStatus_Rejected() {
        TradeOrderDO order = TradeOrderDO.builder()
                .no("O202401010004")
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
                .approvalStatus(TradeOrderApprovalStatusEnum.PENDING.getStatus())
                .build();
        tradeOrderMapper.insert(order);

        tradeOrderApprovalService.updateApprovalStatus(String.valueOf(order.getId()),
                TradeOrderApprovalStatusEnum.REJECTED.getStatus(), "不同意");

        TradeOrderDO updatedOrder = tradeOrderMapper.selectById(order.getId());
        assertNotNull(updatedOrder);
        assertEquals(TradeOrderApprovalStatusEnum.REJECTED.getStatus(), updatedOrder.getApprovalStatus());
        assertEquals(TradeOrderStatusEnum.CANCEL.getStatus(), updatedOrder.getStatus());
        assertEquals("不同意", updatedOrder.getApprovalComment());
    }

}