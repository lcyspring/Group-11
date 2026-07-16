package com.meession.etm.module.crm.dal.dataobject.workorder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@TableName("crm_work_order_check_in")
@KeySequence("crm_work_order_check_in_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmWorkOrderCheckInDO extends BaseDO {
    @TableId
    private Long id;
    private Long workOrderId;
    private Long userId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracyMeters;
    private BigDecimal distanceMeters;
    private Integer result;
}
