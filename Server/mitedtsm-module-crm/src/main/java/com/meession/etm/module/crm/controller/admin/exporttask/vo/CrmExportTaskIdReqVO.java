package com.meession.etm.module.crm.controller.admin.exporttask.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrmExportTaskIdReqVO {
    @NotNull(message = "导出任务编号不能为空")
    private Long id;
}
