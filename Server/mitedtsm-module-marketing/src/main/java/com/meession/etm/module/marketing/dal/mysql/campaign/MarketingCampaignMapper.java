package com.meession.etm.module.marketing.dal.mysql.campaign;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.marketing.controller.admin.campaign.vo.CampaignPageReqVO;
import com.meession.etm.module.marketing.dal.dataobject.campaign.MarketingCampaignDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营销活动 Mapper
 *
 * @author MITEDTSM
 */
@Mapper
public interface MarketingCampaignMapper extends BaseMapperX<MarketingCampaignDO> {

    default PageResult<MarketingCampaignDO> selectPage(CampaignPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MarketingCampaignDO>()
                .eqIfPresent(MarketingCampaignDO::getType, reqVO.getType())
                .eqIfPresent(MarketingCampaignDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MarketingCampaignDO::getTargetType, reqVO.getTargetType())
                .likeIfPresent(MarketingCampaignDO::getName, reqVO.getName())
                .betweenIfPresent(MarketingCampaignDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MarketingCampaignDO::getId));
    }

    default MarketingCampaignDO selectByName(String name) {
        return selectOne(MarketingCampaignDO::getName, name);
    }

}
