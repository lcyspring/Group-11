package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsTargetCompletionReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsTargetCompletionRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsTargetCompletionSummaryRespVO;
import com.meession.etm.module.crm.dal.dataobject.statistics.CrmPerformanceTargetDO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmPerformanceTargetMapper;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsPerformanceMapper;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetScopeTypeEnum;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetTypeEnum;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_FILTER_SCOPE_MISMATCH;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_PERIOD_INVALID;

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
                () -> assertEquals(new BigDecimal("400.00"), result.get(0).getMonthOnMonthRate()),
                () -> assertEquals(new BigDecimal("233.33"), result.get(0).getYearOnYearRate()),
                () -> assertEquals(new BigDecimal("15"), result.get(1).getCurrentMonthCount()),
                () -> assertEquals(new BigDecimal("10"), result.get(1).getLastMonthCount()),
                () -> assertEquals(BigDecimal.ZERO, result.get(1).getLastYearCount()),
                () -> assertEquals(new BigDecimal("50.00"), result.get(1).getMonthOnMonthRate()),
                () -> assertNull(result.get(1).getYearOnYearRate())
        );
    }

    @Test
    void calculateGrowthRateUsesExactDecimalArithmetic() {
        assertAll(
                () -> assertEquals(new BigDecimal("20.00"), CrmStatisticsPerformanceServiceImpl
                        .calculateGrowthRate(new BigDecimal("120"), new BigDecimal("100"))),
                () -> assertEquals(new BigDecimal("-25.00"), CrmStatisticsPerformanceServiceImpl
                        .calculateGrowthRate(new BigDecimal("75"), new BigDecimal("100"))),
                () -> assertEquals(new BigDecimal("-33.33"), CrmStatisticsPerformanceServiceImpl
                        .calculateGrowthRate(new BigDecimal("2"), new BigDecimal("3"))),
                () -> assertNull(CrmStatisticsPerformanceServiceImpl
                        .calculateGrowthRate(new BigDecimal("10"), BigDecimal.ZERO)),
                () -> assertEquals(new BigDecimal("900719925474099200.00"), CrmStatisticsPerformanceServiceImpl
                        .calculateGrowthRate(new BigDecimal("9007199254740993"), BigDecimal.ONE))
        );
    }

    @Test
    void getTargetCompletionCombinesMonthlyTargetActualAndRate() {
        CrmStatisticsPerformanceServiceImpl service = new CrmStatisticsPerformanceServiceImpl();
        ReflectionTestUtils.setField(service, "performanceMapper", proxy(CrmStatisticsPerformanceMapper.class,
                (proxy, method, args) -> {
                    assertEquals("selectCustomerCountPerformance", method.getName());
                    CrmStatisticsPerformanceReqVO request = (CrmStatisticsPerformanceReqVO) args[0];
                    assertEquals(List.of(20L), request.getUserIds());
                    return List.of(performance("202601", 15), performance("202602", 5));
                }));
        ReflectionTestUtils.setField(service, "performanceTargetMapper", proxy(CrmPerformanceTargetMapper.class,
                (proxy, method, args) -> {
                    assertEquals("selectListByScopeAndYear", method.getName());
                    assertEquals(CrmPerformanceTargetScopeTypeEnum.USER.getType(), args[0]);
                    assertEquals(20L, args[1]);
                    assertEquals(2026, args[2]);
                    return List.of(target(1, "10"), target(2, "0"));
                }));
        CrmStatisticsTargetCompletionReqVO reqVO = completionRequest();

        CrmStatisticsTargetCompletionSummaryRespVO summary = service.getTargetCompletion(reqVO);
        List<CrmStatisticsTargetCompletionRespVO> result = summary.getMonthlyList();

        assertEquals(12, result.size());
        assertAll(
                () -> assertEquals("2026-01", result.get(0).getTime()),
                () -> assertEquals("10", result.get(0).getTargetValue()),
                () -> assertEquals("15", result.get(0).getActualValue()),
                () -> assertEquals(new BigDecimal("150.00"), result.get(0).getCompletionRate()),
                () -> assertEquals("5", result.get(1).getActualValue()),
                () -> assertNull(result.get(1).getCompletionRate()),
                () -> assertEquals("10", summary.getAnnualTarget()),
                () -> assertEquals("20", summary.getAnnualActual()),
                () -> assertEquals(new BigDecimal("200.00"), summary.getAnnualCompletionRate())
        );
    }

    @Test
    void getTargetCompletionRejectsScopeThatConflictsWithSelectedUser() {
        CrmStatisticsPerformanceServiceImpl service = new CrmStatisticsPerformanceServiceImpl();
        CrmStatisticsTargetCompletionReqVO reqVO = completionRequest();
        reqVO.setScopeType(CrmPerformanceTargetScopeTypeEnum.DEPARTMENT.getType());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.getTargetCompletion(reqVO));

        assertEquals(PERFORMANCE_TARGET_FILTER_SCOPE_MISMATCH.getCode(), exception.getCode());
    }

    @Test
    void getTargetCompletionRejectsPartialYear() {
        CrmStatisticsPerformanceServiceImpl service = new CrmStatisticsPerformanceServiceImpl();
        CrmStatisticsTargetCompletionReqVO reqVO = completionRequest();
        reqVO.getTimes()[1] = LocalDateTime.of(2026, 6, 30, 23, 59, 59);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.getTargetCompletion(reqVO));

        assertEquals(PERFORMANCE_TARGET_PERIOD_INVALID.getCode(), exception.getCode());
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

    private static CrmStatisticsTargetCompletionReqVO completionRequest() {
        CrmStatisticsTargetCompletionReqVO reqVO = new CrmStatisticsTargetCompletionReqVO();
        reqVO.setDeptId(10L);
        reqVO.setUserId(20L);
        reqVO.setTimes(new LocalDateTime[]{
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59)
        });
        reqVO.setScopeType(CrmPerformanceTargetScopeTypeEnum.USER.getType());
        reqVO.setTargetType(CrmPerformanceTargetTypeEnum.CUSTOMER_COUNT.getType());
        return reqVO;
    }

    private static CrmPerformanceTargetDO target(Integer month, String value) {
        return new CrmPerformanceTargetDO().setTargetMonth(month).setTargetValue(new BigDecimal(value));
    }

}
