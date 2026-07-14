package com.meession.etm.module.crm.dal.mysql.workorder;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.workorder.vo.CrmWorkOrderPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface CrmWorkOrderMapper extends BaseMapperX<CrmWorkOrderDO> {

    default CrmWorkOrderDO selectByNo(String no) {
        return selectOne(CrmWorkOrderDO::getNo, no);
    }

    default PageResult<CrmWorkOrderDO> selectPage(CrmWorkOrderPageReqVO reqVO, Long userId, boolean queryAll) {
        LambdaQueryWrapperX<CrmWorkOrderDO> query = new LambdaQueryWrapperX<CrmWorkOrderDO>()
                .likeIfPresent(CrmWorkOrderDO::getNo, reqVO.getNo())
                .likeIfPresent(CrmWorkOrderDO::getTitle, reqVO.getTitle())
                .eqIfPresent(CrmWorkOrderDO::getType, reqVO.getType())
                .eqIfPresent(CrmWorkOrderDO::getPriority, reqVO.getPriority())
                .eqIfPresent(CrmWorkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CrmWorkOrderDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(CrmWorkOrderDO::getHandlerUserId, reqVO.getHandlerUserId())
                .orderByDesc(CrmWorkOrderDO::getId);
        if (Boolean.TRUE.equals(reqVO.getBacklog())) {
            query.and(wrapper -> wrapper.eq(CrmWorkOrderDO::getHandlerUserId, userId)
                    .in(CrmWorkOrderDO::getStatus, 10, 20)
                    .or(nested -> nested.eq(CrmWorkOrderDO::getCreator, String.valueOf(userId))
                            .eq(CrmWorkOrderDO::getStatus, 40)));
        } else if (!queryAll) {
            if (Integer.valueOf(1).equals(reqVO.getSceneType())) {
                query.eq(CrmWorkOrderDO::getCreator, String.valueOf(userId));
            } else if (Integer.valueOf(2).equals(reqVO.getSceneType())) {
                query.eq(CrmWorkOrderDO::getHandlerUserId, userId);
            } else {
                query.and(wrapper -> wrapper.eq(CrmWorkOrderDO::getCreator, String.valueOf(userId))
                        .or().eq(CrmWorkOrderDO::getHandlerUserId, userId));
            }
        }
        return selectPage(reqVO, query);
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
