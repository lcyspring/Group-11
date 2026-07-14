package com.meession.etm.module.trade.controller.admin.order.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class TradeOrderSaveReqVO {

    private Long id;

    @NotBlank(message = "订单编号不能为空")
    private String no;

    @NotNull(message = "订单类型不能为空")
    private Integer type;

    @NotNull(message = "终端类型不能为空")
    private Integer terminal;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "用户IP不能为空")
    private String userIp;

    private String userRemark;

    @NotNull(message = "订单状态不能为空")
    private Integer status;

    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量必须大于0")
    private Integer productCount;

    private Integer cancelType;

    private String remark;

    private Boolean commentStatus;

    private Long brokerageUserId;

    @NotNull(message = "支付状态不能为空")
    private Boolean payStatus;

    private Integer totalPrice;

    private Integer orderPrice;

    @NotNull(message = "折扣金额不能为空")
    private Integer discountPrice;

    @NotNull(message = "运费不能为空")
    private Integer deliveryPrice;

    @NotNull(message = "调整金额不能为空")
    private Integer adjustPrice;

    @NotNull(message = "实付金额不能为空")
    private Integer payPrice;

    @NotNull(message = "配送方式不能为空")
    private Integer deliveryType;

    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "收货人手机号不能为空")
    private String receiverMobile;

    @NotNull(message = "收货地区ID不能为空")
    private Integer receiverAreaId;

    @NotBlank(message = "收货详细地址不能为空")
    private String receiverDetailAddress;

    private Integer receiverPostCode;

    private Long couponId;

    private Integer couponPrice;

    private Integer pointPrice;

    private List<TradeOrderItemSaveReqVO> items;

}