package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOATaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BpmOATaskMapper extends BaseMapperX<BpmOATaskDO> {
    @Select("""
            SELECT * FROM bpm_oa_task
            WHERE deleted = b'0'
              AND (creator_user_id = #{userId} OR assignee_user_id = #{userId}
                   OR JSON_CONTAINS(COALESCE(participant_user_ids, '[]'), CAST(#{userId} AS JSON), '$'))
              AND (#{status} IS NULL OR status = #{status})
            ORDER BY CASE status WHEN 0 THEN 0 WHEN 1 THEN 1 ELSE 2 END, due_time, id DESC
            """)
    List<BpmOATaskDO> selectAccessibleList(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("""
            SELECT * FROM bpm_oa_task WHERE deleted=b'0' AND status < 2 AND reminder_minutes IS NOT NULL
              AND reminder_status=0 AND DATE_SUB(due_time, INTERVAL reminder_minutes MINUTE) <= #{now}
              AND due_time > #{now} ORDER BY due_time,id LIMIT #{limit}
            """)
    List<BpmOATaskDO> selectDueReminders(@Param("now") LocalDateTime now, @Param("limit") int limit);
    @Update("UPDATE bpm_oa_task SET reminder_status=1,reminder_last_error=NULL WHERE id=#{id} AND reminder_status=0 AND status<2")
    int claimReminder(@Param("id") Long id);
    @Update("UPDATE bpm_oa_task SET reminder_status=2,reminder_sent_time=#{time},reminder_last_error=NULL WHERE id=#{id} AND reminder_status=1")
    int markReminderSent(@Param("id") Long id, @Param("time") LocalDateTime time);
    @Update("UPDATE bpm_oa_task SET reminder_status=0,reminder_last_error=#{error} WHERE id=#{id} AND reminder_status=1")
    int releaseReminder(@Param("id") Long id, @Param("error") String error);
}
