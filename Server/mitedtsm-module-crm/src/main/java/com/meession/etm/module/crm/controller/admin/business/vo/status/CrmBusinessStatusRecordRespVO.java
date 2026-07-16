package com.meession.etm.module.crm.controller.admin.business.vo.status;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 商机状态流转记录 Response VO")
@Data
public class CrmBusinessStatusRecordRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "商机编号", example = "10")
    private Long businessId;

    @Schema(description = "原状态类型编号", example = "1")
    private Long oldStatusTypeId;

    @Schema(description = "原状态类型名称", example = "进行中")
    private String oldStatusTypeName;

    @Schema(description = "原状态编号", example = "1")
    private Long oldStatusId;

    @Schema(description = "原状态名称", example = "跟进中")
    private String oldStatusName;

    @Schema(description = "原结束状态", example = "1")
    private Integer oldEndStatus;

    @Schema(description = "原结束状态名称", example = "赢单")
    private String oldEndStatusName;

    @Schema(description = "新状态类型编号", example = "1")
    private Long newStatusTypeId;

    @Schema(description = "新状态类型名称", example = "进行中")
    private String newStatusTypeName;

    @Schema(description = "新状态编号", example = "2")
    private Long newStatusId;

    @Schema(description = "新状态名称", example = "报价")
    private String newStatusName;

    @Schema(description = "新结束状态", example = "1")
    private Integer newEndStatus;

    @Schema(description = "新结束状态名称", example = "赢单")
    private String newEndStatusName;

    @Schema(description = "操作人编号", example = "1")
    private Long operatorId;

    @Schema(description = "操作人名称", example = "张三")
    private String operatorName;

    @Schema(description = "流转备注", example = "客户接受报价")
    private String remark;

    @Schema(description = "流转时间", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;

}