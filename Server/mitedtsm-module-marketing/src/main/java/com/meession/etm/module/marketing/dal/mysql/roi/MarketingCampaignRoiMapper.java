package com.meession.etm.module.marketing.dal.mysql.roi;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisReqVO;
import com.meession.etm.module.marketing.dal.dataobject.roi.MarketingCampaignRoiDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 营销活动 ROI 数据 Mapper
 *
 * @author MITEDTSM
 */
@Mapper
public interface MarketingCampaignRoiMapper extends BaseMapperX<MarketingCampaignRoiDO> {

    default List<MarketingCampaignRoiDO> selectList(RoiAnalysisReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<MarketingCampaignRoiDO>()
                .eqIfPresent(MarketingCampaignRoiDO::getCampaignId, reqVO.getCampaignId())
                .eqIfPresent(MarketingCampaignRoiDO::getChannel, reqVO.getChannel())
                .betweenIfPresent(MarketingCampaignRoiDO::getStatDate, reqVO.getStatDate())
                .orderByAsc(MarketingCampaignRoiDO::getStatDate));
    }

}
