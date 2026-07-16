package com.meession.etm.module.crm.dal.dataobject.workorder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("crm_work_order_sla")
@KeySequence("crm_work_order_sla_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmWorkOrderSlaDO extends BaseDO {
    @TableId
    private Long id;
    private Long workOrderId;
    private Long policyId;
    private LocalDateTime responseDueTime;
    private LocalDateTime escalationDueTime;
    private LocalDateTime resolutionDueTime;
    private Long pausedSeconds;
    private LocalDateTime pausedAt;
    private Integer status;
    private LocalDateTime escalatedAt;
    private LocalDateTime completedAt;
}
