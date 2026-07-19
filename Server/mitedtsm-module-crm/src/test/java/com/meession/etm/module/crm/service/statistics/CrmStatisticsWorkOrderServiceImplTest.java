package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import com.meession.etm.module.crm.controller.admin.statistics.vo.workorder.*;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsWorkOrderMapper;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CrmStatisticsWorkOrderServiceImplTest {

    @BeforeEach
    void setTenant() { TenantContextHolder.setTenantId(7L); }

    @AfterEach
    void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void summaryUsesExplicitSecurityScopeAndCalculatesRate() {
        CrmStatisticsWorkOrderServiceImpl service = service((proxy, method, args) -> {
            assertEquals("selectSummary", method.getName());
            assertEquals(7L, args[1]);
            assertEquals(9L, args[2]);
            assertEquals(false, args[3]);
            return new CrmStatisticsWorkOrderSummaryRespVO().setTotalCount(4L).setCompletedCount(2L)
                    .setPendingCount(null).setProcessingCount(1L).setReturnedCount(1L);
        });

        CrmStatisticsWorkOrderSummaryRespVO result = service.getSummary(request(), 9L, false);

        assertEquals(0L, result.getPendingCount());
        assertEquals("50%", result.getCompletionRate());
    }

    @Test
    void emptySummaryIsZeroSafe() {
        CrmStatisticsWorkOrderServiceImpl service = service((proxy, method, args) -> null);
        CrmStatisticsWorkOrderSummaryRespVO result = service.getSummary(request(), 9L, true);
        assertAll(() -> assertEquals(0L, result.getTotalCount()), () -> assertEquals("0%", result.getCompletionRate()));
    }

    @Test
    void handlersAreEnrichedWithoutChangingCounts() {
        CrmStatisticsWorkOrderServiceImpl service = service((proxy, method, args) ->
                List.of(new CrmStatisticsWorkOrderHandlerRespVO().setHandlerUserId(11L).setCount(3L)));
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> Map.of(11L, new AdminUserRespDTO().setId(11L).setNickname("客服甲"))));

        List<CrmStatisticsWorkOrderHandlerRespVO> result = service.getByHandler(request(), 9L, true);

        assertEquals("客服甲", result.get(0).getHandlerUserName());
        assertEquals(3L, result.get(0).getCount());
    }

    @Test
    void trendUsesCreateAndCompleteCountsAndFillsEmptyDays() {
        CrmStatisticsWorkOrderServiceImpl service = service((proxy, method, args) -> List.of(
                new CrmStatisticsWorkOrderTrendRespVO().setTime("2026-07-01T00:00:00").setCreatedCount(2L).setCompletedCount(0L),
                new CrmStatisticsWorkOrderTrendRespVO().setTime("2026-07-03T00:00:00").setCreatedCount(0L).setCompletedCount(1L)));
        CrmStatisticsWorkOrderReqVO reqVO = request().setTimes(new LocalDateTime[]{
                LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 3, 23, 59, 59)});

        List<CrmStatisticsWorkOrderTrendRespVO> result = service.getTrend(reqVO, 9L, false);

        assertEquals(3, result.size());
        assertEquals(2L, result.get(0).getCreatedCount());
        assertEquals(0L, result.get(1).getCreatedCount());
        assertEquals(1L, result.get(2).getCompletedCount());
    }

    private static CrmStatisticsWorkOrderReqVO request() {
        return new CrmStatisticsWorkOrderReqVO().setInterval(1).setTimes(new LocalDateTime[]{
                LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 1, 23, 59, 59)});
    }

    private static CrmStatisticsWorkOrderServiceImpl service(java.lang.reflect.InvocationHandler handler) {
        CrmStatisticsWorkOrderServiceImpl service = new CrmStatisticsWorkOrderServiceImpl();
        ReflectionTestUtils.setField(service, "mapper", proxy(CrmStatisticsWorkOrderMapper.class, handler));
        return service;
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
