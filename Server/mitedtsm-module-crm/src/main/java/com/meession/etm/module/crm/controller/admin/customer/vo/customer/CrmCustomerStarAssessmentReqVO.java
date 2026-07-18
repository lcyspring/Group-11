package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户星级评估 Request VO")
@Data
public class CrmCustomerStarAssessmentReqVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "客户编号不能为空")
    private Long id;

    @Schema(description = "星级（1-5星）", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "星级不能为空")
    @Min(value = 1, message = "星级最小为1")
    @Max(value = 5, message = "星级最大为5")
    private Integer star;

    @Schema(description = "评估备注", example = "客户潜力大，建议重点跟进")
    private String remark;

}