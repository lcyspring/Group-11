package com.meession.etm.module.marketing.dal.mysql.log;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.marketing.controller.admin.log.vo.SendLogPageReqVO;
import com.meession.etm.module.marketing.dal.dataobject.log.MarketingSendRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 营销发送记录 Mapper
 *
 * @author MITEDTSM
 */
@Mapper
public interface MarketingSendRecordMapper extends BaseMapperX<MarketingSendRecordDO> {

    default PageResult<MarketingSendRecordDO> selectPage(SendLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MarketingSendRecordDO>()
                .eqIfPresent(MarketingSendRecordDO::getCampaignId, reqVO.getCampaignId())
                .eqIfPresent(MarketingSendRecordDO::getChannel, reqVO.getChannel())
                .betweenIfPresent(MarketingSendRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MarketingSendRecordDO::getId));
    }

    default List<MarketingSendRecordDO> selectListByCampaignId(Long campaignId) {
        return selectList(MarketingSendRecordDO::getCampaignId, campaignId);
    }

    default Long selectCountByCampaignIdAndChannel(Long campaignId, String channel) {
        return selectCount(new LambdaQueryWrapperX<MarketingSendRecordDO>()
                .eq(MarketingSendRecordDO::getCampaignId, campaignId)
                .eq(MarketingSendRecordDO::getChannel, channel));
    }

}
