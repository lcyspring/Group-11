package com.meession.etm.module.crm.dal.mysql.receivable;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableOverdueReminderDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrmReceivableOverdueReminderMapper extends BaseMapperX<CrmReceivableOverdueReminderDO> {

    @Insert("""
            INSERT IGNORE INTO crm_receivable_overdue_reminder
              (receivable_plan_id, recipient_user_id, reminder_date, status, attempts,
               creator, create_time, updater, update_time, deleted, tenant_id)
            SELECT p.id, p.owner_user_id, #{reminderDate}, 0, 0,
                   'receivable-overdue-job', NOW(), 'receivable-overdue-job', NOW(), b'0', p.tenant_id
              FROM crm_receivable_plan p
              LEFT JOIN crm_receivable r ON r.id = p.receivable_id AND r.deleted = b'0'
             WHERE p.deleted = b'0'
               AND (p.receivable_id IS NULL OR r.audit_status IS NULL OR r.audit_status <> 20)
               AND p.return_time < #{todayStart}
               AND NOT EXISTS (
                   SELECT 1 FROM crm_receivable_overdue_reminder existing
                    WHERE existing.receivable_plan_id=p.id
                      AND existing.reminder_date=#{reminderDate}
                      AND existing.tenant_id=p.tenant_id
                      AND existing.deleted=b'0'
               )
             ORDER BY p.id
             LIMIT #{limit}
            """)
    int createDueFacts(@Param("reminderDate") LocalDate reminderDate,
                       @Param("todayStart") LocalDateTime todayStart,
                       @Param("limit") int limit);

    @Select("""
            SELECT * FROM crm_receivable_overdue_reminder
             WHERE deleted = b'0' AND status IN (0, 2) AND attempts < #{maxRetries}
             ORDER BY id LIMIT #{limit}
            """)
    List<CrmReceivableOverdueReminderDO> selectRetryable(@Param("maxRetries") int maxRetries,
                                                         @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*) FROM crm_receivable_plan p
              LEFT JOIN crm_receivable r ON r.id=p.receivable_id AND r.deleted=b'0'
             WHERE p.id=#{planId} AND p.deleted=b'0' AND p.return_time < #{todayStart}
               AND (p.receivable_id IS NULL OR r.audit_status IS NULL OR r.audit_status <> 20)
            """)
    int countStillOverdue(@Param("planId") Long planId, @Param("todayStart") LocalDateTime todayStart);

    @Update("""
            UPDATE crm_receivable_overdue_reminder
               SET status=1, attempts=attempts+1, sent_time=NOW(), last_error=NULL,
                   updater='receivable-overdue-job', update_time=NOW()
             WHERE id=#{id} AND status IN (0, 2) AND attempts=#{attempts}
            """)
    int markSent(@Param("id") Long id, @Param("attempts") Integer attempts);

    @Update("""
            UPDATE crm_receivable_overdue_reminder
               SET status=2, attempts=attempts+1, last_error=#{error},
                   updater='receivable-overdue-job', update_time=NOW()
             WHERE id=#{id} AND status IN (0, 2) AND attempts=#{attempts}
            """)
    int markFailed(@Param("id") Long id, @Param("attempts") Integer attempts,
                   @Param("error") String error);
}
