package com.meession.etm.module.trade.service;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderItemSaveReqVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderPageReqVO;
import com.meession.etm.module.trade.controller.admin.order.vo.TradeOrderSaveReqVO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderItemDO;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderItemMapper;
import com.meession.etm.module.trade.dal.mysql.order.TradeOrderMapper;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.trade.enums.ErrorCodeConstants.ORDER_NOT_EXISTS;
import static com.meession.etm.module.trade.enums.ErrorCodeConstants.ORDER_ITEM_NOT_EXISTS;

@Service
public class TradeOrderServiceImpl implements TradeOrderService {

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Resource
    private TradeOrderItemMapper tradeOrderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(TradeOrderSaveReqVO createReqVO) {
        TradeOrderDO order = BeanUtils.toBean(createReqVO, TradeOrderDO.class);
        tradeOrderMapper.insert(order);

        if (createReqVO.getItems() != null && !createReqVO.getItems().isEmpty()) {
            for (TradeOrderItemSaveReqVO itemReqVO : createReqVO.getItems()) {
                TradeOrderItemDO item = BeanUtils.toBean(itemReqVO, TradeOrderItemDO.class);
                item.setOrderId(order.getId());
                item.setUserId(order.getUserId());
                tradeOrderItemMapper.insert(item);
            }
        }

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(TradeOrderSaveReqVO updateReqVO) {
        validateOrderExists(updateReqVO.getId());

        TradeOrderDO updateObj = BeanUtils.toBean(updateReqVO, TradeOrderDO.class);
        tradeOrderMapper.updateById(updateObj);

        if (updateReqVO.getItems() != null) {
            tradeOrderItemMapper.deleteByOrderId(updateReqVO.getId());
            for (TradeOrderItemSaveReqVO itemReqVO : updateReqVO.getItems()) {
                TradeOrderItemDO item = BeanUtils.toBean(itemReqVO, TradeOrderItemDO.class);
                item.setOrderId(updateReqVO.getId());
                item.setUserId(updateObj.getUserId());
                tradeOrderItemMapper.insert(item);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        validateOrderExists(id);
        tradeOrderItemMapper.deleteByOrderId(id);
        tradeOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrderList(List<Long> ids) {
        for (Long id : ids) {
            tradeOrderItemMapper.deleteByOrderId(id);
        }
        tradeOrderMapper.deleteByIds(ids);
    }

    @Override
    public PageResult<TradeOrderDO> getOrderPage(TradeOrderPageReqVO reqVO) {
        return tradeOrderMapper.selectPage(reqVO);
    }

    @Override
    public TradeOrderDO getOrder(Long id) {
        return tradeOrderMapper.selectById(id);
    }

    @Override
    public TradeOrderDO getOrderWithItems(Long id) {
        TradeOrderDO order = tradeOrderMapper.selectById(id);
        if (order != null) {
            List<TradeOrderItemDO> items = tradeOrderItemMapper.selectListByOrderId(id);
            order.setItems(items);
        }
        return order;
    }

    @Override
    public List<TradeOrderDO> getOrdersByUserId(Long userId) {
        return tradeOrderMapper.selectListByUserId(userId);
    }

    @Override
    public List<TradeOrderItemDO> getOrderItems(Long orderId) {
        return tradeOrderItemMapper.selectListByOrderId(orderId);
    }

    @Override
    public TradeOrderItemDO getOrderItem(Long id) {
        TradeOrderItemDO item = tradeOrderItemMapper.selectById(id);
        if (item == null) {
            throw exception(ORDER_ITEM_NOT_EXISTS);
        }
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long id, Integer status) {
        validateOrderExists(id);
        TradeOrderDO order = new TradeOrderDO();
        order.setId(id);
        order.setStatus(status);
        tradeOrderMapper.updateById(order);
    }

    @VisibleForTesting
    public void validateOrderExists(Long id) {
        if (id == null) {
            return;
        }
        TradeOrderDO order = tradeOrderMapper.selectById(id);
        if (order == null) {
            throw exception(ORDER_NOT_EXISTS);
        }
    }

}