package com.meession.etm.module.crm.dal.dataobject.reimbursement;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("crm_reimbursement")
@KeySequence("crm_reimbursement_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmReimbursementDO extends BaseDO {
    @TableId
    private Long id;
    private String no;
    private Long applicantUserId;
    private Long ownerUserId;
    private Long departmentId;
    private Long customerId;
    private Long contractId;
    private Long tripId;
    private String currency;
    private BigDecimal totalAmount;
    private LocalDate expenseStartDate;
    private LocalDate expenseEndDate;
    private String reason;
    private String remark;
    private String processInstanceId;
    private Integer auditStatus;
    private Integer version;
}
