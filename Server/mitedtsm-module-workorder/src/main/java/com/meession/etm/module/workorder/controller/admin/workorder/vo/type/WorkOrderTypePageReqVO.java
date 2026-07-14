package com.meession.etm.module.workorder.controller.admin.workorder.vo.type;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 工单类型分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WorkOrderTypePageReqVO extends PageParam {

    @Schema(description = "类型名称", example = "故障报修")
    private String name;

    @Schema(description = "类型编码", example = "REPAIR")
    private String code;

    @Schema(description = "状态: 0-启用, 1-禁用", example = "0")
    private Integer status;

}
