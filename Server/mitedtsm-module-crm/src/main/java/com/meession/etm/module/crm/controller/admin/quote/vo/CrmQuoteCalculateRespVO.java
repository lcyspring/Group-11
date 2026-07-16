package com.meession.etm.module.crm.controller.admin.quote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 产品报价计算响应 VO")
@Data
public class CrmQuoteCalculateRespVO {

    @Schema(description = "产品总金额（未折扣）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1500")
    private BigDecimal totalProductPrice;

    @Schema(description = "整单折扣百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private BigDecimal discountPercent;

    @Schema(description = "折扣金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "150")
    private BigDecimal discountAmount;

    @Schema(description = "最终报价金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1350")
    private BigDecimal totalPrice;

    @Schema(description = "产品明细列表")
    private List<Product> products;

    @Schema(description = "产品明细")
    @Data
    public static class Product {

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private Long productId;

        @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "产品A")
        private String productName;

        @Schema(description = "产品编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "P001")
        private String productNo;

        @Schema(description = "产品单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
        private BigDecimal productPrice;

        @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        private BigDecimal count;

        @Schema(description = "产品小计", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
        private BigDecimal totalPrice;

    }

}
