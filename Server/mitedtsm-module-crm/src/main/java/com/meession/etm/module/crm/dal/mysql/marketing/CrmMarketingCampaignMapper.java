package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmMarketingCampaignPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingCampaignDO;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingCampaignStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.Set;

@Mapper
public interface CrmMarketingCampaignMapper extends BaseMapperX<CrmMarketingCampaignDO> {
    default CrmMarketingCampaignDO selectByCode(String code) {
        return selectOne(CrmMarketingCampaignDO::getCode, code);
    }

    default int deleteDraftById(Long id) {
        return delete(new LambdaQueryWrapperX<CrmMarketingCampaignDO>()
                .eq(CrmMarketingCampaignDO::getId, id)
                .eq(CrmMarketingCampaignDO::getStatus, CrmMarketingCampaignStatusEnum.DRAFT.getStatus()));
    }

    default PageResult<CrmMarketingCampaignDO> selectPage(CrmMarketingCampaignPageReqVO request,
                                                           boolean all, Set<Long> ownerUserIds) {
        LambdaQueryWrapperX<CrmMarketingCampaignDO> query = new LambdaQueryWrapperX<CrmMarketingCampaignDO>()
                .likeIfPresent(CrmMarketingCampaignDO::getCode, request.getCode())
                .likeIfPresent(CrmMarketingCampaignDO::getName, request.getName())
                .eqIfPresent(CrmMarketingCampaignDO::getStatus, request.getStatus())
                .eqIfPresent(CrmMarketingCampaignDO::getOwnerUserId, request.getOwnerUserId())
                .orderByDesc(CrmMarketingCampaignDO::getId);
        if (!all) {
            if (ownerUserIds.isEmpty()) query.eq(CrmMarketingCampaignDO::getOwnerUserId, -1L);
            else query.in(CrmMarketingCampaignDO::getOwnerUserId, ownerUserIds);
        }
        return selectPage(request, query);
    }
}
