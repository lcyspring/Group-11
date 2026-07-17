package com.meession.etm.module.trade.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单报表响应")
public class TradeOrderReportRespVO {

    @Schema(description = "订单总数")
    private Integer totalCount;

    @Schema(description = "已支付订单数")
    private Integer paidCount;

    @Schema(description = "已发货订单数")
    private Integer deliveryCount;

    @Schema(description = "已完成订单数")
    private Integer receiveCount;

    @Schema(description = "已取消订单数")
    private Integer cancelCount;

    @Schema(description = "总金额（分）")
    private Long totalAmount;

    @Schema(description = "支付金额（分）")
    private Long paidAmount;

    @Schema(description = "退款金额（分）")
    private Long refundAmount;

    @Schema(description = "客单价（分）")
    private Long avgOrderAmount;

}