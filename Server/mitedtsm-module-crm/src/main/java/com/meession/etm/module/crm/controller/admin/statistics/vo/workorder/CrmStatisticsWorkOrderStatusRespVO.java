package com.meession.etm.module.crm.controller.admin.statistics.vo.workorder;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CrmStatisticsWorkOrderStatusRespVO {
    private Integer status;
    private Long count;
}
