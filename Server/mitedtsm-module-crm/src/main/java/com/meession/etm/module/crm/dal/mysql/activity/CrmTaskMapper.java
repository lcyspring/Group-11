package com.meession.etm.module.crm.dal.mysql.activity;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.activity.vo.CrmTaskPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.activity.CrmTaskDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrmTaskMapper extends BaseMapperX<CrmTaskDO> {

    default PageResult<CrmTaskDO> selectPage(CrmTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CrmTaskDO>()
                .eq(CrmTaskDO::getBizType, reqVO.getBizType())
                .eq(CrmTaskDO::getBizId, reqVO.getBizId())
                .eqIfPresent(CrmTaskDO::getType, reqVO.getType())
                .eqIfPresent(CrmTaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CrmTaskDO::getAssigneeUserId, reqVO.getAssigneeUserId())
                .orderByAsc(CrmTaskDO::getDueTime).orderByDesc(CrmTaskDO::getId));
    }

    @Select("SELECT * FROM crm_task WHERE id=#{id} AND deleted=b'0' FOR UPDATE")
    CrmTaskDO selectByIdForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE crm_task SET type=#{task.type}, title=#{task.title}, description=#{task.description},
              priority=#{task.priority}, assignee_user_id=#{task.assigneeUserId}, due_time=#{task.dueTime},
              remind_time=#{task.remindTime}, notify_system=#{task.notifySystem},
              notify_email=#{task.notifyEmail}, notify_sms=#{task.notifySms}, version=version+1,
              updater=#{userId}, update_time=NOW()
            WHERE id=#{task.id} AND deleted=b'0' AND status=0 AND version=#{task.version}
            """)
    int updateDraft(@Param("task") CrmTaskDO task, @Param("userId") Long userId);

    @Update("""
            UPDATE crm_task SET status=10, start_time=#{now}, version=version+1,
              updater=#{userId}, update_time=NOW()
            WHERE id=#{id} AND deleted=b'0' AND status IN (0,50)
            """)
    int startIfStartable(@Param("id") Long id, @Param("userId") Long userId,
                         @Param("now") LocalDateTime now);

    @Update("""
            UPDATE crm_task SET status=#{targetStatus}, finish_time=#{now}, result=#{remark},
              version=version+1, updater=#{userId}, update_time=NOW()
            WHERE id=#{id} AND deleted=b'0' AND status IN (0,10,50)
            """)
    int finishIfOpen(@Param("id") Long id, @Param("targetStatus") Integer targetStatus,
                     @Param("remark") String remark, @Param("userId") Long userId,
                     @Param("now") LocalDateTime now);

    default List<CrmTaskDO> selectOverdueCandidates(long afterId, LocalDateTime now,
                                                     int batchSize, int maxBatchSize) {
        return selectList(new LambdaQueryWrapperX<CrmTaskDO>()
                .in(CrmTaskDO::getStatus, 0, 10)
                .lt(CrmTaskDO::getDueTime, now)
                .gt(CrmTaskDO::getId, afterId)
                .orderByAsc(CrmTaskDO::getId)
                .last("LIMIT " + Math.max(1, Math.min(batchSize, maxBatchSize))));
    }

    default List<CrmTaskDO> selectListByBiz(Integer bizType, Long bizId) {
        return selectList(new LambdaQueryWrapperX<CrmTaskDO>()
                .eq(CrmTaskDO::getBizType, bizType).eq(CrmTaskDO::getBizId, bizId)
                .orderByAsc(CrmTaskDO::getId));
    }

    @Update("""
            UPDATE crm_task SET status=50, version=version+1, updater='crm-task-overdue-job', update_time=NOW()
            WHERE id=#{id} AND deleted=b'0' AND status IN (0,10) AND due_time < #{now}
            """)
    int markOverdueIfOpen(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE crm_task SET biz_type=#{customerType}, biz_id=#{customerId}, source_clue_id=#{clueId},
              version=version+1, updater=#{userId}, update_time=NOW()
            WHERE deleted=b'0' AND biz_type=#{clueType} AND biz_id=#{clueId}
              AND source_clue_id IS NULL
            """)
    int migrateFromClue(@Param("clueType") Integer clueType, @Param("customerType") Integer customerType,
                        @Param("clueId") Long clueId, @Param("customerId") Long customerId,
                        @Param("userId") Long userId);

    default int migrateFromClue(Long clueId, Long customerId, Long userId) {
        return migrateFromClue(CrmBizTypeEnum.CRM_CLUE.getType(), CrmBizTypeEnum.CRM_CUSTOMER.getType(),
                clueId, customerId, userId);
    }
}
