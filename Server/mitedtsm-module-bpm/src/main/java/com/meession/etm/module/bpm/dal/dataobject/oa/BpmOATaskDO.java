package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;
import java.time.LocalDateTime;

@TableName("bpm_oa_task") @KeySequence("bpm_oa_task_seq")
@Data @EqualsAndHashCode(callSuper = true) @Builder @NoArgsConstructor @AllArgsConstructor
public class BpmOATaskDO extends BaseDO {
    @TableId private Long id;
    private String title; private String description; private Long creatorUserId; private Long assigneeUserId;
    private String participantUserIds; private Integer priority; private Integer status;
    private LocalDateTime dueTime; private Integer reminderMinutes; private LocalDateTime completedTime;
    private Integer reminderStatus; private LocalDateTime reminderSentTime; private String reminderLastError;
    private String businessType; private Long businessId; private String result;
}
