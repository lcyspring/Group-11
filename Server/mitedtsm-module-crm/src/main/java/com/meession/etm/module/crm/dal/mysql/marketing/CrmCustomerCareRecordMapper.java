package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmCustomerCareRecordPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCareRecordDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Set;

@Mapper
public interface CrmCustomerCareRecordMapper extends BaseMapperX<CrmCustomerCareRecordDO> {
    default PageResult<CrmCustomerCareRecordDO> selectPage(CrmCustomerCareRecordPageReqVO request,
                                                            boolean all, Set<Long> ownerUserIds) {
        MPJLambdaWrapperX<CrmCustomerCareRecordDO> query = new MPJLambdaWrapperX<>();
        query.selectAll(CrmCustomerCareRecordDO.class)
                .leftJoin(CrmCustomerDO.class, CrmCustomerDO::getId, CrmCustomerCareRecordDO::getCustomerId)
                .eqIfPresent(CrmCustomerCareRecordDO::getPlanId, request.getPlanId())
                .eqIfPresent(CrmCustomerCareRecordDO::getStatus, request.getStatus())
                .eqIfPresent(CrmCustomerCareRecordDO::getEventDate, request.getEventDate())
                .orderByDesc(CrmCustomerCareRecordDO::getId);
        if (!all) {
            if (ownerUserIds.isEmpty()) query.eq(CrmCustomerDO::getOwnerUserId, -1L);
            else query.in(CrmCustomerDO::getOwnerUserId, ownerUserIds);
        }
        return selectJoinPage(request, CrmCustomerCareRecordDO.class, query);
    }

    default long countDeliveredSince(Long customerId, Long contactId, Integer channel, LocalDateTime since) {
        return selectCount(new LambdaQueryWrapperX<CrmCustomerCareRecordDO>()
                .eq(CrmCustomerCareRecordDO::getCustomerId, customerId)
                .eqIfPresent(CrmCustomerCareRecordDO::getContactId, contactId)
                .eq(CrmCustomerCareRecordDO::getChannel, channel)
                .ge(CrmCustomerCareRecordDO::getSentAt, since)
                .in(CrmCustomerCareRecordDO::getStatus, 20, 50));
    }

    default long countDeliveredSince(LocalDateTime since) {
        return selectCount(new LambdaQueryWrapperX<CrmCustomerCareRecordDO>()
                .ge(CrmCustomerCareRecordDO::getSentAt, since)
                .in(CrmCustomerCareRecordDO::getStatus, 20, 50));
    }
}
