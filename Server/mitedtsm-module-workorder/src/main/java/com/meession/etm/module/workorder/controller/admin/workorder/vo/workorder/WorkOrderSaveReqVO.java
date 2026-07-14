package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工单创建/更新 Request VO")
@Data
public class WorkOrderSaveReqVO {

    @Schema(description = "工单编号", example = "1")
    private Long id;

    @Schema(description = "工单标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "系统故障报修")
    @NotBlank(message = "工单标题不能为空")
    private String title;

    @Schema(description = "工单内容/描述", example = "用户反映系统登录异常")
    private String content;

    @Schema(description = "工单类型编号", example = "1")
    private Long typeId;

    @Schema(description = "优先级: 0-低, 1-中, 2-高, 3-紧急", example = "1")
    private Integer priority;

    @Schema(description = "处理人用户编号", example = "1")
    private Long handlerUserId;

    @Schema(description = "预计完成时间")
    private LocalDateTime expectedFinishTime;

    @Schema(description = "关联客户编号", example = "1")
    private Long customerId;

    @Schema(description = "关联商机编号", example = "1")
    private Long businessId;

    @Schema(description = "备注", example = "需要尽快处理")
    private String remark;

}
