package com.meession.etm.module.crm.dal.dataobject.refund;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("crm_receivable_refund_action_record")
@KeySequence("crm_receivable_refund_action_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmReceivableRefundActionRecordDO extends BaseDO {

    @TableId
    private Long id;
    private Long refundId;
    private Integer actionType;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorUserId;
    private LocalDateTime actionTime;
    private String processInstanceId;
    private String remark;
}
