package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;
import java.time.LocalDateTime;

@TableName("bpm_oa_event") @KeySequence("bpm_oa_event_seq")
@Data @EqualsAndHashCode(callSuper = true) @Builder @NoArgsConstructor @AllArgsConstructor
public class BpmOAEventDO extends BaseDO {
    @TableId private Long id; private Long userId; private String title; private String description;
    private LocalDateTime startTime; private LocalDateTime endTime; private Boolean allDay;
    private String location; private String participantUserIds; private Integer reminderMinutes; private Integer status;
}
