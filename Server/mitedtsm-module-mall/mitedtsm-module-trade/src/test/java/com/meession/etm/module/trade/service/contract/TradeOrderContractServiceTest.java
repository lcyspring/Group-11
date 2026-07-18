package com.meession.etm.module.trade.service.contract;

import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.trade.dal.dataobject.contract.TradeContractDO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.mysql.contract.TradeContractMapper;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.meession.etm.module.trade.enums.TradeOrderStatusEnum;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TradeOrderContractServiceTest extends BaseDbUnitTest {

    @Resource
    private TradeOrderContractService tradeOrderContractService;

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Resource
    private TradeContractMapper tradeContractMapper;

    @Test
    public void testCreateContractForOrder() {
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

        Long contractId = tradeOrderContractService.createContractForOrder(order.getId(), "测试合同", null);
        assertNotNull(contractId);

        TradeContractDO contract = tradeContractMapper.selectById(contractId);
        assertNotNull(contract);
        assertEquals(order.getId(), contract.getOrderId());
    }

    @Test
    public void testGetContractByOrderId() {
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

        Long contractId = tradeOrderContractService.createContractForOrder(order.getId(), "测试合同2", null);

        TradeContractDO contract = tradeOrderContractService.getContractByOrderId(order.getId());
        assertNotNull(contract);
        assertEquals(contractId, contract.getId());
    }

    @Test
    public void testGetContractsByOrderIds() {
        TradeOrderDO order1 = TradeOrderDO.builder()
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
                .build();
        tradeOrderMapper.insert(order1);

        Long contractId1 = tradeOrderContractService.createContractForOrder(order1.getId(), "测试合同3", null);

        List<TradeContractDO> contracts = tradeOrderContractService.getContractsByOrderIds(Arrays.asList(order1.getId()));
        assertNotNull(contracts);
        assertFalse(contracts.isEmpty());
    }

}