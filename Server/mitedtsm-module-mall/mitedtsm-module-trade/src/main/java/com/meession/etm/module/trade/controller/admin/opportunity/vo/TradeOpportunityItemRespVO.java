package com.meession.etm.module.trade.controller.admin.opportunity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 商机产品行 Response VO")
@Data
public class TradeOpportunityItemRespVO {

    @Schema(description = "产品行编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "商机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long opportunityId;

    @Schema(description = "商品SPU编号", example = "1")
    private Long spuId;

    @Schema(description = "商品SKU编号", example = "10")
    private Long skuId;

    @Schema(description = "商品名称", example = "笔记本电脑")
    private String productName;

    @Schema(description = "SKU名称", example = "MacBook Pro 16寸")
    private String skuName;

    @Schema(description = "单价（分）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000000")
    private BigDecimal price;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer count;

    @Schema(description = "总价（分）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000000")
    private BigDecimal totalPrice;

}