package com.meession.etm.module.trade.controller.admin.opportunity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 商机 Response VO")
@Data
public class TradeOpportunityRespVO {

    @Schema(description = "商机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "商机流水号", requiredMode = Schema.RequiredMode.REQUIRED, example = "O202401010001")
    private String no;

    @Schema(description = "商机名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "企业采购项目")
    private String name;

    @Schema(description = "商机描述", example = "某企业年度采购计划")
    private String description;

    @Schema(description = "客户编号", example = "1")
    private Long customerId;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "XX科技有限公司")
    private String customerName;

    @Schema(description = "产品编号", example = "100")
    private Long productId;

    @Schema(description = "产品名称", example = "办公设备套餐")
    private String productName;

    @Schema(description = "预估金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    private BigDecimal amount;

    @Schema(description = "数量", example = "10")
    private Integer quantity;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "状态名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "待跟进")
    private String statusName;

    @Schema(description = "销售用户编号", example = "1")
    private Long salesUserId;

    @Schema(description = "销售用户名称", example = "张三")
    private String salesUserName;

    @Schema(description = "预计成交时间")
    private LocalDateTime expectedCloseTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "订单编号", example = "100")
    private Long orderId;

    @Schema(description = "商机产品行")
    private List<TradeOpportunityItemRespVO> items;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}