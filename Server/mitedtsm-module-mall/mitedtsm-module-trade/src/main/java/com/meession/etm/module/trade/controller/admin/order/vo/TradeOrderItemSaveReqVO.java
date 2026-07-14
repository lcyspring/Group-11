package com.meession.etm.module.trade.controller.admin.order.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TradeOrderItemSaveReqVO {

    private Long id;

    @NotNull(message = "SPU ID不能为空")
    private Long spuId;

    @NotBlank(message = "SPU名称不能为空")
    private String spuName;

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    private String properties;

    private String picUrl;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer count;

    private Boolean commentStatus;

    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private Integer price;

    @NotNull(message = "折扣金额不能为空")
    private Integer discountPrice;

    private Integer deliveryPrice;

    private Integer adjustPrice;

    @NotNull(message = "实付金额不能为空")
    @Min(value = 0, message = "实付金额不能为负数")
    private Integer payPrice;

    private Integer couponPrice;

    private Integer pointPrice;

    private Integer usePoint;

    private Integer givePoint;

    private Integer vipPrice;

    @NotNull(message = "售后状态不能为空")
    private Integer afterSaleStatus;

}