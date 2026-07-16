package com.meession.etm.module.trade.dal.mysql.opportunity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.trade.controller.admin.opportunity.vo.TradeOpportunityPageReqVO;
import com.meession.etm.module.trade.dal.dataobject.opportunity.TradeOpportunityDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TradeOpportunityMapper extends BaseMapper<TradeOpportunityDO> {

    default PageResult<TradeOpportunityDO> selectPage(TradeOpportunityPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TradeOpportunityDO>()
                .likeIfPresent(TradeOpportunityDO::getName, reqVO.getName())
                .likeIfPresent(TradeOpportunityDO::getCustomerName, reqVO.getCustomerName())
                .eqIfPresent(TradeOpportunityDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TradeOpportunityDO::getSalesUserId, reqVO.getSalesUserId())
                .betweenIfPresent(TradeOpportunityDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TradeOpportunityDO::getId));
    }

    default List<TradeOpportunityDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<TradeOpportunityDO>()
                .eq(TradeOpportunityDO::getSalesUserId, userId));
    }

    default List<TradeOpportunityDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapper<TradeOpportunityDO>()
                .eq(TradeOpportunityDO::getCustomerId, customerId));
    }

}