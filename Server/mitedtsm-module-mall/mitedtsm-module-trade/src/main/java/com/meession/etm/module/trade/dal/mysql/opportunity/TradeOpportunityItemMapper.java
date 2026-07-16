package com.meession.etm.module.trade.dal.mysql.opportunity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meession.etm.module.trade.dal.dataobject.opportunity.TradeOpportunityItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TradeOpportunityItemMapper extends BaseMapper<TradeOpportunityItemDO> {

    default List<TradeOpportunityItemDO> selectListByOpportunityId(Long opportunityId) {
        return selectList(new LambdaQueryWrapper<TradeOpportunityItemDO>()
                .eq(TradeOpportunityItemDO::getOpportunityId, opportunityId));
    }

    default int deleteByOpportunityId(Long opportunityId) {
        return delete(new LambdaQueryWrapper<TradeOpportunityItemDO>()
                .eq(TradeOpportunityItemDO::getOpportunityId, opportunityId));
    }

}