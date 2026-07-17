package com.meession.etm.module.trade.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "订单日报表响应")
public class TradeOrderDailyReportRespVO {

    @Schema(description = "日期")
    private LocalDate date;

    @Schema(description = "订单数量")
    private Integer orderCount;

    @Schema(description = "订单金额（分）")
    private Long orderAmount;

    @Schema(description = "支付金额（分）")
    private Long paidAmount;

    @Schema(description = "退款金额（分）")
    private Long refundAmount;

}