package com.meession.etm.module.crm.controller.admin.statistics.vo.workorder;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CrmStatisticsWorkOrderTrendRespVO {
    private String time;
    private Long createdCount;
    private Long completedCount;
}
