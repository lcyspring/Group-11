package com.meession.etm.module.crm.dal.mysql.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.workorder.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CrmStatisticsWorkOrderMapper {
    CrmStatisticsWorkOrderSummaryRespVO selectSummary(@Param("times") java.time.LocalDateTime[] times,
                                                       @Param("tenantId") Long tenantId,
                                                       @Param("userId") Long userId,
                                                       @Param("queryAll") boolean queryAll);

    List<CrmStatisticsWorkOrderStatusRespVO> selectByStatus(@Param("times") java.time.LocalDateTime[] times,
                                                             @Param("tenantId") Long tenantId,
                                                             @Param("userId") Long userId,
                                                             @Param("queryAll") boolean queryAll);

    List<CrmStatisticsWorkOrderTypeRespVO> selectByType(@Param("times") java.time.LocalDateTime[] times,
                                                         @Param("tenantId") Long tenantId,
                                                         @Param("userId") Long userId,
                                                         @Param("queryAll") boolean queryAll);

    List<CrmStatisticsWorkOrderHandlerRespVO> selectByHandler(@Param("times") java.time.LocalDateTime[] times,
                                                               @Param("tenantId") Long tenantId,
                                                               @Param("userId") Long userId,
                                                               @Param("queryAll") boolean queryAll);

    List<CrmStatisticsWorkOrderTrendRespVO> selectTrend(@Param("times") java.time.LocalDateTime[] times,
                                                         @Param("tenantId") Long tenantId,
                                                         @Param("userId") Long userId,
                                                         @Param("queryAll") boolean queryAll);
}
