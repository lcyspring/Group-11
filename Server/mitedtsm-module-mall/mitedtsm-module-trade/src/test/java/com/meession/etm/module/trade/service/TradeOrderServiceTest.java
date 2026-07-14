package com.meession.etm.module.trade.service;

import cn.hutool.core.util.RandomUtil;
import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.framework.test.core.util.AssertUtils;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderItemSaveReqVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderPageReqVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderSaveReqVO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderItemDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderItemMapper;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertPojoEquals;
import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.framework.test.core.util.RandomUtils.randomPojo;
import static com.meession.etm.module.trade.enums.ErrorCodeConstants.ORDER_NOT_EXISTS;
import static com.meession.etm.module.trade.enums.ErrorCodeConstants.ORDER_ITEM_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;

@Import(TradeOrderServiceImpl.class)
public class TradeOrderServiceTest extends BaseDbUnitTest {

    @Resource
    private TradeOrderService tradeOrderService;

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Resource
    private TradeOrderItemMapper tradeOrderItemMapper;

    public Long generateId() {
        return RandomUtil.randomLong(100000, 999999);
    }

    @Test
    public void testCreateOrder_success() {
        TradeOrderSaveReqVO createReqVO = new TradeOrderSaveReqVO();
        createReqVO.setNo("TEST" + RandomUtil.randomString(10));
        createReqVO.setType(0);
        createReqVO.setTerminal(0);
        createReqVO.setUserId(1L);
        createReqVO.setUserIp("127.0.0.1");
        createReqVO.setStatus(0);
        createReqVO.setProductCount(2);
        createReqVO.setPayStatus(false);
        createReqVO.setDiscountPrice(0);
        createReqVO.setDeliveryPrice(100);
        createReqVO.setAdjustPrice(0);
        createReqVO.setPayPrice(1000);
        createReqVO.setDeliveryType(0);
        createReqVO.setReceiverName("张三");
        createReqVO.setReceiverMobile("13800138000");
        createReqVO.setReceiverAreaId(110000);
        createReqVO.setReceiverDetailAddress("测试地址");
        createReqVO.setCouponId(0L);
        createReqVO.setCouponPrice(0);
        createReqVO.setPointPrice(0);

        TradeOrderItemSaveReqVO item1 = new TradeOrderItemSaveReqVO();
        item1.setSpuId(1L);
        item1.setSpuName("测试商品1");
        item1.setSkuId(1L);
        item1.setCount(1);
        item1.setPrice(500);
        item1.setDiscountPrice(0);
        item1.setPayPrice(500);
        item1.setAfterSaleStatus(0);

        TradeOrderItemSaveReqVO item2 = new TradeOrderItemSaveReqVO();
        item2.setSpuId(2L);
        item2.setSpuName("测试商品2");
        item2.setSkuId(2L);
        item2.setCount(1);
        item2.setPrice(500);
        item2.setDiscountPrice(0);
        item2.setPayPrice(500);
        item2.setAfterSaleStatus(0);

        createReqVO.setItems(Arrays.asList(item1, item2));

        Long orderId = tradeOrderService.createOrder(createReqVO);

        assertNotNull(orderId);
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        assertNotNull(order);
        assertEquals(createReqVO.getNo(), order.getNo());
        assertEquals(createReqVO.getStatus(), order.getStatus());

        List<TradeOrderItemDO> items = tradeOrderItemMapper.selectListByOrderId(orderId);
        assertEquals(2, items.size());
    }

    @Test
    public void testGetOrder_success() {
        TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
            o.setNo("TEST" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(1L);
            o.setUserIp("127.0.0.1");
            o.setStatus(0);
            o.setProductCount(1);
            o.setPayStatus(false);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(100);
            o.setDeliveryType(0);
            o.setReceiverName("张三");
            o.setReceiverMobile("13800138000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("测试地址");
        });
        tradeOrderMapper.insert(order);

        TradeOrderDO result = tradeOrderService.getOrder(order.getId());

        assertNotNull(result);
        assertEquals(order.getNo(), result.getNo());
    }

    @Test
    public void testGetOrder_notExists() {
        TradeOrderDO result = tradeOrderService.getOrder(99999L);

        assertNull(result);
    }

    @Test
    public void testGetOrderWithItems_success() {
        TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
            o.setNo("TEST" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(1L);
            o.setUserIp("127.0.0.1");
            o.setStatus(0);
            o.setProductCount(2);
            o.setPayStatus(false);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(200);
            o.setDeliveryType(0);
            o.setReceiverName("张三");
            o.setReceiverMobile("13800138000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("测试地址");
        });
        tradeOrderMapper.insert(order);

        TradeOrderItemDO item1 = randomPojo(TradeOrderItemDO.class, o -> {
            o.setOrderId(order.getId());
            o.setUserId(order.getUserId());
            o.setSpuId(1L);
            o.setSpuName("测试商品1");
            o.setSkuId(1L);
            o.setCount(1);
            o.setPrice(100);
            o.setDiscountPrice(0);
            o.setPayPrice(100);
            o.setAfterSaleStatus(0);
        });
        tradeOrderItemMapper.insert(item1);

        TradeOrderItemDO item2 = randomPojo(TradeOrderItemDO.class, o -> {
            o.setOrderId(order.getId());
            o.setUserId(order.getUserId());
            o.setSpuId(2L);
            o.setSpuName("测试商品2");
            o.setSkuId(2L);
            o.setCount(1);
            o.setPrice(100);
            o.setDiscountPrice(0);
            o.setPayPrice(100);
            o.setAfterSaleStatus(0);
        });
        tradeOrderItemMapper.insert(item2);

        TradeOrderDO result = tradeOrderService.getOrderWithItems(order.getId());

        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(2, result.getItems().size());
    }

    @Test
    public void testGetOrderPage_success() {
        for (int i = 0; i < 5; i++) {
            final int status = i % 2;
            TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
                o.setNo("TEST" + RandomUtil.randomString(10));
                o.setType(0);
                o.setTerminal(0);
                o.setUserId(1L);
                o.setUserIp("127.0.0.1");
                o.setStatus(status);
                o.setProductCount(1);
                o.setPayStatus(false);
                o.setDiscountPrice(0);
                o.setDeliveryPrice(0);
                o.setAdjustPrice(0);
                o.setPayPrice(100);
                o.setDeliveryType(0);
                o.setReceiverName("张三");
                o.setReceiverMobile("13800138000");
                o.setReceiverAreaId(110000);
                o.setReceiverDetailAddress("测试地址");
            });
            tradeOrderMapper.insert(order);
        }

        TradeOrderPageReqVO pageReqVO = new TradeOrderPageReqVO();
        pageReqVO.setStatus(0);

        var pageResult = tradeOrderService.getOrderPage(pageReqVO);

        assertNotNull(pageResult);
        assertTrue(pageResult.getTotal() >= 0);
        assertTrue(pageResult.getList().size() <= pageResult.getTotal());
    }

    @Test
    public void testUpdateOrder_success() {
        TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
            o.setNo("TEST" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(1L);
            o.setUserIp("127.0.0.1");
            o.setStatus(0);
            o.setProductCount(1);
            o.setPayStatus(false);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(100);
            o.setDeliveryType(0);
            o.setReceiverName("张三");
            o.setReceiverMobile("13800138000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("测试地址");
        });
        tradeOrderMapper.insert(order);

        TradeOrderSaveReqVO updateReqVO = randomPojo(TradeOrderSaveReqVO.class, o -> {
            o.setId(order.getId());
            o.setNo("UPDATED" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(order.getUserId());
            o.setUserIp("127.0.0.1");
            o.setStatus(1);
            o.setProductCount(1);
            o.setPayStatus(true);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(100);
            o.setDeliveryType(0);
            o.setReceiverName("李四");
            o.setReceiverMobile("13900139000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("更新地址");
            o.setCouponId(0L);
            o.setCouponPrice(0);
            o.setPointPrice(0);
            o.setItems(null);
        });

        tradeOrderService.updateOrder(updateReqVO);

        TradeOrderDO result = tradeOrderMapper.selectById(order.getId());
        assertEquals(updateReqVO.getNo(), result.getNo());
        assertEquals(updateReqVO.getStatus(), result.getStatus());
        assertEquals(updateReqVO.getReceiverName(), result.getReceiverName());
    }

    @Test
    public void testUpdateOrder_notExists() {
        TradeOrderSaveReqVO updateReqVO = randomPojo(TradeOrderSaveReqVO.class, o -> {
            o.setId(99999L);
            o.setNo("UPDATED" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(1L);
            o.setUserIp("127.0.0.1");
            o.setStatus(1);
            o.setProductCount(1);
            o.setPayStatus(true);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(100);
            o.setDeliveryType(0);
            o.setReceiverName("李四");
            o.setReceiverMobile("13900139000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("更新地址");
        });

        assertServiceException(() -> tradeOrderService.updateOrder(updateReqVO), ORDER_NOT_EXISTS);
    }

    @Test
    public void testDeleteOrder_success() {
        TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
            o.setNo("TEST" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(1L);
            o.setUserIp("127.0.0.1");
            o.setStatus(0);
            o.setProductCount(1);
            o.setPayStatus(false);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(100);
            o.setDeliveryType(0);
            o.setReceiverName("张三");
            o.setReceiverMobile("13800138000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("测试地址");
        });
        tradeOrderMapper.insert(order);

        TradeOrderItemDO item = randomPojo(TradeOrderItemDO.class, o -> {
            o.setOrderId(order.getId());
            o.setUserId(order.getUserId());
            o.setSpuId(1L);
            o.setSpuName("测试商品");
            o.setSkuId(1L);
            o.setCount(1);
            o.setPrice(100);
            o.setDiscountPrice(0);
            o.setPayPrice(100);
            o.setAfterSaleStatus(0);
        });
        tradeOrderItemMapper.insert(item);

        tradeOrderService.deleteOrder(order.getId());

        assertNull(tradeOrderMapper.selectById(order.getId()));
        List<TradeOrderItemDO> items = tradeOrderItemMapper.selectListByOrderId(order.getId());
        assertEquals(0, items.size());
    }

    @Test
    public void testDeleteOrder_notExists() {
        assertServiceException(() -> tradeOrderService.deleteOrder(99999L), ORDER_NOT_EXISTS);
    }

    @Test
    public void testDeleteOrderList_success() {
        TradeOrderDO order1 = randomPojo(TradeOrderDO.class, o -> {
            o.setNo("TEST" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(1L);
            o.setUserIp("127.0.0.1");
            o.setStatus(0);
            o.setProductCount(1);
            o.setPayStatus(false);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(100);
            o.setDeliveryType(0);
            o.setReceiverName("张三");
            o.setReceiverMobile("13800138000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("测试地址");
        });
        tradeOrderMapper.insert(order1);

        TradeOrderDO order2 = randomPojo(TradeOrderDO.class, o -> {
            o.setNo("TEST" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(1L);
            o.setUserIp("127.0.0.1");
            o.setStatus(0);
            o.setProductCount(1);
            o.setPayStatus(false);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(100);
            o.setDeliveryType(0);
            o.setReceiverName("张三");
            o.setReceiverMobile("13800138000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("测试地址");
        });
        tradeOrderMapper.insert(order2);

        tradeOrderService.deleteOrderList(Arrays.asList(order1.getId(), order2.getId()));

        assertNull(tradeOrderMapper.selectById(order1.getId()));
        assertNull(tradeOrderMapper.selectById(order2.getId()));
    }

    @Test
    public void testGetOrderItems_success() {
        TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
            o.setNo("TEST" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(1L);
            o.setUserIp("127.0.0.1");
            o.setStatus(0);
            o.setProductCount(2);
            o.setPayStatus(false);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(200);
            o.setDeliveryType(0);
            o.setReceiverName("张三");
            o.setReceiverMobile("13800138000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("测试地址");
        });
        tradeOrderMapper.insert(order);

        TradeOrderItemDO item1 = randomPojo(TradeOrderItemDO.class, o -> {
            o.setOrderId(order.getId());
            o.setUserId(order.getUserId());
            o.setSpuId(1L);
            o.setSpuName("测试商品1");
            o.setSkuId(1L);
            o.setCount(1);
            o.setPrice(100);
            o.setDiscountPrice(0);
            o.setPayPrice(100);
            o.setAfterSaleStatus(0);
        });
        tradeOrderItemMapper.insert(item1);

        TradeOrderItemDO item2 = randomPojo(TradeOrderItemDO.class, o -> {
            o.setOrderId(order.getId());
            o.setUserId(order.getUserId());
            o.setSpuId(2L);
            o.setSpuName("测试商品2");
            o.setSkuId(2L);
            o.setCount(1);
            o.setPrice(100);
            o.setDiscountPrice(0);
            o.setPayPrice(100);
            o.setAfterSaleStatus(0);
        });
        tradeOrderItemMapper.insert(item2);

        List<TradeOrderItemDO> items = tradeOrderService.getOrderItems(order.getId());

        assertEquals(2, items.size());
    }

    @Test
    public void testGetOrderItem_success() {
        TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
            o.setNo("TEST" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(1L);
            o.setUserIp("127.0.0.1");
            o.setStatus(0);
            o.setProductCount(1);
            o.setPayStatus(false);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(100);
            o.setDeliveryType(0);
            o.setReceiverName("张三");
            o.setReceiverMobile("13800138000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("测试地址");
        });
        tradeOrderMapper.insert(order);

        TradeOrderItemDO item = randomPojo(TradeOrderItemDO.class, o -> {
            o.setOrderId(order.getId());
            o.setUserId(order.getUserId());
            o.setSpuId(1L);
            o.setSpuName("测试商品");
            o.setSkuId(1L);
            o.setCount(1);
            o.setPrice(100);
            o.setDiscountPrice(0);
            o.setPayPrice(100);
            o.setAfterSaleStatus(0);
        });
        tradeOrderItemMapper.insert(item);

        TradeOrderItemDO result = tradeOrderService.getOrderItem(item.getId());

        assertNotNull(result);
        assertEquals(item.getSpuName(), result.getSpuName());
    }

    @Test
    public void testGetOrderItem_notExists() {
        assertServiceException(() -> tradeOrderService.getOrderItem(99999L), ORDER_ITEM_NOT_EXISTS);
    }

    @Test
    public void testUpdateOrderStatus_success() {
        TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
            o.setNo("TEST" + RandomUtil.randomString(10));
            o.setType(0);
            o.setTerminal(0);
            o.setUserId(1L);
            o.setUserIp("127.0.0.1");
            o.setStatus(0);
            o.setProductCount(1);
            o.setPayStatus(false);
            o.setDiscountPrice(0);
            o.setDeliveryPrice(0);
            o.setAdjustPrice(0);
            o.setPayPrice(100);
            o.setDeliveryType(0);
            o.setReceiverName("张三");
            o.setReceiverMobile("13800138000");
            o.setReceiverAreaId(110000);
            o.setReceiverDetailAddress("测试地址");
        });
        tradeOrderMapper.insert(order);

        tradeOrderService.updateOrderStatus(order.getId(), 1);

        TradeOrderDO result = tradeOrderMapper.selectById(order.getId());
        assertEquals(1, result.getStatus());
    }

    @Test
    public void testUpdateOrderStatus_notExists() {
        assertServiceException(() -> tradeOrderService.updateOrderStatus(99999L, 1), ORDER_NOT_EXISTS);
    }

    @Test
    public void testGetOrdersByUserId_success() {
        for (int i = 0; i < 3; i++) {
            TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
                o.setNo("TEST" + RandomUtil.randomString(10));
                o.setType(0);
                o.setTerminal(0);
                o.setUserId(1L);
                o.setUserIp("127.0.0.1");
                o.setStatus(0);
                o.setProductCount(1);
                o.setPayStatus(false);
                o.setDiscountPrice(0);
                o.setDeliveryPrice(0);
                o.setAdjustPrice(0);
                o.setPayPrice(100);
                o.setDeliveryType(0);
                o.setReceiverName("张三");
                o.setReceiverMobile("13800138000");
                o.setReceiverAreaId(110000);
                o.setReceiverDetailAddress("测试地址");
            });
            tradeOrderMapper.insert(order);
        }

        for (int i = 0; i < 2; i++) {
            TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
                o.setNo("TEST" + RandomUtil.randomString(10));
                o.setType(0);
                o.setTerminal(0);
                o.setUserId(2L);
                o.setUserIp("127.0.0.1");
                o.setStatus(0);
                o.setProductCount(1);
                o.setPayStatus(false);
                o.setDiscountPrice(0);
                o.setDeliveryPrice(0);
                o.setAdjustPrice(0);
                o.setPayPrice(100);
                o.setDeliveryType(0);
                o.setReceiverName("张三");
                o.setReceiverMobile("13800138000");
                o.setReceiverAreaId(110000);
                o.setReceiverDetailAddress("测试地址");
            });
            tradeOrderMapper.insert(order);
        }

        List<TradeOrderDO> orders = tradeOrderService.getOrdersByUserId(1L);

        assertEquals(3, orders.size());
    }

}