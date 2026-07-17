package com.meession.etm.module.trade.service.report;

import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderDailyReportRespVO;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderReportReqVO;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderReportRespVO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.order.TradeOrderStatusEnum;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TradeOrderReportServiceTest extends BaseDbUnitTest {

    @Resource
    private TradeOrderReportService tradeOrderReportService;

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Test
    public void testGetOrderReport() {
        TradeOrderDO order1 = TradeOrderDO.builder()
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
        tradeOrderMapper.insert(order1);

        TradeOrderDO order2 = TradeOrderDO.builder()
                .no("O202401010002")
                .type(0)
                .terminal(0)
                .userId(2L)
                .userIp("127.0.0.1")
                .status(TradeOrderStatusEnum.RECEIVE.getStatus())
                .productCount(2)
                .payStatus(true)
                .payTime(LocalDateTime.now())
                .totalPrice(20000)
                .discountPrice(0)
                .deliveryPrice(0)
                .adjustPrice(0)
                .payPrice(20000)
                .deliveryType(0)
                .receiverName("test")
                .receiverMobile("13800138000")
                .receiverAreaId(1)
                .receiverDetailAddress("address")
                .couponId(0L)
                .couponPrice(0)
                .pointPrice(0)
                .build();
        tradeOrderMapper.insert(order2);

        TradeOrderReportReqVO reqVO = new TradeOrderReportReqVO();
        TradeOrderReportRespVO report = tradeOrderReportService.getOrderReport(reqVO);

        assertNotNull(report);
        assertEquals(2, report.getTotalCount());
        assertEquals(30000L, report.getTotalAmount());
    }

    @Test
    public void testGetDailyReport() {
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

        TradeOrderReportReqVO reqVO = new TradeOrderReportReqVO();
        List<TradeOrderDailyReportRespVO> dailyReports = tradeOrderReportService.getDailyReport(reqVO);

        assertNotNull(dailyReports);
        assertFalse(dailyReports.isEmpty());
    }

}