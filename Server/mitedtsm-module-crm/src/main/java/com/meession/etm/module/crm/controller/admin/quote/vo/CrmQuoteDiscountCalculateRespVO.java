package com.meession.etm.module.crm.controller.admin.quote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 报价折扣计算响应 VO")
@Data
public class CrmQuoteDiscountCalculateRespVO {

    @Schema(description = "计算出的折扣百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "15")
    private BigDecimal discountPercent;

    @Schema(description = "折扣金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "750")
    private BigDecimal discountAmount;

    @Schema(description = "折扣后的金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "4250")
    private BigDecimal finalAmount;

    @Schema(description = "应用的折扣规则列表")
    private List<DiscountRule> rules;

    @Schema(description = "折扣规则")
    @Data
    public static class DiscountRule {

        @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "金额阶梯折扣")
        private String ruleName;

        @Schema(description = "规则折扣百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        private BigDecimal discountPercent;

        @Schema(description = "规则描述", example = "满5000打9折")
        private String description;

    }

}
