package com.meession.etm.module.trade.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "订单报表查询请求")
public class TradeOrderReportReqVO {

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "订单类型")
    private Integer type;

    @Schema(description = "用户编号")
    private Long userId;

}