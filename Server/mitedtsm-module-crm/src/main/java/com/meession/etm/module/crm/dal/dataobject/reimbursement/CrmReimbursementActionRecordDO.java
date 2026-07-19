package com.meession.etm.module.crm.dal.dataobject.reimbursement;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("crm_reimbursement_action_record")
@KeySequence("crm_reimbursement_action_record_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmReimbursementActionRecordDO extends BaseDO {
    @TableId
    private Long id;
    private Long reimbursementId;
    private Integer actionType;
    private Integer fromStatus;
    private Integer toStatus;
    private BigDecimal amountSnapshot;
    private Long operatorUserId;
    private LocalDateTime actionTime;
    private String processInstanceId;
    private String remark;
}
