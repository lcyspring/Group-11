package com.meession.etm.module.trade.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单列表响应VO（前端联调）")
public class TradeOrderListRespVO {

    @Schema(description = "订单编号")
    private Long id;

    @Schema(description = "订单号")
    private String no;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "订单状态名称")
    private String statusName;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "商品数量")
    private Integer productCount;

    @Schema(description = "支付金额（元）")
    private BigDecimal payPriceYuan;

    @Schema(description = "支付状态")
    private Boolean payStatus;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "审批状态")
    private Integer approvalStatus;

    @Schema(description = "审批状态名称")
    private String approvalStatusName;

    @Schema(description = "收货人姓名")
    private String receiverName;

    @Schema(description = "收货人手机号")
    private String receiverMobile;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}