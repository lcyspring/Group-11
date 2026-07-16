package com.meession.etm.module.trade.service.order;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.ErrorCodeConstants;
import com.meession.etm.module.trade.enums.TradeOrderStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TradeOrderStatusServiceTest extends BaseDbUnitTest {

    @Resource
    private TradeOrderStatusService tradeOrderStatusService;

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Test
    void testCanTransition_createToPaid() {
        assertTrue(tradeOrderStatusService.canTransition(TradeOrderStatusEnum.CREATE, TradeOrderStatusEnum.PAID));
    }

    @Test
    void testCanTransition_createToCancel() {
        assertTrue(tradeOrderStatusService.canTransition(TradeOrderStatusEnum.CREATE, TradeOrderStatusEnum.CANCEL));
    }

    @Test
    void testCanTransition_createToReceive() {
        assertFalse(tradeOrderStatusService.canTransition(TradeOrderStatusEnum.CREATE, TradeOrderStatusEnum.RECEIVE));
    }

    @Test
    void testCanTransition_cancelToPaid() {
        assertFalse(tradeOrderStatusService.canTransition(TradeOrderStatusEnum.CANCEL, TradeOrderStatusEnum.PAID));
    }

    @Test
    void testUpdateStatus_success() {
        TradeOrderDO order = new TradeOrderDO();
        order.setNo("TEST001");
        order.setType(0);
        order.setTerminal(0);
        order.setUserId(1L);
        order.setUserIp("127.0.0.1");
        order.setStatus(TradeOrderStatusEnum.CREATE.getStatus());
        order.setProductCount(1);
        order.setPayStatus(false);
        order.setDiscountPrice(0);
        order.setDeliveryPrice(0);
        order.setAdjustPrice(0);
        order.setPayPrice(100);
        order.setDeliveryType(0);
        order.setReceiverName("test");
        order.setReceiverMobile("13800138000");
        order.setReceiverAreaId(1);
        order.setReceiverDetailAddress("test");
        order.setCouponId(0L);
        order.setCouponPrice(0);
        order.setPointPrice(0);
        tradeOrderMapper.insert(order);

        tradeOrderStatusService.updateStatus(order.getId(), TradeOrderStatusEnum.PAID);

        TradeOrderDO updated = tradeOrderMapper.selectById(order.getId());
        assertEquals(TradeOrderStatusEnum.PAID.getStatus(), updated.getStatus());
    }

    @Test
    void testUpdateStatus_notExists() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> tradeOrderStatusService.updateStatus(99999L, TradeOrderStatusEnum.PAID));
        assertEquals(ErrorCodeConstants.ORDER_NOT_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void testUpdateStatus_cannotTransition() {
        TradeOrderDO order = new TradeOrderDO();
        order.setNo("TEST002");
        order.setType(0);
        order.setTerminal(0);
        order.setUserId(1L);
        order.setUserIp("127.0.0.1");
        order.setStatus(TradeOrderStatusEnum.CANCEL.getStatus());
        order.setProductCount(1);
        order.setPayStatus(false);
        order.setDiscountPrice(0);
        order.setDeliveryPrice(0);
        order.setAdjustPrice(0);
        order.setPayPrice(100);
        order.setDeliveryType(0);
        order.setReceiverName("test");
        order.setReceiverMobile("13800138000");
        order.setReceiverAreaId(1);
        order.setReceiverDetailAddress("test");
        order.setCouponId(0L);
        order.setCouponPrice(0);
        order.setPointPrice(0);
        tradeOrderMapper.insert(order);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> tradeOrderStatusService.updateStatus(order.getId(), TradeOrderStatusEnum.PAID));
        assertEquals(ErrorCodeConstants.ORDER_STATUS_CANNOT_TRANSITION.getCode(), exception.getCode());
    }

}