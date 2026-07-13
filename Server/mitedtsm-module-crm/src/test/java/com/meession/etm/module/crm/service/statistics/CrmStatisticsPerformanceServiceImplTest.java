package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceRespVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsPerformanceMapper;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmStatisticsPerformanceServiceImplTest {

    @Test
    void getContractCountPerformanceReturnsEmptyWhenDepartmentHasNoUsers() {
        CrmStatisticsPerformanceServiceImpl performanceService = createService(Collections.emptyList(),
                (proxy, method, args) -> {
                    throw new AssertionError("空用户范围不应访问统计 Mapper");
                });
        CrmStatisticsPerformanceReqVO reqVO = createRequest();

        List<CrmStatisticsPerformanceRespVO> result = performanceService.getContractCountPerformance(reqVO);

        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void getContractCountPerformanceBuildsTwelveMonthsAndRestoresRequestTime() {
        List<CrmStatisticsPerformanceRespVO> mapperResult = List.of(
                performance("202301", 3),
                performance("202312", 2),
                performance("202401", 10),
                performance("202402", 15)
        );
        CrmStatisticsPerformanceServiceImpl performanceService = createService(
                Collections.singletonList(new AdminUserRespDTO().setId(20L)),
                (proxy, method, args) -> mapperResult);
        CrmStatisticsPerformanceReqVO reqVO = createRequest();
        LocalDateTime originalStartTime = reqVO.getTimes()[0];

        List<CrmStatisticsPerformanceRespVO> result = performanceService.getContractCountPerformance(reqVO);

        assertEquals(12, result.size());
        assertAll(
                () -> assertEquals(originalStartTime, reqVO.getTimes()[0]),
                () -> assertEquals(new BigDecimal("10"), result.get(0).getCurrentMonthCount()),
                () -> assertEquals(new BigDecimal("2"), result.get(0).getLastMonthCount()),
                () -> assertEquals(new BigDecimal("3"), result.get(0).getLastYearCount()),
                () -> assertEquals(new BigDecimal("15"), result.get(1).getCurrentMonthCount()),
                () -> assertEquals(new BigDecimal("10"), result.get(1).getLastMonthCount()),
                () -> assertEquals(BigDecimal.ZERO, result.get(1).getLastYearCount())
        );
    }

    private static CrmStatisticsPerformanceServiceImpl createService(
            List<AdminUserRespDTO> users, java.lang.reflect.InvocationHandler mapperHandler) {
        CrmStatisticsPerformanceServiceImpl service = new CrmStatisticsPerformanceServiceImpl();
        ReflectionTestUtils.setField(service, "deptApi", proxy(DeptApi.class,
                (proxy, method, args) -> Collections.emptyList()));
        ReflectionTestUtils.setField(service, "adminUserApi", proxy(AdminUserApi.class,
                (proxy, method, args) -> users));
        ReflectionTestUtils.setField(service, "performanceMapper",
                proxy(CrmStatisticsPerformanceMapper.class, mapperHandler));
        return service;
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static CrmStatisticsPerformanceReqVO createRequest() {
        return new CrmStatisticsPerformanceReqVO()
                .setDeptId(10L)
                .setTimes(new LocalDateTime[]{
                        LocalDateTime.of(2024, 1, 1, 0, 0),
                        LocalDateTime.of(2024, 12, 31, 23, 59, 59)
                });
    }

    private static CrmStatisticsPerformanceRespVO performance(String time, long value) {
        return new CrmStatisticsPerformanceRespVO()
                .setTime(time)
                .setCurrentMonthCount(BigDecimal.valueOf(value));
    }

}
