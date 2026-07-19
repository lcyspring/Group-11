package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingLinkRecipientDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrmMarketingLinkRecipientMapper extends BaseMapperX<CrmMarketingLinkRecipientDO> {
    default List<CrmMarketingLinkRecipientDO> selectByLinkIds(List<Long> linkIds) {
        if (linkIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapperX<CrmMarketingLinkRecipientDO>()
                .in(CrmMarketingLinkRecipientDO::getLinkId, linkIds)
                .orderByAsc(CrmMarketingLinkRecipientDO::getId));
    }

    default CrmMarketingLinkRecipientDO selectByLinkAndRecipient(Long linkId, Long recipientId) {
        return selectOne(new LambdaQueryWrapperX<CrmMarketingLinkRecipientDO>()
                .eq(CrmMarketingLinkRecipientDO::getLinkId, linkId)
                .eq(CrmMarketingLinkRecipientDO::getRecipientId, recipientId));
    }

    @TenantIgnore
    default CrmMarketingLinkRecipientDO selectByTrackingToken(String token) {
        return selectOne(CrmMarketingLinkRecipientDO::getTrackingToken, token);
    }

    @TenantIgnore
    @Update("UPDATE crm_marketing_link_recipient "
            + "SET first_clicked_at=COALESCE(first_clicked_at, #{clickedAt}), "
            + "last_clicked_at=#{clickedAt}, click_count=click_count+1 "
            + "WHERE id=#{id} AND deleted=b'0'")
    int recordClick(@Param("id") Long id, @Param("clickedAt") LocalDateTime clickedAt);

    @Delete("<script>DELETE FROM crm_marketing_link_recipient WHERE link_id IN "
            + "<foreach collection='linkIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deletePhysicalByLinkIds(@Param("linkIds") List<Long> linkIds);
}
