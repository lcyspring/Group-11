package com.meession.etm.module.crm.dal.dataobject.workorder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("crm_work_order_cc")
@KeySequence("crm_work_order_cc_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmWorkOrderCcDO extends BaseDO {

    @TableId
    private Long id;
    private Long workOrderId;
    private Long userId;
}
