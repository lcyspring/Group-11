package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmMarketingBroadcastPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface CrmMarketingBroadcastMapper extends BaseMapperX<CrmMarketingBroadcastDO> {
    default PageResult<CrmMarketingBroadcastDO> selectPage(CrmMarketingBroadcastPageReqVO request,
                                                            boolean readAll, String creator) {
        LambdaQueryWrapperX<CrmMarketingBroadcastDO> query = new LambdaQueryWrapperX<CrmMarketingBroadcastDO>()
                .likeIfPresent(CrmMarketingBroadcastDO::getName, request.getName())
                .eqIfPresent(CrmMarketingBroadcastDO::getChannel, request.getChannel())
                .eqIfPresent(CrmMarketingBroadcastDO::getStatus, request.getStatus())
                .eqIfPresent(CrmMarketingBroadcastDO::getCampaignId, request.getCampaignId())
                .orderByDesc(CrmMarketingBroadcastDO::getId);
        if (!readAll) {
            query.eq(CrmMarketingBroadcastDO::getCreator, creator);
        }
        return selectPage(request, query);
    }

    default int updateEditable(CrmMarketingBroadcastDO row, Collection<Integer> expectedStatuses) {
        return update(row, new LambdaUpdateWrapper<CrmMarketingBroadcastDO>()
                .eq(CrmMarketingBroadcastDO::getId, row.getId())
                .in(CrmMarketingBroadcastDO::getStatus, expectedStatuses));
    }

    default int transition(Long id, Collection<Integer> expectedStatuses, Integer targetStatus) {
        return update(new LambdaUpdateWrapper<CrmMarketingBroadcastDO>()
                .eq(CrmMarketingBroadcastDO::getId, id)
                .in(CrmMarketingBroadcastDO::getStatus, expectedStatuses)
                .set(CrmMarketingBroadcastDO::getStatus, targetStatus));
    }

    default int submitReview(Long id, Integer draftStatus, Integer pendingStatus, String processInstanceId) {
        return update(new LambdaUpdateWrapper<CrmMarketingBroadcastDO>()
                .eq(CrmMarketingBroadcastDO::getId, id)
                .eq(CrmMarketingBroadcastDO::getStatus, draftStatus)
                .set(CrmMarketingBroadcastDO::getStatus, pendingStatus)
                .set(CrmMarketingBroadcastDO::getProcessInstanceId, processInstanceId)
                .set(CrmMarketingBroadcastDO::getReviewerUserId, null)
                .set(CrmMarketingBroadcastDO::getReviewedAt, null)
                .set(CrmMarketingBroadcastDO::getReviewComment, null));
    }

    default int updateReviewStatus(Long id, String processInstanceId, Integer pendingStatus,
                                   Integer targetStatus, LocalDateTime reviewedAt, String comment) {
        return update(new LambdaUpdateWrapper<CrmMarketingBroadcastDO>()
                .eq(CrmMarketingBroadcastDO::getId, id)
                .eq(CrmMarketingBroadcastDO::getProcessInstanceId, processInstanceId)
                .eq(CrmMarketingBroadcastDO::getStatus, pendingStatus)
                .set(CrmMarketingBroadcastDO::getStatus, targetStatus)
                .set(CrmMarketingBroadcastDO::getReviewedAt, reviewedAt)
                .set(CrmMarketingBroadcastDO::getReviewComment, comment));
    }

    default int reviewIfPending(Long id, Long reviewerUserId, LocalDateTime reviewedAt,
                                String comment, Integer targetStatus, Integer pendingStatus) {
        return update(new LambdaUpdateWrapper<CrmMarketingBroadcastDO>()
                .eq(CrmMarketingBroadcastDO::getId, id)
                .eq(CrmMarketingBroadcastDO::getStatus, pendingStatus)
                .set(CrmMarketingBroadcastDO::getReviewerUserId, reviewerUserId)
                .set(CrmMarketingBroadcastDO::getReviewedAt, reviewedAt)
                .set(CrmMarketingBroadcastDO::getReviewComment, comment)
                .set(CrmMarketingBroadcastDO::getStatus, targetStatus));
    }

    default int deleteDraft(Long id, Integer draftStatus) {
        return delete(new LambdaUpdateWrapper<CrmMarketingBroadcastDO>()
                .eq(CrmMarketingBroadcastDO::getId, id)
                .eq(CrmMarketingBroadcastDO::getStatus, draftStatus));
    }

    default List<CrmMarketingBroadcastDO> selectDueScheduled(Integer readyStatus, LocalDateTime now, int limit) {
        return selectList(new com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX<CrmMarketingBroadcastDO>()
                .eq(CrmMarketingBroadcastDO::getStatus, readyStatus)
                .isNotNull(CrmMarketingBroadcastDO::getScheduledAt)
                .le(CrmMarketingBroadcastDO::getScheduledAt, now)
                .orderByAsc(CrmMarketingBroadcastDO::getScheduledAt)
                .orderByAsc(CrmMarketingBroadcastDO::getId)
                .last("LIMIT " + Math.max(1, limit)));
    }
}
