package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("bpm_oa_loan")
@KeySequence("bpm_oa_loan_seq")
@Data
public class BpmOALoanDO extends BaseDO {
    @TableId private Long id;
    private Long userId;
    private String type;
    private BigDecimal amount;
    private String reason;
    private Long tripId;
    private String employeeLevel;
    private BigDecimal approvalLimit;
    private Boolean escalatedApproval;
    private BigDecimal outstandingAmount;
    private Integer repaymentStatus;
    private Integer status;
    private String processInstanceId;
    private LocalDateTime approvalTime;
    private LocalDateTime repaidTime;
}
