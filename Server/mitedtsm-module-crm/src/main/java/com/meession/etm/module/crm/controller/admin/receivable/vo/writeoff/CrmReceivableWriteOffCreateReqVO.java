package com.meession.etm.module.crm.controller.admin.receivable.vo.writeoff;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.receivable.CrmReceivableWriteOffSourceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CrmReceivableWriteOffCreateReqVO {
    @NotNull(message = "回款编号不能为空")
    private Long receivableId;
    @NotNull(message = "核销金额不能为空")
    @DecimalMin(value = "0.01", message = "核销金额必须大于等于 0.01")
    private BigDecimal amount;
    @NotNull(message = "核销时间不能为空")
    @PastOrPresent(message = "核销时间不能晚于当前时间")
    private LocalDateTime writeOffTime;
    @NotNull(message = "核销来源不能为空")
    @InEnum(value = CrmReceivableWriteOffSourceTypeEnum.class, message = "核销来源必须是 {value}")
    private Integer sourceType;
    @Size(max = 128, message = "外部流水号不能超过 128 个字符")
    private String referenceNo;
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
