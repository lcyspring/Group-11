package com.meession.etm.module.bpm.controller.admin.oa.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class BpmOALoanCreateReqVO {
    @NotBlank(message = "借款类型不能为空") @Size(max = 30) private String type;
    @NotNull(message = "借款金额不能为空") @DecimalMin(value = "0.01", message = "借款金额必须大于 0") private BigDecimal amount;
    @NotBlank(message = "借款原因不能为空") @Size(min = 5, max = 1000) private String reason;
    private Long tripId;
    private Map<String, List<Long>> startUserSelectAssignees;
}
