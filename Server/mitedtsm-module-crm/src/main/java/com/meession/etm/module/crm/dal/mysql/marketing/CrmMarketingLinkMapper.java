package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingLinkDO;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CrmMarketingLinkMapper extends BaseMapperX<CrmMarketingLinkDO> {
    default List<CrmMarketingLinkDO> selectByBroadcastId(Long broadcastId) {
        return selectList(new LambdaQueryWrapperX<CrmMarketingLinkDO>()
                .eq(CrmMarketingLinkDO::getBroadcastId, broadcastId)
                .orderByAsc(CrmMarketingLinkDO::getId));
    }

    @Delete("DELETE FROM crm_marketing_link WHERE broadcast_id=#{broadcastId}")
    int deletePhysicalByBroadcast(@Param("broadcastId") Long broadcastId);

    @TenantIgnore
    default CrmMarketingLinkDO selectByIdIgnoringTenant(Long id) {
        return selectById(id);
    }
}
