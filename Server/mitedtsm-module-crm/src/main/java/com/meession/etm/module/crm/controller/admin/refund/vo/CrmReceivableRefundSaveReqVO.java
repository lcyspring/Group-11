package com.meession.etm.module.crm.controller.admin.refund.vo;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.refund.CrmReceivableRefundTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 退款/冲销新增修改 Request VO")
@Data
public class CrmReceivableRefundSaveReqVO {

    private Long id;

    @NotNull(message = "原回款编号不能为空")
    private Long receivableId;

    @NotNull(message = "退款/冲销类型不能为空")
    @InEnum(CrmReceivableRefundTypeEnum.class)
    private Integer type;

    @NotNull(message = "退款/冲销日期不能为空")
    private LocalDateTime refundTime;

    @NotNull(message = "退款/冲销金额不能为空")
    @DecimalMin(value = "0.01", message = "退款/冲销金额必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "退款/冲销原因不能为空")
    @Size(min = 10, max = 500, message = "退款/冲销原因长度必须为 10 到 500 个字符")
    private String reason;

    @Size(max = 1000, message = "备注不能超过 1000 个字符")
    private String remark;
}
