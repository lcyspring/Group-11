package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAEventDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BpmOAEventMapper extends BaseMapperX<BpmOAEventDO> {
    default List<BpmOAEventDO> selectByUserId(Long userId, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return selectList(new LambdaQueryWrapperX<BpmOAEventDO>().eq(BpmOAEventDO::getUserId, userId)
                .ne(BpmOAEventDO::getStatus, 10).lt(BpmOAEventDO::getStartTime, to)
                .gt(BpmOAEventDO::getEndTime, from).orderByAsc(BpmOAEventDO::getStartTime));
    }

    @Select("""
            SELECT * FROM bpm_oa_event
            WHERE deleted = b'0' AND status = 0 AND reminder_minutes IS NOT NULL
              AND reminder_minutes >= 0 AND reminder_status = 0
              AND DATE_SUB(start_time, INTERVAL reminder_minutes MINUTE) <= #{now}
              AND start_time > #{now}
            ORDER BY start_time, id LIMIT #{limit}
            """)
    List<BpmOAEventDO> selectDueReminders(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("UPDATE bpm_oa_event SET reminder_status = 1, reminder_last_error = NULL " +
            "WHERE id = #{id} AND reminder_status = 0 AND status = 0 AND deleted = b'0'")
    int claimReminder(@Param("id") Long id);

    @Update("UPDATE bpm_oa_event SET reminder_status = 2, reminder_sent_time = #{sentTime}, " +
            "reminder_last_error = NULL WHERE id = #{id} AND reminder_status = 1")
    int markReminderSent(@Param("id") Long id, @Param("sentTime") LocalDateTime sentTime);

    @Update("UPDATE bpm_oa_event SET reminder_status = 0, reminder_last_error = #{error} " +
            "WHERE id = #{id} AND reminder_status = 1")
    int releaseReminder(@Param("id") Long id, @Param("error") String error);
}
