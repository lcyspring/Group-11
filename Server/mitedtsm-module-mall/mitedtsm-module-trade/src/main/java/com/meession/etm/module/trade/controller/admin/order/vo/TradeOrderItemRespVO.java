package com.meession.etm.module.trade.controller.admin.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TradeOrderItemRespVO {

    private Long id;

    private Long userId;

    private Long orderId;

    private Integer cartId;

    private Long spuId;

    private String spuName;

    private Long skuId;

    private String properties;

    private String picUrl;

    private Integer count;

    private Boolean commentStatus;

    private Integer price;

    private Integer discountPrice;

    private Integer deliveryPrice;

    private Integer adjustPrice;

    private Integer payPrice;

    private Integer couponPrice;

    private Integer pointPrice;

    private Integer usePoint;

    private Integer givePoint;

    private Integer vipPrice;

    private Long afterSaleId;

    private Integer afterSaleStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}