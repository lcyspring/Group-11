package com.meession.etm.module.bpm.controller.admin.oa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - OA客户拜访申请创建 Request VO")
@Data
public class BpmOACustomerVisitCreateReqVO {

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "华为技术有限公司")
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    @Schema(description = "联系人", example = "张三")
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "拜访地址", example = "深圳市南山区科技园")
    private String visitAddress;

    @Schema(description = "拜访时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-16 14:00:00")
    @NotNull(message = "拜访时间不能为空")
    private LocalDateTime visitTime;

    @Schema(description = "拜访目的", requiredMode = Schema.RequiredMode.REQUIRED, example = "产品演示")
    @NotBlank(message = "拜访目的不能为空")
    private String purpose;

    @Schema(description = "拜访内容", example = "向客户演示新产品功能")
    private String content;

    @Schema(description = "发起人自选审批人 Map", example = "{taskKey1: [1, 2]}")
    private Map<String, List<Long>> startUserSelectAssignees;

}