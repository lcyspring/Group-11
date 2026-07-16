package com.meession.etm.module.crm.controller.admin.quote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 产品报价计算请求 VO")
@Data
public class CrmQuoteCalculateReqVO {

    @Schema(description = "产品列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "产品列表不能为空")
    private List<Product> products;

    @Schema(description = "整单折扣百分比", example = "10")
    private BigDecimal discountPercent;

    @Schema(description = "产品信息")
    @Data
    public static class Product {

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        @NotNull(message = "产品编号不能为空")
        private Long productId;

        @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @NotNull(message = "购买数量不能为空")
        @Positive(message = "购买数量必须大于0")
        private BigDecimal count;

    }

}
