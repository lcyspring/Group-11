package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingCampaignRelationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CrmMarketingCampaignRelationMapper extends BaseMapperX<CrmMarketingCampaignRelationDO> {
    default List<CrmMarketingCampaignRelationDO> selectByCampaignId(Long campaignId) {
        return selectList(new LambdaQueryWrapperX<CrmMarketingCampaignRelationDO>()
                .eq(CrmMarketingCampaignRelationDO::getCampaignId, campaignId)
                .orderByAsc(CrmMarketingCampaignRelationDO::getBizType)
                .orderByAsc(CrmMarketingCampaignRelationDO::getBizId));
    }

    default void deleteByCampaignId(Long campaignId) {
        delete(CrmMarketingCampaignRelationDO::getCampaignId, campaignId);
    }

    default long countByCampaignIdAndBizIds(Long campaignId, Integer bizType, Collection<Long> bizIds) {
        return selectCount(new LambdaQueryWrapperX<CrmMarketingCampaignRelationDO>()
                .eq(CrmMarketingCampaignRelationDO::getCampaignId, campaignId)
                .eq(CrmMarketingCampaignRelationDO::getBizType, bizType)
                .in(CrmMarketingCampaignRelationDO::getBizId, bizIds));
    }
}
