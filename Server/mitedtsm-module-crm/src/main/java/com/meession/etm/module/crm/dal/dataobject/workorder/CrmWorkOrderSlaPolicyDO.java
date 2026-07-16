package com.meession.etm.module.crm.dal.dataobject.workorder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("crm_work_order_sla_policy")
@KeySequence("crm_work_order_sla_policy_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmWorkOrderSlaPolicyDO extends BaseDO {
    @TableId
    private Long id;
    private String code;
    private String name;
    private Integer priority;
    private Integer responseMinutes;
    private Integer resolutionMinutes;
    private Integer escalationMinutes;
    private Boolean enabled;
    private Integer sort;
}
