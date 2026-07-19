package com.meession.etm.module.crm.dal.dataobject.workorder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName("crm_work_order_record")
@KeySequence("crm_work_order_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmWorkOrderRecordDO extends BaseDO {

    @TableId
    private Long id;
    private Long workOrderId;
    private Integer actionType;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorUserId;
    private Long handlerUserId;
    private String remark;
}
