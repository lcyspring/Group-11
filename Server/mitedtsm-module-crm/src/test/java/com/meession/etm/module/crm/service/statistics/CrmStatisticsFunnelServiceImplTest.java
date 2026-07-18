package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticFunnelSummaryRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsBusinessForecastByDateRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsBusinessInversionRateSummaryByDateRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsBusinessStageReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsBusinessStagePageReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsBusinessStageSummaryRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsFunnelReqVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsFunnelMapper;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessStatusDO;
import com.meession.etm.module.crm.dal.mysql.business.CrmBusinessMapper;
import com.meession.etm.module.crm.service.business.CrmBusinessStatusService;
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
    void getBusinessStagePageUsesValidatedStageSortAndScopedUsers() {
        CrmStatisticsFunnelServiceImpl funnelService = new CrmStatisticsFunnelServiceImpl();
        CrmBusinessDO business = new CrmBusinessDO().setId(100L).setName("重点商机");
        ReflectionTestUtils.setField(funnelService, "businessStatusService", proxy(CrmBusinessStatusService.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("validateBusinessStatus")) {
                        assertEquals(20L, args[0]);
                        assertEquals(30L, args[1]);
                        return new CrmBusinessStatusDO().setId(30L).setTypeId(20L).setSort(40);
                    }
                    throw new AssertionError("不应访问其他状态服务方法: " + method.getName());
                }));
        ReflectionTestUtils.setField(funnelService, "businessMapper", proxy(CrmBusinessMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectStagePage")) {
                        CrmStatisticsBusinessStagePageReqVO request = (CrmStatisticsBusinessStagePageReqVO) args[0];
                        assertEquals(List.of(10L), request.getUserIds());
                        assertEquals(40, args[1]);
                        return new PageResult<>(List.of(business), 1L);
                    }
                    throw new AssertionError("不应访问其他商机 Mapper 方法: " + method.getName());
                }));
        CrmStatisticsBusinessStagePageReqVO reqVO = stagePageRequest();

        PageResult<CrmBusinessDO> result = funnelService.getBusinessStagePage(reqVO);

        assertAll(
                () -> assertEquals(1L, result.getTotal()),
                () -> assertEquals(100L, result.getList().get(0).getId())
        );
    }

    @Test
    void getBusinessWonPageKeepsStatusTypeAndScopedUsers() {
        CrmStatisticsFunnelServiceImpl funnelService = new CrmStatisticsFunnelServiceImpl();
        ReflectionTestUtils.setField(funnelService, "businessStatusService", proxy(CrmBusinessStatusService.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("validateBusinessStatusType")) {
                        assertEquals(20L, args[0]);
                        return null;
                    }
                    throw new AssertionError("不应访问其他状态服务方法: " + method.getName());
                }));
        ReflectionTestUtils.setField(funnelService, "businessMapper", proxy(CrmBusinessMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectWonPage")) {
                        CrmStatisticsBusinessStageReqVO request = (CrmStatisticsBusinessStageReqVO) args[0];
                        assertEquals(List.of(10L), request.getUserIds());
                        return PageResult.empty();
                    }
                    throw new AssertionError("不应访问其他商机 Mapper 方法: " + method.getName());
                }));
        CrmStatisticsBusinessStageReqVO reqVO = stagePageRequest();

        PageResult<CrmBusinessDO> result = funnelService.getBusinessWonPage(reqVO);

        assertEquals(0L, result.getTotal());
    }

    @Test
    void getBusinessStageSummaryBuildsMonotonicCumulativeFunnel() {
        CrmStatisticsFunnelServiceImpl funnelService = new CrmStatisticsFunnelServiceImpl();
        List<CrmStatisticsBusinessStageSummaryRespVO> rawRows = List.of(
                stage(11L, "初步接洽", 10, null, 10L, "100000.00"),
                stage(12L, "需求确认", 20, null, 4L, "50000.00"),
                stage(13L, "方案报价", 30, null, 1L, "20000.00"),
                stage(null, null, Integer.MAX_VALUE - 2, 1, 2L, "40000.00"),
                stage(null, null, Integer.MAX_VALUE - 1, 2, 3L, "30000.00"),
                stage(null, null, Integer.MAX_VALUE, 3, 1L, "10000.00")
        );
        ReflectionTestUtils.setField(funnelService, "funnelMapper", proxy(CrmStatisticsFunnelMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectBusinessStageSummary")) {
                        return rawRows;
                    }
                    throw new AssertionError("不应访问其他统计 Mapper 方法: " + method.getName());
                }));
        ReflectionTestUtils.setField(funnelService, "businessStatusService", proxy(CrmBusinessStatusService.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("validateBusinessStatusType")) {
                        assertEquals(20L, args[0]);
                        return null;
                    }
                    throw new AssertionError("不应访问其他状态服务方法: " + method.getName());
                }));
        CrmStatisticsBusinessStageReqVO reqVO = new CrmStatisticsBusinessStageReqVO();
        reqVO.setDeptId(1L).setUserId(10L).setInterval(2).setTimes(new LocalDateTime[]{
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 31, 23, 59, 59)
        });
        reqVO.setStatusTypeId(20L);

        List<CrmStatisticsBusinessStageSummaryRespVO> result = funnelService.getBusinessStageSummary(reqVO);

        assertAll(
                () -> assertEquals(List.of(15L, 5L, 1L, 2L, 3L, 1L),
                        result.stream().map(CrmStatisticsBusinessStageSummaryRespVO::getBusinessCount).toList()),
                () -> assertEquals(List.of(new BigDecimal("170000.00"), new BigDecimal("70000.00"),
                                new BigDecimal("20000.00"), new BigDecimal("40000.00"),
                                new BigDecimal("30000.00"), new BigDecimal("10000.00")),
                        result.stream().map(CrmStatisticsBusinessStageSummaryRespVO::getTotalPrice).toList()),
                () -> assertEquals(List.of(new BigDecimal("100.00"), new BigDecimal("33.33"),
                                new BigDecimal("20.00"), new BigDecimal("33.33"),
                                new BigDecimal("50.00"), new BigDecimal("16.67")),
                        result.stream().map(CrmStatisticsBusinessStageSummaryRespVO::getConversionRate).toList())
        );
    }

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

    @Test
    void getBusinessForecastAggregatesForecastAndActualAmountsByInterval() {
        CrmStatisticsFunnelServiceImpl funnelService = new CrmStatisticsFunnelServiceImpl();
        List<CrmStatisticsBusinessForecastByDateRespVO> dailyRows = List.of(
                forecast("2026-07-01", 2L, 1L, "100000.00", "30000.00"),
                forecast("2026-07-20", 1L, 2L, "50000.00", "20000.00")
        );
        ReflectionTestUtils.setField(funnelService, "funnelMapper", proxy(CrmStatisticsFunnelMapper.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("selectBusinessForecastGroupByDate")) {
                        return dailyRows;
                    }
                    throw new AssertionError("不应访问其他统计 Mapper 方法: " + method.getName());
                }));
        CrmStatisticsFunnelReqVO reqVO = new CrmStatisticsFunnelReqVO()
                .setUserId(10L)
                .setInterval(3)
                .setTimes(new LocalDateTime[]{
                        LocalDateTime.of(2026, 7, 1, 0, 0),
                        LocalDateTime.of(2026, 8, 31, 23, 59, 59)
                });

        List<CrmStatisticsBusinessForecastByDateRespVO> result =
                funnelService.getBusinessForecastByDate(reqVO);

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals(3L, result.get(0).getForecastBusinessCount()),
                () -> assertEquals(3L, result.get(0).getActualBusinessCount()),
                () -> assertEquals(new BigDecimal("150000.00"), result.get(0).getForecastAmount()),
                () -> assertEquals(new BigDecimal("50000.00"), result.get(0).getActualAmount()),
                () -> assertEquals(0L, result.get(1).getForecastBusinessCount()),
                () -> assertEquals(0L, result.get(1).getActualBusinessCount()),
                () -> assertEquals(new BigDecimal("0.00"), result.get(1).getForecastAmount()),
                () -> assertEquals(new BigDecimal("0.00"), result.get(1).getActualAmount())
        );
    }

    private static CrmStatisticsBusinessForecastByDateRespVO forecast(String time, long forecastCount,
                                                                       long actualCount, String forecast,
                                                                       String actual) {
        return new CrmStatisticsBusinessForecastByDateRespVO()
                .setTime(time)
                .setForecastBusinessCount(forecastCount)
                .setActualBusinessCount(actualCount)
                .setForecastAmount(new BigDecimal(forecast))
                .setActualAmount(new BigDecimal(actual));
    }

    private static CrmStatisticsBusinessStageSummaryRespVO stage(Long statusId, String statusName, Integer sort,
                                                                  Integer endStatus, long count, String totalPrice) {
        return new CrmStatisticsBusinessStageSummaryRespVO()
                .setStatusId(statusId).setStatusName(statusName).setSort(sort).setEndStatus(endStatus)
                .setBusinessCount(count).setTotalPrice(new BigDecimal(totalPrice));
    }

    private static CrmStatisticsBusinessStagePageReqVO stagePageRequest() {
        CrmStatisticsBusinessStagePageReqVO reqVO = new CrmStatisticsBusinessStagePageReqVO();
        reqVO.setDeptId(1L).setUserId(10L).setInterval(2).setTimes(new LocalDateTime[]{
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 31, 23, 59, 59)
        });
        reqVO.setStatusTypeId(20L);
        reqVO.setStatusId(30L);
        return reqVO;
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

}
