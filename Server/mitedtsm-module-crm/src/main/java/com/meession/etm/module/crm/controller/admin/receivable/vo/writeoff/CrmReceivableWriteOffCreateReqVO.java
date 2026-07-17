package com.meession.etm.module.crm.controller.admin.receivable.vo.writeoff;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CrmReceivableWriteOffCreateReqVO {
    @NotNull private Long receivableId;
    @NotNull @Positive private BigDecimal amount;
    @NotNull private LocalDateTime writeOffTime;
    @NotNull private Integer sourceType;
    private String referenceNo;
    private String remark;
}
