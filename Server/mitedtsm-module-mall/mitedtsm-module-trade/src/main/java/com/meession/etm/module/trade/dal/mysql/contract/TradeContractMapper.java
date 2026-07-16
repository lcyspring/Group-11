package com.meession.etm.module.trade.dal.mysql.contract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.trade.controller.admin.contract.vo.TradeContractPageReqVO;
import com.meession.etm.module.trade.dal.dataobject.contract.TradeContractDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TradeContractMapper extends BaseMapper<TradeContractDO> {

    default PageResult<TradeContractDO> selectPage(TradeContractPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TradeContractDO>()
                .likeIfPresent(TradeContractDO::getName, reqVO.getName())
                .likeIfPresent(TradeContractDO::getNo, reqVO.getNo())
                .eqIfPresent(TradeContractDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(TradeContractDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TradeContractDO::getCustomerId, reqVO.getCustomerId())
                .betweenIfPresent(TradeContractDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TradeContractDO::getId));
    }

    default List<TradeContractDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapper<TradeContractDO>()
                .eq(TradeContractDO::getOrderId, orderId));
    }

    default List<TradeContractDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapper<TradeContractDO>()
                .eq(TradeContractDO::getCustomerId, customerId));
    }

}