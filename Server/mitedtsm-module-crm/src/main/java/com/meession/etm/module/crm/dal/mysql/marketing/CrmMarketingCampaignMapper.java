package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.campaign.CrmMarketingCampaignPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingCampaignDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 营销活动 Mapper
 *
 * @author mitedtsm
 */
@Mapper
public interface CrmMarketingCampaignMapper extends BaseMapperX<CrmMarketingCampaignDO> {

    default PageResult<CrmMarketingCampaignDO> selectPage(CrmMarketingCampaignPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmMarketingCampaignDO>()
                .likeIfPresent(CrmMarketingCampaignDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmMarketingCampaignDO::getType, pageReqVO.getType())
                .eqIfPresent(CrmMarketingCampaignDO::getStatus, pageReqVO.getStatus())
                .betweenIfPresent(CrmMarketingCampaignDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(CrmMarketingCampaignDO::getId));
    }

    default List<CrmMarketingCampaignDO> selectListByStatus(Integer status) {
        return selectList(CrmMarketingCampaignDO::getStatus, status);
    }

}
