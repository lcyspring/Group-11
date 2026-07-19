package com.meession.etm.module.crm.controller.admin.statistics.vo.workorder;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CrmStatisticsWorkOrderHandlerRespVO {
    private Long handlerUserId;
    private String handlerUserName;
    private Long count;
}
