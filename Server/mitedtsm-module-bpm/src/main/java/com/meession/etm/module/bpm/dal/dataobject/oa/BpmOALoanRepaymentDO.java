package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("bpm_oa_loan_repayment")
@KeySequence("bpm_oa_loan_repayment_seq")
@Data
public class BpmOALoanRepaymentDO extends BaseDO {
    @TableId private Long id;
    private Long loanId;
    private Long userId;
    private BigDecimal amount;
    private LocalDateTime repaidAt;
    private String referenceNo;
    private String remark;
}
