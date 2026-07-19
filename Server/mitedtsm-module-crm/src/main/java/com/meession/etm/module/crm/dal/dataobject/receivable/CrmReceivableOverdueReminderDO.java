package com.meession.etm.module.crm.dal.dataobject.receivable;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("crm_receivable_overdue_reminder")
@KeySequence("crm_receivable_overdue_reminder_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmReceivableOverdueReminderDO extends BaseDO {
    @TableId
    private Long id;
    private Long receivablePlanId;
    private Long recipientUserId;
    private LocalDate reminderDate;
    private Integer status;
    private Integer attempts;
    private LocalDateTime sentTime;
    private String lastError;
}
