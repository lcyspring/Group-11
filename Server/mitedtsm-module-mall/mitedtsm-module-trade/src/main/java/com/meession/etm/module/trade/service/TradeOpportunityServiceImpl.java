package com.meession.etm.module.trade.service;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunityItemSaveReqVO;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunityPageReqVO;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunitySaveReqVO;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunityToOrderReqVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderItemSaveReqVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderSaveReqVO;
import com.meession.etm.module.trade.dal.dataobject.opportunity.TradeOpportunityDO;
import com.meession.etm.module.trade.dal.dataobject.opportunity.TradeOpportunityItemDO;
import com.meession.etm.module.trade.dal.mysql.opportunity.TradeOpportunityItemMapper;
import com.meession.etm.module.trade.dal.mysql.opportunity.TradeOpportunityMapper;
import com.meession.etm.module.trade.enums.TradeOpportunityStatusEnum;
import com.meession.etm.module.trade.service.order.TradeOrderNoGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.trade.enums.ErrorCodeConstants.OPPORTUNITY_NOT_EXISTS;

@Service
public class TradeOpportunityServiceImpl implements TradeOpportunityService {

    @Resource
    private TradeOpportunityMapper tradeOpportunityMapper;

    @Resource
    private TradeOpportunityItemMapper tradeOpportunityItemMapper;

    @Resource
    private TradeOrderNoGenerator tradeOrderNoGenerator;

    @Resource
    private TradeOrderService tradeOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOpportunity(TradeOpportunitySaveReqVO createReqVO) {
        TradeOpportunityDO opportunity = BeanUtils.toBean(createReqVO, TradeOpportunityDO.class);
        opportunity.setNo(tradeOrderNoGenerator.generateOrderNo());
        if (opportunity.getStatus() == null) {
            opportunity.setStatus(TradeOpportunityStatusEnum.PENDING.getStatus());
        }
        tradeOpportunityMapper.insert(opportunity);

        if (createReqVO.getItems() != null && !createReqVO.getItems().isEmpty()) {
            for (TradeOpportunityItemSaveReqVO itemReqVO : createReqVO.getItems()) {
                TradeOpportunityItemDO item = BeanUtils.toBean(itemReqVO, TradeOpportunityItemDO.class);
                item.setOpportunityId(opportunity.getId());
                tradeOpportunityItemMapper.insert(item);
            }
        }

        return opportunity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOpportunity(TradeOpportunitySaveReqVO updateReqVO) {
        validateOpportunityExists(updateReqVO.getId());

        TradeOpportunityDO updateObj = BeanUtils.toBean(updateReqVO, TradeOpportunityDO.class);
        tradeOpportunityMapper.updateById(updateObj);

        if (updateReqVO.getItems() != null) {
            tradeOpportunityItemMapper.deleteByOpportunityId(updateReqVO.getId());
            for (TradeOpportunityItemSaveReqVO itemReqVO : updateReqVO.getItems()) {
                TradeOpportunityItemDO item = BeanUtils.toBean(itemReqVO, TradeOpportunityItemDO.class);
                item.setOpportunityId(updateReqVO.getId());
                tradeOpportunityItemMapper.insert(item);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOpportunity(Long id) {
        validateOpportunityExists(id);
        tradeOpportunityItemMapper.deleteByOpportunityId(id);
        tradeOpportunityMapper.deleteById(id);
    }

    @Override
    public PageResult<TradeOpportunityDO> getOpportunityPage(TradeOpportunityPageReqVO reqVO) {
        return tradeOpportunityMapper.selectPage(reqVO);
    }

    @Override
    public TradeOpportunityDO getOpportunity(Long id) {
        return tradeOpportunityMapper.selectById(id);
    }

    @Override
    public TradeOpportunityDO getOpportunityWithItems(Long id) {
        TradeOpportunityDO opportunity = tradeOpportunityMapper.selectById(id);
        if (opportunity != null) {
            List<TradeOpportunityItemDO> items = tradeOpportunityItemMapper.selectListByOpportunityId(id);
            opportunity.setItem(items.isEmpty() ? null : items.get(0));
        }
        return opportunity;
    }

    @Override
    public List<TradeOpportunityDO> getOpportunitiesByUserId(Long userId) {
        return tradeOpportunityMapper.selectListByUserId(userId);
    }

    @Override
    public List<TradeOpportunityDO> getOpportunitiesByCustomerId(Long customerId) {
        return tradeOpportunityMapper.selectListByCustomerId(customerId);
    }

    @Override
    public List<TradeOpportunityItemDO> getOpportunityItems(Long opportunityId) {
        return tradeOpportunityItemMapper.selectListByOpportunityId(opportunityId);
    }

    @Override
    public TradeOpportunityItemDO getOpportunityItem(Long id) {
        TradeOpportunityItemDO item = tradeOpportunityItemMapper.selectById(id);
        if (item == null) {
            throw exception(OPPORTUNITY_NOT_EXISTS);
        }
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToOrder(TradeOpportunityToOrderReqVO reqVO) {
        TradeOpportunityDO opportunity = tradeOpportunityMapper.selectById(reqVO.getOpportunityId());
        if (opportunity == null) {
            throw exception(OPPORTUNITY_NOT_EXISTS);
        }

        TradeOrderSaveReqVO orderReqVO = new TradeOrderSaveReqVO();
        orderReqVO.setUserId(reqVO.getUserId() != null ? reqVO.getUserId() : opportunity.getSalesUserId());
        orderReqVO.setReceiverName(reqVO.getReceiverName());
        orderReqVO.setReceiverMobile(reqVO.getReceiverMobile());
        orderReqVO.setReceiverAreaId(reqVO.getReceiverAreaId());
        orderReqVO.setReceiverDetailAddress(reqVO.getReceiverDetailAddress());
        orderReqVO.setTotalPrice(opportunity.getAmount().multiply(new java.math.BigDecimal(100)).intValue());
        orderReqVO.setPayPrice(orderReqVO.getTotalPrice());

        List<TradeOpportunityItemDO> items = tradeOpportunityItemMapper.selectListByOpportunityId(opportunity.getId());
        if (!items.isEmpty()) {
            List<TradeOrderItemSaveReqVO> orderItems = new ArrayList<>();
            for (TradeOpportunityItemDO item : items) {
                TradeOrderItemSaveReqVO orderItem = new TradeOrderItemSaveReqVO();
                orderItem.setSpuId(item.getSpuId());
                orderItem.setSkuId(item.getSkuId());
                orderItem.setCount(item.getCount());
                orderItem.setPrice(item.getPrice().multiply(new java.math.BigDecimal(100)).intValue());
                orderItem.setPayPrice(orderItem.getPrice());
                orderItems.add(orderItem);
            }
            orderReqVO.setItems(orderItems);
        }

        Long orderId = tradeOrderService.createOrder(orderReqVO);

        TradeOpportunityDO updateObj = new TradeOpportunityDO();
        updateObj.setId(opportunity.getId());
        updateObj.setOrderId(orderId);
        updateObj.setStatus(TradeOpportunityStatusEnum.WON.getStatus());
        tradeOpportunityMapper.updateById(updateObj);

        return orderId;
    }

    private void validateOpportunityExists(Long id) {
        if (id == null) {
            return;
        }
        TradeOpportunityDO opportunity = tradeOpportunityMapper.selectById(id);
        if (opportunity == null) {
            throw exception(OPPORTUNITY_NOT_EXISTS);
        }
    }

}