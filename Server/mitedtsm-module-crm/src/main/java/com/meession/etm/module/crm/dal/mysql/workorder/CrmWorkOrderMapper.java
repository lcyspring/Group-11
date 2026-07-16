package com.meession.etm.module.crm.dal.mysql.workorder;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.workorder.vo.CrmWorkOrderPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Set;

@Mapper
public interface CrmWorkOrderMapper extends BaseMapperX<CrmWorkOrderDO> {

    default CrmWorkOrderDO selectByNo(String no) {
        return selectOne(CrmWorkOrderDO::getNo, no);
    }

    default PageResult<CrmWorkOrderDO> selectPage(CrmWorkOrderPageReqVO reqVO, Long userId, boolean queryAll,
                                                  Set<Long> managedGroupIds, Set<Long> memberGroupIds,
                                                  Set<Long> ccWorkOrderIds) {
        LambdaQueryWrapperX<CrmWorkOrderDO> query = new LambdaQueryWrapperX<CrmWorkOrderDO>()
                .likeIfPresent(CrmWorkOrderDO::getNo, reqVO.getNo())
                .likeIfPresent(CrmWorkOrderDO::getTitle, reqVO.getTitle())
                .eqIfPresent(CrmWorkOrderDO::getType, reqVO.getType())
                .eqIfPresent(CrmWorkOrderDO::getPriority, reqVO.getPriority())
                .eqIfPresent(CrmWorkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CrmWorkOrderDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(CrmWorkOrderDO::getHandlerUserId, reqVO.getHandlerUserId())
                .orderByDesc(CrmWorkOrderDO::getPriority)
                .orderByDesc(CrmWorkOrderDO::getCreateTime)
                .orderByDesc(CrmWorkOrderDO::getId);
        if (Boolean.TRUE.equals(reqVO.getBacklog())) {
            query.and(wrapper -> wrapper.eq(CrmWorkOrderDO::getHandlerUserId, userId)
                    .in(CrmWorkOrderDO::getStatus, 10, 20)
                    .or(nested -> nested.eq(CrmWorkOrderDO::getCreator, String.valueOf(userId))
                            .eq(CrmWorkOrderDO::getStatus, 40))
                    .or(!memberGroupIds.isEmpty(), nested -> nested.isNull(CrmWorkOrderDO::getHandlerUserId)
                            .in(CrmWorkOrderDO::getGroupId, memberGroupIds)
                            .eq(CrmWorkOrderDO::getStatus, 10)));
        } else if (Integer.valueOf(1).equals(reqVO.getSceneType())) {
            query.eq(CrmWorkOrderDO::getCreator, String.valueOf(userId));
        } else if (Integer.valueOf(2).equals(reqVO.getSceneType())) {
            query.eq(CrmWorkOrderDO::getHandlerUserId, userId);
        } else if (Integer.valueOf(3).equals(reqVO.getSceneType())) {
            if (ccWorkOrderIds.isEmpty()) query.eq(CrmWorkOrderDO::getId, -1L);
            else query.in(CrmWorkOrderDO::getId, ccWorkOrderIds);
        } else if (Integer.valueOf(4).equals(reqVO.getSceneType())) {
            if (memberGroupIds.isEmpty()) query.eq(CrmWorkOrderDO::getId, -1L);
            else query.isNull(CrmWorkOrderDO::getHandlerUserId).in(CrmWorkOrderDO::getGroupId, memberGroupIds)
                    .eq(CrmWorkOrderDO::getStatus, 10);
        } else if (!queryAll) {
            query.and(wrapper -> wrapper.eq(CrmWorkOrderDO::getCreator, String.valueOf(userId))
                    .or().eq(CrmWorkOrderDO::getHandlerUserId, userId)
                    .or(!ccWorkOrderIds.isEmpty(), nested -> nested.in(CrmWorkOrderDO::getId, ccWorkOrderIds))
                    .or(!managedGroupIds.isEmpty(), nested -> nested.in(CrmWorkOrderDO::getGroupId, managedGroupIds))
                    .or(!memberGroupIds.isEmpty(), nested -> nested.isNull(CrmWorkOrderDO::getHandlerUserId)
                            .in(CrmWorkOrderDO::getGroupId, memberGroupIds)));
        }
        return selectPage(reqVO, query);
    }

    default int assignIfPending(Long id, Integer pendingStatus, Long oldHandlerUserId, Long oldGroupId,
                                Long newGroupId, Long newHandlerUserId, Integer dispatchMode,
                                LocalDateTime assignTime) {
        LambdaUpdateWrapper<CrmWorkOrderDO> update = new LambdaUpdateWrapper<CrmWorkOrderDO>()
                .eq(CrmWorkOrderDO::getId, id)
                .eq(CrmWorkOrderDO::getStatus, pendingStatus)
                .set(CrmWorkOrderDO::getGroupId, newGroupId)
                .set(CrmWorkOrderDO::getHandlerUserId, newHandlerUserId)
                .set(CrmWorkOrderDO::getDispatchMode, dispatchMode)
                .set(CrmWorkOrderDO::getAssignTime, assignTime);
        if (oldHandlerUserId == null) update.isNull(CrmWorkOrderDO::getHandlerUserId);
        else update.eq(CrmWorkOrderDO::getHandlerUserId, oldHandlerUserId);
        if (oldGroupId == null) update.isNull(CrmWorkOrderDO::getGroupId);
        else update.eq(CrmWorkOrderDO::getGroupId, oldGroupId);
        return update(update);
    }

    default int claimIfUnassigned(Long id, Long groupId, Long userId, LocalDateTime assignTime) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderDO>()
                .eq(CrmWorkOrderDO::getId, id).eq(CrmWorkOrderDO::getStatus, 10)
                .eq(CrmWorkOrderDO::getGroupId, groupId).isNull(CrmWorkOrderDO::getHandlerUserId)
                .set(CrmWorkOrderDO::getHandlerUserId, userId).set(CrmWorkOrderDO::getDispatchMode, 3)
                .set(CrmWorkOrderDO::getAssignTime, assignTime));
    }

    default int countOpenByHandler(Long handlerUserId) {
        return Math.toIntExact(selectCount(new LambdaQueryWrapperX<CrmWorkOrderDO>()
                .eq(CrmWorkOrderDO::getHandlerUserId, handlerUserId).in(CrmWorkOrderDO::getStatus, 10, 20)));
    }

    default int startIfPending(Long id, Integer pendingStatus, Integer processingStatus, LocalDateTime processTime) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderDO>()
                .eq(CrmWorkOrderDO::getId, id).eq(CrmWorkOrderDO::getStatus, pendingStatus)
                .set(CrmWorkOrderDO::getStatus, processingStatus)
                .set(CrmWorkOrderDO::getProcessTime, processTime));
    }

    default int returnIfProcessing(Long id, Integer processingStatus, Integer returnedStatus, String reason) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderDO>()
                .eq(CrmWorkOrderDO::getId, id).eq(CrmWorkOrderDO::getStatus, processingStatus)
                .set(CrmWorkOrderDO::getStatus, returnedStatus)
                .set(CrmWorkOrderDO::getReturnReason, reason));
    }

    default int resubmitIfReturned(Long id, Integer returnedStatus, Integer pendingStatus) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderDO>()
                .eq(CrmWorkOrderDO::getId, id).eq(CrmWorkOrderDO::getStatus, returnedStatus)
                .set(CrmWorkOrderDO::getStatus, pendingStatus)
                .set(CrmWorkOrderDO::getReturnReason, null)
                .set(CrmWorkOrderDO::getProcessTime, null));
    }

    default int completeIfProcessing(Long id, Integer processingStatus, Integer completedStatus,
                                     String solution, LocalDateTime completeTime) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderDO>()
                .eq(CrmWorkOrderDO::getId, id).eq(CrmWorkOrderDO::getStatus, processingStatus)
                .set(CrmWorkOrderDO::getStatus, completedStatus)
                .set(CrmWorkOrderDO::getSolution, solution)
                .set(CrmWorkOrderDO::getCompleteTime, completeTime));
    }
}
