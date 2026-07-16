package com.meession.etm.module.trade.controller.admin.opportunity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 商机转订单 Request VO")
@Data
public class TradeOpportunityToOrderReqVO {

    @Schema(description = "商机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "商机编号不能为空")
    private Long opportunityId;

    @Schema(description = "用户编号", example = "1")
    private Long userId;

    @Schema(description = "收件人姓名", example = "张三")
    private String receiverName;

    @Schema(description = "收件人手机", example = "13800138000")
    private String receiverMobile;

    @Schema(description = "收件地区ID", example = "1")
    private Integer receiverAreaId;

    @Schema(description = "收件详细地址", example = "北京市朝阳区xxx街道")
    private String receiverDetailAddress;

}