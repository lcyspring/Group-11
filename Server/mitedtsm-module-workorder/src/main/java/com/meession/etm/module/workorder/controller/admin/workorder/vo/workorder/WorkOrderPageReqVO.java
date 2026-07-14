package com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WorkOrderPageReqVO extends PageParam {

    @Schema(description = "工单标题", example = "系统故障")
    private String title;

    @Schema(description = "工单类型编号", example = "1")
    private Long typeId;

    @Schema(description = "优先级: 0-低, 1-中, 2-高, 3-紧急", example = "1")
    private Integer priority;

    @Schema(description = "工单状态: 0-待处理, 1-处理中, 2-已完成, 3-已关闭, 4-已退回", example = "0")
    private Integer status;

    @Schema(description = "处理人用户编号", example = "1")
    private Long handlerUserId;

    @Schema(description = "发起人用户编号", example = "1")
    private Long submitterUserId;

}
