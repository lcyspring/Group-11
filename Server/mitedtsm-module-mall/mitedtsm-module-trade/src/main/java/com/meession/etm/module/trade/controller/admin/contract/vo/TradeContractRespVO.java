package com.meession.etm.module.trade.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同 Response VO")
@Data
public class TradeContractRespVO {

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "合同流水号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CT202401010001")
    private String no;

    @Schema(description = "合同名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "采购合同")
    private String name;

    @Schema(description = "订单编号", example = "1")
    private Long orderId;

    @Schema(description = "订单流水号", example = "O202401010001")
    private String orderNo;

    @Schema(description = "客户编号", example = "1")
    private Long customerId;

    @Schema(description = "客户名称", example = "XX科技有限公司")
    private String customerName;

    @Schema(description = "合同金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    private BigDecimal amount;

    @Schema(description = "合同类型", example = "采购")
    private String contractType;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "状态名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "草稿")
    private String statusName;

    @Schema(description = "开始日期")
    private LocalDateTime startDate;

    @Schema(description = "结束日期")
    private LocalDateTime endDate;

    @Schema(description = "签署人", example = "张三")
    private String signer;

    @Schema(description = "签署人电话", example = "13800138000")
    private String signerPhone;

    @Schema(description = "签署日期")
    private LocalDateTime signDate;

    @Schema(description = "附件URL（逗号分隔）")
    private String attachmentUrls;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}