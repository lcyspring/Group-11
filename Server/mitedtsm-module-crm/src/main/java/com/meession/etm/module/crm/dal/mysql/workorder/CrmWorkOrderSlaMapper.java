package com.meession.etm.module.crm.dal.mysql.workorder;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderSlaDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrmWorkOrderSlaMapper extends BaseMapperX<CrmWorkOrderSlaDO> {
    default CrmWorkOrderSlaDO selectByWorkOrderId(Long workOrderId) {
        return selectOne(new LambdaQueryWrapperX<CrmWorkOrderSlaDO>()
                .eq(CrmWorkOrderSlaDO::getWorkOrderId, workOrderId));
    }
    default List<CrmWorkOrderSlaDO> selectDue(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<CrmWorkOrderSlaDO>()
                .in(CrmWorkOrderSlaDO::getStatus, 0, 1, 3)
                .le(CrmWorkOrderSlaDO::getEscalationDueTime, now));
    }
    default int pause(Long id, LocalDateTime at) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderSlaDO>()
                .eq(CrmWorkOrderSlaDO::getId, id).in(CrmWorkOrderSlaDO::getStatus, 0, 1)
                .isNull(CrmWorkOrderSlaDO::getPausedAt)
                .set(CrmWorkOrderSlaDO::getPausedAt, at));
    }
    default int resume(Long id, long seconds) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderSlaDO>()
                .eq(CrmWorkOrderSlaDO::getId, id).isNotNull(CrmWorkOrderSlaDO::getPausedAt)
                .set(CrmWorkOrderSlaDO::getPausedAt, null)
                .setSql("paused_seconds = paused_seconds + " + seconds
                        + ", response_due_time = DATE_ADD(response_due_time, INTERVAL " + seconds + " SECOND)"
                        + ", escalation_due_time = DATE_ADD(escalation_due_time, INTERVAL " + seconds + " SECOND)"
                        + ", resolution_due_time = DATE_ADD(resolution_due_time, INTERVAL " + seconds + " SECOND)"));
    }
    default int markEscalated(Long id, LocalDateTime at) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderSlaDO>()
                .eq(CrmWorkOrderSlaDO::getId, id).in(CrmWorkOrderSlaDO::getStatus, 0, 1)
                .set(CrmWorkOrderSlaDO::getStatus, 4).set(CrmWorkOrderSlaDO::getEscalatedAt, at));
    }
    default int markOverdue(Long id, LocalDateTime at) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderSlaDO>()
                .eq(CrmWorkOrderSlaDO::getId, id).in(CrmWorkOrderSlaDO::getStatus, 0, 1)
                .le(CrmWorkOrderSlaDO::getResolutionDueTime, at)
                .set(CrmWorkOrderSlaDO::getStatus, 3));
    }
    default int markResponded(Long id) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderSlaDO>()
                .eq(CrmWorkOrderSlaDO::getId, id).eq(CrmWorkOrderSlaDO::getStatus, 0)
                .set(CrmWorkOrderSlaDO::getStatus, 1));
    }
    default int markCompleted(Long id, LocalDateTime at) {
        return update(new LambdaUpdateWrapper<CrmWorkOrderSlaDO>()
                .eq(CrmWorkOrderSlaDO::getId, id).notIn(CrmWorkOrderSlaDO::getStatus, 2)
                .set(CrmWorkOrderSlaDO::getStatus, 2).set(CrmWorkOrderSlaDO::getCompletedAt, at));
    }
}
