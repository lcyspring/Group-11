package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmCompetitorPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCompetitorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Set;

@Mapper
public interface CrmCompetitorMapper extends BaseMapperX<CrmCompetitorDO> {
    default PageResult<CrmCompetitorDO> selectPage(CrmCompetitorPageReqVO request,
                                                   boolean all, Set<Long> ownerUserIds) {
        LambdaQueryWrapperX<CrmCompetitorDO> query = new LambdaQueryWrapperX<CrmCompetitorDO>()
                .likeIfPresent(CrmCompetitorDO::getName, request.getName())
                .eqIfPresent(CrmCompetitorDO::getStatus, request.getStatus())
                .eqIfPresent(CrmCompetitorDO::getOwnerUserId, request.getOwnerUserId())
                .orderByDesc(CrmCompetitorDO::getId);
        if (!all) {
            if (ownerUserIds.isEmpty()) query.eq(CrmCompetitorDO::getOwnerUserId, -1L);
            else query.in(CrmCompetitorDO::getOwnerUserId, ownerUserIds);
        }
        return selectPage(request, query);
    }
}
