package com.meession.etm.module.trade.controller.admin.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "订单支付记录")
public class TradeOrderPaymentRespVO {

    @Schema(description = "支付记录编号")
    private Long id;

    @Schema(description = "订单编号")
    private Long orderId;

    @Schema(description = "支付方式")
    private Integer payType;

    @Schema(description = "支付渠道")
    private String payChannel;

    @Schema(description = "支付金额（分）")
    private Long payAmount;

    @Schema(description = "支付状态")
    private Integer status;

    @Schema(description = "第三方支付流水号")
    private String transactionNo;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "备注")
    private String remark;

}