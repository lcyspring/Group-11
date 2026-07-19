package com.meession.etm.module.bpm.controller.admin.oa.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BpmOALoanRepaymentCreateReqVO {
    @NotNull private Long loanId;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    private LocalDateTime repaidAt;
    @Size(max = 100) private String referenceNo;
    @Size(max = 500) private String remark;
}
