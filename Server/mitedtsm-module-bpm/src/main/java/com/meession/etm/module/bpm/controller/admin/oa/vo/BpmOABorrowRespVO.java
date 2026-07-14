package com.meession.etm.module.bpm.controller.admin.oa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 借款申请 Response VO")
@Data
public class BpmOABorrowRespVO {

    @Schema(description = "借款表单主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "借款金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000.00")
    private BigDecimal amount;

    @Schema(description = "借款原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "项目备用金")
    private String reason;

    @Schema(description = "收款银行账号", example = "622202********1234")
    private String bankAccount;

    @Schema(description = "开户银行", example = "工商银行")
    private String bankName;

    @Schema(description = "预计还款日期")
    private LocalDateTime expectRepayDate;

    @Schema(description = "申请时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "流程编号")
    private String processInstanceId;

    @Schema(description = "审批结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

}