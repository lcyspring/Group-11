package com.meession.etm.module.trade.service;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.test.core.ut.BaseDbUnitTest;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunityPageReqVO;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunitySaveReqVO;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunityToOrderReqVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderSaveReqVO;
import com.meession.etm.module.trade.dal.dataobject.opportunity.TradeOpportunityDO;
import com.meession.etm.module.trade.dal.mysql.opportunity.TradeOpportunityMapper;
import com.meession.etm.module.trade.enums.ErrorCodeConstants;
import com.meession.etm.module.trade.enums.TradeOpportunityStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TradeOpportunityServiceTest extends BaseDbUnitTest {

    @Resource
    private TradeOpportunityService tradeOpportunityService;

    @Resource
    private TradeOpportunityMapper tradeOpportunityMapper;

    @Resource
    private TradeOrderService tradeOrderService;

    @Test
    void testCreateOpportunity_success() {
        TradeOpportunitySaveReqVO reqVO = new TradeOpportunitySaveReqVO();
        reqVO.setName("测试商机");
        reqVO.setCustomerName("测试客户");
        reqVO.setAmount(new BigDecimal("10000.00"));

        Long id = tradeOpportunityService.createOpportunity(reqVO);

        assertNotNull(id);
        TradeOpportunityDO opportunity = tradeOpportunityMapper.selectById(id);
        assertEquals("测试商机", opportunity.getName());
        assertEquals(TradeOpportunityStatusEnum.PENDING.getStatus(), opportunity.getStatus());
    }

    @Test
    void testUpdateOpportunity_success() {
        TradeOpportunitySaveReqVO createReqVO = new TradeOpportunitySaveReqVO();
        createReqVO.setName("测试商机");
        createReqVO.setCustomerName("测试客户");
        createReqVO.setAmount(new BigDecimal("10000.00"));
        Long id = tradeOpportunityService.createOpportunity(createReqVO);

        TradeOpportunitySaveReqVO updateReqVO = new TradeOpportunitySaveReqVO();
        updateReqVO.setId(id);
        updateReqVO.setName("修改后的商机");
        updateReqVO.setCustomerName("测试客户");
        updateReqVO.setAmount(new BigDecimal("20000.00"));

        tradeOpportunityService.updateOpportunity(updateReqVO);

        TradeOpportunityDO opportunity = tradeOpportunityMapper.selectById(id);
        assertEquals("修改后的商机", opportunity.getName());
    }

    @Test
    void testDeleteOpportunity_success() {
        TradeOpportunitySaveReqVO reqVO = new TradeOpportunitySaveReqVO();
        reqVO.setName("测试商机");
        reqVO.setCustomerName("测试客户");
        reqVO.setAmount(new BigDecimal("10000.00"));
        Long id = tradeOpportunityService.createOpportunity(reqVO);

        tradeOpportunityService.deleteOpportunity(id);

        assertNull(tradeOpportunityMapper.selectById(id));
    }

    @Test
    void testDeleteOpportunity_notExists() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> tradeOpportunityService.deleteOpportunity(99999L));
        assertEquals(ErrorCodeConstants.OPPORTUNITY_NOT_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void testGetOpportunityPage_success() {
        TradeOpportunityPageReqVO reqVO = new TradeOpportunityPageReqVO();

        PageResult<TradeOpportunityDO> pageResult = tradeOpportunityService.getOpportunityPage(reqVO);

        assertNotNull(pageResult);
    }

    @Test
    void testGetOpportunityWithItems_success() {
        TradeOpportunitySaveReqVO reqVO = new TradeOpportunitySaveReqVO();
        reqVO.setName("测试商机");
        reqVO.setCustomerName("测试客户");
        reqVO.setAmount(new BigDecimal("10000.00"));
        Long id = tradeOpportunityService.createOpportunity(reqVO);

        TradeOpportunityDO opportunity = tradeOpportunityService.getOpportunityWithItems(id);

        assertNotNull(opportunity);
    }

    @Test
    void testGetOpportunitiesByUserId_success() {
        List<TradeOpportunityDO> opportunities = tradeOpportunityService.getOpportunitiesByUserId(1L);
        assertNotNull(opportunities);
    }

    @Test
    void testConvertToOrder_success() {
        TradeOpportunitySaveReqVO reqVO = new TradeOpportunitySaveReqVO();
        reqVO.setName("测试商机");
        reqVO.setCustomerName("测试客户");
        reqVO.setAmount(new BigDecimal("10000.00"));
        reqVO.setSalesUserId(1L);
        Long opportunityId = tradeOpportunityService.createOpportunity(reqVO);

        TradeOpportunityToOrderReqVO toOrderReqVO = new TradeOpportunityToOrderReqVO();
        toOrderReqVO.setOpportunityId(opportunityId);
        toOrderReqVO.setReceiverName("张三");
        toOrderReqVO.setReceiverMobile("13800138000");
        toOrderReqVO.setReceiverAreaId(1);
        toOrderReqVO.setReceiverDetailAddress("测试地址");

        Long orderId = tradeOpportunityService.convertToOrder(toOrderReqVO);

        assertNotNull(orderId);
        TradeOpportunityDO opportunity = tradeOpportunityMapper.selectById(opportunityId);
        assertEquals(orderId, opportunity.getOrderId());
        assertEquals(TradeOpportunityStatusEnum.WON.getStatus(), opportunity.getStatus());
    }

}