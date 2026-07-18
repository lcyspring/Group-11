package com.meession.etm.module.trade.controller.admin.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "订单支付请求")
public class TradeOrderPaymentReqVO {

    @Schema(description = "订单编号", required = true)
    private Long orderId;

    @Schema(description = "支付方式", required = true)
    private Integer payType;

    @Schema(description = "支付金额（分）", required = true)
    private Long payAmount;

    @Schema(description = "支付渠道")
    private String payChannel;

    @Schema(description = "第三方支付流水号")
    private String transactionNo;

    @Schema(description = "备注")
    private String remark;

}