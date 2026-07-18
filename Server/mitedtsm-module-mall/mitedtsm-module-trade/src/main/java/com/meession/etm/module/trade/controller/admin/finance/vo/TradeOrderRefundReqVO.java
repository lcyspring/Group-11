package com.meession.etm.module.trade.controller.admin.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单退款请求")
public class TradeOrderRefundReqVO {

    @Schema(description = "订单编号", required = true)
    private Long orderId;

    @Schema(description = "退款金额（分）", required = true)
    private Long refundAmount;

    @Schema(description = "退款原因", required = true)
    private String refundReason;

    @Schema(description = "退款类型：1-仅退款 2-退货退款")
    private Integer refundType;

    @Schema(description = "备注")
    private String remark;

}