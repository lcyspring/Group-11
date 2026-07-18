package com.meession.etm.module.trade.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "订单详情响应VO（前端联调）")
public class TradeOrderDetailRespVO {

    @Schema(description = "订单编号")
    private Long id;

    @Schema(description = "订单号")
    private String no;

    @Schema(description = "订单类型")
    private Integer type;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "订单状态名称")
    private String statusName;

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "用户手机号")
    private String userMobile;

    @Schema(description = "商品数量")
    private Integer productCount;

    @Schema(description = "订单总价（分）")
    private Long totalPrice;

    @Schema(description = "订单总价（元）")
    private BigDecimal totalPriceYuan;

    @Schema(description = "支付金额（分）")
    private Long payPrice;

    @Schema(description = "支付金额（元）")
    private BigDecimal payPriceYuan;

    @Schema(description = "优惠金额（分）")
    private Long discountPrice;

    @Schema(description = "运费（分）")
    private Long deliveryPrice;

    @Schema(description = "支付状态")
    private Boolean payStatus;

    @Schema(description = "支付方式")
    private Integer payType;

    @Schema(description = "支付方式名称")
    private String payTypeName;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "发货状态")
    private Boolean deliveryStatus;

    @Schema(description = "发货时间")
    private LocalDateTime deliveryTime;

    @Schema(description = "收货状态")
    private Boolean receiveStatus;

    @Schema(description = "收货时间")
    private LocalDateTime receiveTime;

    @Schema(description = "退款状态")
    private Integer refundStatus;

    @Schema(description = "退款金额（分）")
    private Long refundPrice;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "审批状态")
    private Integer approvalStatus;

    @Schema(description = "审批状态名称")
    private String approvalStatusName;

    @Schema(description = "审批意见")
    private String approvalComment;

    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "收货人姓名")
    private String receiverName;

    @Schema(description = "收货人手机号")
    private String receiverMobile;

    @Schema(description = "收货地址")
    private String receiverAddress;

    @Schema(description = "订单项列表")
    private List<OrderItemVO> items;

    @Schema(description = "合同信息")
    private ContractInfo contractInfo;

    @Schema(description = "支付记录")
    private PaymentInfo paymentInfo;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Data
    @Schema(description = "订单项VO")
    public static class OrderItemVO {

        @Schema(description = "订单项编号")
        private Long id;

        @Schema(description = "商品编号")
        private Long productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品图片")
        private String productImage;

        @Schema(description = "规格编号")
        private Long skuId;

        @Schema(description = "规格名称")
        private String skuName;

        @Schema(description = "数量")
        private Integer count;

        @Schema(description = "单价（分）")
        private Long price;

        @Schema(description = "优惠金额（分）")
        private Long discountPrice;

        @Schema(description = "实付金额（分）")
        private Long payPrice;

        @Schema(description = "售后状态")
        private Integer afterSaleStatus;

    }

    @Data
    @Schema(description = "合同信息VO")
    public static class ContractInfo {

        @Schema(description = "合同编号")
        private Long contractId;

        @Schema(description = "合同号")
        private String contractNo;

        @Schema(description = "合同名称")
        private String contractName;

        @Schema(description = "合同状态")
        private Integer contractStatus;

        @Schema(description = "合同状态名称")
        private String contractStatusName;

        @Schema(description = "签署日期")
        private LocalDateTime signDate;

    }

    @Data
    @Schema(description = "支付信息VO")
    public static class PaymentInfo {

        @Schema(description = "支付记录编号")
        private Long paymentId;

        @Schema(description = "支付方式")
        private Integer payType;

        @Schema(description = "支付渠道")
        private String payChannel;

        @Schema(description = "支付金额（分）")
        private Long payAmount;

        @Schema(description = "支付时间")
        private LocalDateTime payTime;

        @Schema(description = "第三方流水号")
        private String transactionNo;

    }

}