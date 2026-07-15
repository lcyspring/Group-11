package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 客户归属变更记录 Response VO")
@Data
public class CrmCustomerOwnerRecordRespVO {

    @Schema(description = "记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long customerId;

    @Schema(description = "变更类型：1 进入公海，2 领取或分配，3 初始分配，4 转移", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer type;

    @Schema(description = "事件来源编码")
    private String source;

    @Schema(description = "事件原因")
    private String reason;

    @Schema(description = "变更前负责人编号", example = "100")
    private Long previousOwnerUserId;

    @Schema(description = "变更前负责人名称", example = "张三")
    private String previousOwnerUserName;

    @Schema(description = "变更后负责人编号", example = "101")
    private Long newOwnerUserId;

    @Schema(description = "变更后负责人名称", example = "李四")
    private String newOwnerUserName;

    @Schema(description = "操作人编号", example = "102")
    private Long operatorUserId;

    @Schema(description = "操作人名称", example = "王五")
    private String operatorUserName;

    @Schema(description = "变更时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
