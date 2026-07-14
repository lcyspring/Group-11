package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WorkOrderRespVO {

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("工单编号")
    private Long id;

    @Schema(description = "工单标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "系统故障报修")
    @ExcelProperty("工单标题")
    private String title;

    @Schema(description = "工单内容/描述", example = "用户反映系统登录异常")
    @ExcelProperty("工单内容")
    private String content;

    @Schema(description = "工单类型编号", example = "1")
    private Long typeId;
    @Schema(description = "工单类型名称", example = "故障报修")
    @ExcelProperty("工单类型")
    private String typeName;

    @Schema(description = "优先级", example = "1")
    @ExcelProperty("优先级")
    private Integer priority;
    @Schema(description = "优先级名称", example = "中")
    private String priorityName;

    @Schema(description = "工单状态", example = "0")
    @ExcelProperty("工单状态")
    private Integer status;
    @Schema(description = "工单状态名称", example = "待处理")
    private String statusName;

    @Schema(description = "处理人用户编号", example = "1")
    private Long handlerUserId;
    @Schema(description = "处理人名称", example = "张三")
    @ExcelProperty("处理人")
    private String handlerUserName;

    @Schema(description = "发起人用户编号", example = "2")
    private Long submitterUserId;
    @Schema(description = "发起人名称", example = "李四")
    @ExcelProperty("发起人")
    private String submitterUserName;

    @Schema(description = "处理结果", example = "已修复完成")
    @ExcelProperty("处理结果")
    private String result;

    @Schema(description = "处理时间")
    @ExcelProperty("处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "预计完成时间")
    @ExcelProperty("预计完成时间")
    private LocalDateTime expectedFinishTime;

    @Schema(description = "实际完成时间")
    @ExcelProperty("实际完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "关联客户编号", example = "1")
    private Long customerId;

    @Schema(description = "关联商机编号", example = "1")
    private Long businessId;

    @Schema(description = "备注", example = "需要尽快处理")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建人", example = "admin")
    @ExcelProperty("创建人")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
