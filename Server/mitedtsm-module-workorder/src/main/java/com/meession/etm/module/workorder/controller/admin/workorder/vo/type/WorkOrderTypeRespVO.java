package com.meession.etm.module.workorder.controller.admin.workorder.vo.type;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工单类型 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WorkOrderTypeRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "故障报修")
    @ExcelProperty("类型名称")
    private String name;

    @Schema(description = "类型编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "REPAIR")
    @ExcelProperty("类型编码")
    private String code;

    @Schema(description = "类型描述", example = "系统故障相关的工单")
    @ExcelProperty("类型描述")
    private String description;

    @Schema(description = "排序", example = "0")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "状态: 0-启用, 1-禁用", example = "0")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
