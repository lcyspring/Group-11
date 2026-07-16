package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmMarketingBroadcastPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrmMarketingBroadcastMapper extends BaseMapperX<CrmMarketingBroadcastDO> {
    default PageResult<CrmMarketingBroadcastDO> selectPage(CrmMarketingBroadcastPageReqVO request) {
        return selectPage(request, new LambdaQueryWrapperX<CrmMarketingBroadcastDO>()
                .likeIfPresent(CrmMarketingBroadcastDO::getName, request.getName())
                .eqIfPresent(CrmMarketingBroadcastDO::getStatus, request.getStatus())
                .eqIfPresent(CrmMarketingBroadcastDO::getCampaignId, request.getCampaignId())
                .orderByDesc(CrmMarketingBroadcastDO::getId));
    }
}
