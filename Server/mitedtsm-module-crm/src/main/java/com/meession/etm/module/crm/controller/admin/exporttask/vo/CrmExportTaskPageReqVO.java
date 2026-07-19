package com.meession.etm.module.crm.controller.admin.exporttask.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CrmExportTaskPageReqVO extends PageParam {
    private String objectType;
    private Integer status;
}
