package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.workorder.*;

import java.util.List;

public interface CrmStatisticsWorkOrderService {
    CrmStatisticsWorkOrderSummaryRespVO getSummary(CrmStatisticsWorkOrderReqVO reqVO, Long userId, boolean queryAll);
    List<CrmStatisticsWorkOrderStatusRespVO> getByStatus(CrmStatisticsWorkOrderReqVO reqVO, Long userId, boolean queryAll);
    List<CrmStatisticsWorkOrderTypeRespVO> getByType(CrmStatisticsWorkOrderReqVO reqVO, Long userId, boolean queryAll);
    List<CrmStatisticsWorkOrderHandlerRespVO> getByHandler(CrmStatisticsWorkOrderReqVO reqVO, Long userId, boolean queryAll);
    List<CrmStatisticsWorkOrderTrendRespVO> getTrend(CrmStatisticsWorkOrderReqVO reqVO, Long userId, boolean queryAll);
}
