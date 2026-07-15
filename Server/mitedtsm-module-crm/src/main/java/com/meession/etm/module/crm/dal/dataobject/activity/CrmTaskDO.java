package com.meession.etm.module.crm.dal.dataobject.activity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("crm_task")
@KeySequence("crm_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmTaskDO extends BaseDO {
    @TableId
    private Long id;
    private Integer bizType;
    private Long bizId;
    private Long sourceClueId;
    private Integer type;
    private String title;
    private String description;
    private Integer priority;
    private Integer status;
    private Long assigneeUserId;
    private LocalDateTime dueTime;
    private LocalDateTime remindTime;
    private Boolean notifySystem;
    private Boolean notifyEmail;
    private Boolean notifySms;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private String result;
    private Integer version;
}
