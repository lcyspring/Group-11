package com.meession.etm.module.trade.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "订单创建请求VO（前端联调）")
public class TradeOrderCreateReqVO {

    @Schema(description = "用户编号", required = true)
    private Long userId;

    @Schema(description = "订单类型", required = true)
    private Integer type;

    @Schema(description = "终端类型", required = true)
    private Integer terminal;

    @Schema(description = "用户IP", required = true)
    private String userIp;

    @Schema(description = "收货人姓名", required = true)
    private String receiverName;

    @Schema(description = "收货人手机号", required = true)
    private String receiverMobile;

    @Schema(description = "收货地区编号", required = true)
    private Long receiverAreaId;

    @Schema(description = "收货详细地址", required = true)
    private String receiverDetailAddress;

    @Schema(description = "配送方式", required = true)
    private Integer deliveryType;

    @Schema(description = "订单项列表", required = true)
    private List<OrderItem> items;

    @Schema(description = "优惠券编号")
    private Long couponId;

    @Schema(description = "备注")
    private String remark;

    @Data
    @Schema(description = "订单项")
    public static class OrderItem {

        @Schema(description = "商品编号", required = true)
        private Long productId;

        @Schema(description = "规格编号", required = true)
        private Long skuId;

        @Schema(description = "数量", required = true)
        private Integer count;

        @Schema(description = "购物车项编号")
        private Long cartItemId;

    }

}