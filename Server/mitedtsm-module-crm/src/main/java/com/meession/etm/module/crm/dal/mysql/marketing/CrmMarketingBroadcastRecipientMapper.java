package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmMarketingRecipientPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastRecipientDO;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingRecipientStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingDeliveryStatusEnum;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrmMarketingBroadcastRecipientMapper extends BaseMapperX<CrmMarketingBroadcastRecipientDO> {
    default List<CrmMarketingBroadcastRecipientDO> selectPending(Long broadcastId, int limit) {
        return selectList(new LambdaQueryWrapperX<CrmMarketingBroadcastRecipientDO>()
                .eq(CrmMarketingBroadcastRecipientDO::getBroadcastId, broadcastId)
                .eq(CrmMarketingBroadcastRecipientDO::getStatus, CrmMarketingRecipientStatusEnum.PENDING.getStatus())
                .orderByAsc(CrmMarketingBroadcastRecipientDO::getId)
                .last("LIMIT " + Math.max(1, limit)));
    }

    default PageResult<CrmMarketingBroadcastRecipientDO> selectPage(CrmMarketingRecipientPageReqVO request) {
        return selectPage(request, new LambdaQueryWrapperX<CrmMarketingBroadcastRecipientDO>()
                .eq(CrmMarketingBroadcastRecipientDO::getBroadcastId, request.getBroadcastId())
                .eqIfPresent(CrmMarketingBroadcastRecipientDO::getStatus, request.getStatus())
                .orderByAsc(CrmMarketingBroadcastRecipientDO::getId));
    }

    default int claimForSending(Long id, LocalDateTime now) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrmMarketingBroadcastRecipientDO>()
                .eq(CrmMarketingBroadcastRecipientDO::getId, id)
                .eq(CrmMarketingBroadcastRecipientDO::getStatus, CrmMarketingRecipientStatusEnum.PENDING.getStatus())
                .set(CrmMarketingBroadcastRecipientDO::getStatus, CrmMarketingRecipientStatusEnum.SENDING.getStatus())
                .set(CrmMarketingBroadcastRecipientDO::getLastAttemptAt, now)
                .setSql("attempt_count = attempt_count + 1"));
    }

    default int resetFailed(Long broadcastId) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrmMarketingBroadcastRecipientDO>()
                .eq(CrmMarketingBroadcastRecipientDO::getBroadcastId, broadcastId)
                .eq(CrmMarketingBroadcastRecipientDO::getStatus, CrmMarketingRecipientStatusEnum.FAILED.getStatus())
                .set(CrmMarketingBroadcastRecipientDO::getStatus, CrmMarketingRecipientStatusEnum.PENDING.getStatus())
                .set(CrmMarketingBroadcastRecipientDO::getFailureReason, null));
    }

    default List<CrmMarketingBroadcastRecipientDO> selectPendingDeliveryResults(int limit) {
        return selectList(new LambdaQueryWrapperX<CrmMarketingBroadcastRecipientDO>()
                .isNotNull(CrmMarketingBroadcastRecipientDO::getProviderLogId)
                .eq(CrmMarketingBroadcastRecipientDO::getDeliveryStatus,
                        CrmMarketingDeliveryStatusEnum.PROVIDER_PENDING.getStatus())
                .orderByAsc(CrmMarketingBroadcastRecipientDO::getId)
                .last("LIMIT " + Math.max(1, limit)));
    }

    @TenantIgnore
    default CrmMarketingBroadcastRecipientDO selectByTrackingToken(String token) {
        return selectOne(CrmMarketingBroadcastRecipientDO::getTrackingToken, token);
    }

    @TenantIgnore
    default CrmMarketingBroadcastRecipientDO selectByIdIgnoringTenant(Long id) {
        return selectById(id);
    }

    @TenantIgnore
    default int markOpened(String token, LocalDateTime openedAt) {
        return update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrmMarketingBroadcastRecipientDO>()
                .eq(CrmMarketingBroadcastRecipientDO::getTrackingToken, token)
                .eq(CrmMarketingBroadcastRecipientDO::getChannel, 2)
                .eq(CrmMarketingBroadcastRecipientDO::getStatus,
                        CrmMarketingRecipientStatusEnum.SENT.getStatus())
                .isNull(CrmMarketingBroadcastRecipientDO::getOpenedAt)
                .and(wrapper -> wrapper.isNull(CrmMarketingBroadcastRecipientDO::getDeliveryStatus)
                        .or().ne(CrmMarketingBroadcastRecipientDO::getDeliveryStatus,
                                CrmMarketingDeliveryStatusEnum.FAILED.getStatus()))
                .set(CrmMarketingBroadcastRecipientDO::getOpenedAt, openedAt)
                .set(CrmMarketingBroadcastRecipientDO::getDeliveryStatus,
                        CrmMarketingDeliveryStatusEnum.ACCEPTED.getStatus()));
    }
}
