package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticFunnelSummaryRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsBusinessInversionRateSummaryByDateRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsFunnelReqVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsFunnelMapper;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmStatisticsFunnelServiceImplTest {

    @Test
    void getFunnelSummaryReturnsZeroSummaryWhenDepartmentHasNoUsers() {
        CrmStatisticsFunnelServiceImpl funnelService = new CrmStatisticsFunnelServiceImpl();
        ReflectionTestUtils.setField(funnelService, "deptApi", proxy(DeptApi.class,
                (proxy, method, args) -> Collections.emptyList()));
        ReflectionTestUtils.setField(funnelService, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> Collections.emptyList()));
        ReflectionTestUtils.setField(funnelService, "funnelMapper", proxy(CrmStatisticsFunnelMapper.class,
                (proxy, method, args) -> {
                    throw new AssertionError("空用户范围不应访问统计 Mapper");
                }));
        CrmStatisticsFunnelReqVO reqVO = new CrmStatisticsFunnelReqVO()
                .setDeptId(10L)
                .setInterval(2)
                .setTimes(new LocalDateTime[]{LocalDateTime.now().minusDays(7), LocalDateTime.now()});

        CrmStatisticFunnelSummaryRespVO result = funnelService.getFunnelSummary(reqVO);

        assertAll(
                () -> assertEquals(0L, result.getCustomerCount()),
                () -> assertEquals(0L, result.getBusinessCount()),
                () -> assertEquals(0L, result.getBusinessWinCount())
        );
    }

    @Test
    void getBusinessInversionRateSummaryReturnsServerDefinedPercentages() {
        CrmStatisticsFunnelServiceImpl funnelService = new CrmStatisticsFunnelServiceImpl();
        List<CrmStatisticsBusinessInversionRateSummaryByDateRespVO> dailyRows = List.of(
                new CrmStatisticsBusinessInversionRateSummaryByDateRespVO()
                        .setTime("2026-07-01").setBusinessCount(4L).setBusinessWinCount(1L),
                new CrmStatisticsBusinessInversionRateSummaryByDateRespVO()
                        .setTime("2026-07-02").setBusinessCount(3L).setBusinessWinCount(2L)
        );
        ReflectionTestUtils.setField(funnelService, "funnelMapper", proxy(CrmStatisticsFunnelMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectBusinessInversionRateSummaryByDate")) {
                        return dailyRows;
                    }
                    throw new AssertionError("不应访问其他统计 Mapper 方法: " + method.getName());
                }));
        CrmStatisticsFunnelReqVO reqVO = new CrmStatisticsFunnelReqVO()
                .setUserId(10L)
                .setInterval(1)
                .setTimes(new LocalDateTime[]{
                        LocalDateTime.of(2026, 7, 1, 0, 0),
                        LocalDateTime.of(2026, 7, 3, 23, 59, 59)
                });

        List<CrmStatisticsBusinessInversionRateSummaryByDateRespVO> result =
                funnelService.getBusinessInversionRateSummaryByDate(reqVO);

        assertAll(
                () -> assertEquals(3, result.size()),
                () -> assertEquals(new BigDecimal("25.00"), result.get(0).getBusinessWinRate()),
                () -> assertEquals(new BigDecimal("66.67"), result.get(1).getBusinessWinRate()),
                () -> assertEquals(new BigDecimal("0.00"), result.get(2).getBusinessWinRate()),
                () -> assertEquals(0L, result.get(2).getBusinessCount()),
                () -> assertEquals(0L, result.get(2).getBusinessWinCount())
        );
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
