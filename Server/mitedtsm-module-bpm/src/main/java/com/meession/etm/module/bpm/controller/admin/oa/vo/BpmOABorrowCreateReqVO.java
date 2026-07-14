package com.meession.etm.module.bpm.controller.admin.oa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 借款申请创建 Request VO")
@Data
public class BpmOABorrowCreateReqVO {

    @Schema(description = "借款金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000.00")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @Schema(description = "借款原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "项目备用金")
    @NotNull(message = "原因不能为空")
    private String reason;

    @Schema(description = "收款银行账号", example = "622202********1234")
    private String bankAccount;

    @Schema(description = "开户银行", example = "工商银行")
    private String bankName;

    @Schema(description = "预计还款日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectRepayDate;

    @Schema(description = "发起人自选审批人 Map", example = "{taskKey1: [1, 2]}")
    private Map<String, List<Long>> startUserSelectAssignees;

}