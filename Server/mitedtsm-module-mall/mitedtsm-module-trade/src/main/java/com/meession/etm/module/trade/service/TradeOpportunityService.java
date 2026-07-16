package com.meession.etm.module.trade.service;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunityPageReqVO;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunitySaveReqVO;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunityToOrderReqVO;
import com.meession.etm.module.trade.dal.dataobject.opportunity.TradeOpportunityDO;
import com.meession.etm.module.trade.dal.dataobject.opportunity.TradeOpportunityItemDO;

import java.util.List;

public interface TradeOpportunityService {

    Long createOpportunity(TradeOpportunitySaveReqVO createReqVO);

    void updateOpportunity(TradeOpportunitySaveReqVO updateReqVO);

    void deleteOpportunity(Long id);

    PageResult<TradeOpportunityDO> getOpportunityPage(TradeOpportunityPageReqVO reqVO);

    TradeOpportunityDO getOpportunity(Long id);

    TradeOpportunityDO getOpportunityWithItems(Long id);

    List<TradeOpportunityDO> getOpportunitiesByUserId(Long userId);

    List<TradeOpportunityDO> getOpportunitiesByCustomerId(Long customerId);

    List<TradeOpportunityItemDO> getOpportunityItems(Long opportunityId);

    TradeOpportunityItemDO getOpportunityItem(Long id);

    Long convertToOrder(TradeOpportunityToOrderReqVO reqVO);

}