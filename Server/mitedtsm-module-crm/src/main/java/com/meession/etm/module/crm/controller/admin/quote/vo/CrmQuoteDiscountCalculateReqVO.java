package com.meession.etm.module.crm.controller.admin.quote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 报价折扣计算请求 VO")
@Data
public class CrmQuoteDiscountCalculateReqVO {

    @Schema(description = "订单总金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000")
    @NotNull(message = "订单总金额不能为空")
    @DecimalMin(value = "0", message = "订单总金额不能小于0")
    private BigDecimal totalAmount;

    @Schema(description = "客户等级（1-普通，2-银卡，3-金卡，4-钻石）", example = "3")
    @Min(value = 1, message = "客户等级最小为1")
    @Max(value = 4, message = "客户等级最大为4")
    private Integer customerLevel;

    @Schema(description = "产品总数量", example = "20")
    @Min(value = 1, message = "产品数量不能小于1")
    private Integer productCount;

    @Schema(description = "业务类型ID", example = "1")
    private Long businessTypeId;

}
